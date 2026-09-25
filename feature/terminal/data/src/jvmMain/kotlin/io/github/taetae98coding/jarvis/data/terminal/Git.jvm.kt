package io.github.taetae98coding.jarvis.data.terminal

import io.github.taetae98coding.jarvis.data.state.observeByPolling
import io.github.taetae98coding.jarvis.data.state.observeOnSignals
import io.github.taetae98coding.jarvis.domain.terminal.FileContent
import io.github.taetae98coding.jarvis.domain.terminal.GitBranch
import io.github.taetae98coding.jarvis.domain.terminal.GitChange
import io.github.taetae98coding.jarvis.domain.terminal.GitCommitFile
import io.github.taetae98coding.jarvis.domain.terminal.GitFileDiff
import io.github.taetae98coding.jarvis.domain.terminal.GitGraphLine
import io.github.taetae98coding.jarvis.domain.terminal.GitPushTarget
import io.github.taetae98coding.jarvis.domain.terminal.GitStatus
import io.github.taetae98coding.jarvis.domain.terminal.GitWorktree
import io.github.taetae98coding.jarvis.domain.terminal.GitWorktreeException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.util.concurrent.TimeUnit
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

internal actual fun createGitDataSource(): GitDataSource = ProcessGitDataSource()

/** 저장소가 되고 안 되는 일은 드물다. + 하나가 이만큼 늦게 뜬다(docs/platform/jvm.html#terminal-worktree). */
internal val GitWorktreePollInterval: Duration = 10.seconds

/** "새 워크트리" 창이 떠 있는 동안 셸에서 한 fetch·브랜치 작업을 따라가는 간격(docs/platform/jvm.html#terminal-worktree-base-branch). */
internal val GitBranchesPollInterval: Duration = 5.seconds

/** 사이드 바의 Git 구획이 보이는 동안 셸에서 한 git 작업을 따라가는 간격(docs/platform/jvm.html#terminal-side-bar). */
internal val GitChangesPollInterval: Duration = 3.seconds

internal const val GitGraphMaxCommits = 200

internal val GitCommandTimeout: Duration = 30.seconds

/** 올릴 커밋이 많거나 느린 원격이면 30초를 넘는다. 인증을 기다리며 멈춘 push 도 여기서 끝난다. */
internal val GitPushTimeout: Duration = 5.minutes

// upstream 도 없고 원격을 고를 단서가 없을 때 먼저 찾는 원격(docs/common/terminal-side-bar.html 용어 "올릴 곳").
private const val DefaultRemote = "origin"

/** [outputBytes] 는 stdout 그대로다. [output] 은 그것을 UTF-8 로 푼 것이고, blob 내용처럼 바이트가 필요한 곳만 [outputBytes] 를 본다. */
internal class GitResult(
    val exitCode: Int,
    val outputBytes: ByteArray,
    val error: String,
) {
    constructor(exitCode: Int, output: String, error: String) : this(exitCode, output.encodeToByteArray(), error)

    val output: String by lazy { outputBytes.decodeToString() }
}

/**
 * `git` CLI 로 저장소를 판정하고 워크트리를 만든다. JGit 을 버린 이유는 docs/platform/jvm.html#terminal-worktree 에 있다.
 * [runCommand] 는 테스트가 갈아 끼운다.
 */
internal class ProcessGitDataSource(
    private val home: String = System.getProperty("user.home"),
    private val runCommand: suspend (command: List<String>, timeout: Duration) -> GitResult = ::runGit,
    private val changesPollInterval: Duration = GitChangesPollInterval,
) : GitDataSource {
    // stage·unstage·push 가 끝나면 폴링 간격을 기다리지 않고 상태·그래프를 다시 읽는다.
    private val repositoryChanges = MutableSharedFlow<Unit>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    private suspend fun run(command: List<String>): GitResult = runCommand(command, GitCommandTimeout)

    override fun observeWorktree(directory: String): Flow<GitWorktree?> =
        observeByPolling(interval = GitWorktreePollInterval) { readWorktree(directory) }.flowOn(Dispatchers.IO)

    override fun observeBranches(directory: String): Flow<List<GitBranch>> =
        observeByPolling(interval = GitBranchesPollInterval) { readBranches(directory) }.flowOn(Dispatchers.IO)

    override suspend fun addWorktree(repositoryDirectory: String, branch: String, path: String, baseBranch: String?): Result<GitWorktree> =
        withContext(Dispatchers.IO) {
            val repository = File(expandHome(repositoryDirectory, home))
            val target = File(expandHome(path, home)).let { if (it.isAbsolute) it else File(repository, it.path) }

            val branchExists = run(git(repository, "rev-parse", "--verify", "--quiet", "refs/heads/$branch")).exitCode == 0
            val added = if (branchExists) {
                run(git(repository, "worktree", "add", target.path, branch))
            } else {
                run(git(repository, "worktree", "add", "-b", branch, target.path, *listOfNotNull(baseBranch).toTypedArray()))
            }
            added.failure()?.let { return@withContext Result.failure(it) }

            readWorktree(target.path)
                ?.let { Result.success(it) }
                ?: Result.failure(GitWorktreeException("만든 워크트리를 읽을 수 없습니다: ${target.path}"))
        }

    // 브랜치는 워크트리를 뗀 뒤에 지운다. 체크아웃하고 있는 워크트리가 있는 동안은 git branch -D 가 거부한다.
    override suspend fun removeWorktree(directory: String, deleteDirectory: Boolean): Result<Unit> =
        withContext(Dispatchers.IO) {
            val worktree = readWorktree(directory)
                ?: return@withContext Result.failure(GitWorktreeException("워크트리가 아닙니다: $directory"))
            if (worktree.isMain) return@withContext Result.failure(GitWorktreeException("main 워크트리는 지울 수 없습니다: ${worktree.path}"))

            val main = File(worktree.mainPath)
            val removed = if (deleteDirectory) {
                // --force 는 쓰지 않는다. 커밋하지 않은 변경이 있으면 git 이 거부하게 둔다(docs/common/terminal-worktree.html#requirements R15).
                run(git(main, "worktree", "remove", worktree.path))
            } else {
                // worktree remove 는 폴더를 남기는 옵션이 없다. .git 파일이 사라지면 저장소 쪽 기록이 가리키는 곳이 없어져 prune 이 걷는다.
                val link = File(worktree.path, ".git")
                if (!link.isFile || !link.delete()) {
                    return@withContext Result.failure(GitWorktreeException("연결된 워크트리의 .git 파일을 지울 수 없습니다: ${link.path}"))
                }
                run(git(main, "worktree", "prune"))
            }
            removed.failure()?.let { return@withContext Result.failure(it) }

            worktree.branch
                ?.let { run(git(main, "branch", "-D", it)).failure() }
                ?.let { return@withContext Result.failure(it) }

            Result.success(Unit)
        }

    override fun observeStatus(directory: String): Flow<GitStatus?> =
        observeOnSignals(changeSignals()) { readStatus(directory) }.flowOn(Dispatchers.IO)

    override fun observeGraph(directory: String): Flow<List<GitGraphLine>> =
        observeOnSignals(changeSignals()) { readGraph(directory) }.flowOn(Dispatchers.IO)

    // 커밋은 바뀌지 않으므로 틱·신호 없이 한 번 읽고 끝난다(docs/common/terminal-side-bar.html 결정).
    override fun observeCommitFiles(directory: String, hash: String): Flow<List<GitChange>?> =
        flow { emit(readCommitFiles(directory, hash)) }.flowOn(Dispatchers.IO)

    override fun observeCommitFile(path: String, hash: String): Flow<GitCommitFile?> =
        flow { emit(readCommitFile(path, hash)) }.flowOn(Dispatchers.IO)

    override fun observeFileDiff(path: String): Flow<GitFileDiff?> =
        observeOnSignals(changeSignals()) { readFileDiff(path) }.flowOn(Dispatchers.IO)

    override suspend fun stage(root: String, changes: List<GitChange>): Result<Unit> =
        withContext(Dispatchers.IO) {
            val paths = changes.map { it.path }.distinct()
            // -A 는 지운 파일도 index 에서 지운다.
            val result = run(git(File(root), "add", "-A", "--", *paths.toTypedArray()))
            repositoryChanges.tryEmit(Unit)
            result.failure()?.let { Result.failure(it) } ?: Result.success(Unit)
        }

    override suspend fun unstage(root: String, changes: List<GitChange>): Result<Unit> =
        withContext(Dispatchers.IO) {
            val repository = File(root)
            val paths = changes.flatMap { listOfNotNull(it.path, it.originalPath) }.distinct().toTypedArray()
            // restore --staged 는 커밋이 없는 저장소에서 HEAD 를 풀지 못해 실패한다. 그때는 index 에서 빼는 것이 곧 unstage 다.
            val hasHead = run(git(repository, "rev-parse", "--verify", "--quiet", "HEAD")).exitCode == 0
            val result = if (hasHead) {
                run(git(repository, "restore", "--staged", "--", *paths))
            } else {
                run(git(repository, "rm", "--cached", "-r", "-q", "--", *paths))
            }
            repositoryChanges.tryEmit(Unit)
            result.failure()?.let { Result.failure(it) } ?: Result.success(Unit)
        }

    // -u 는 이미 같은 곳이면 바뀌는 것이 없고, 원격 추적 브랜치에서 갈라 만든 워크트리처럼 upstream 이 다른 이름이면 올린 브랜치로 옮긴다.
    override suspend fun push(root: String, target: GitPushTarget): Result<Unit> =
        withContext(Dispatchers.IO) {
            val ref = "refs/heads/${target.branch}"
            val result = runCommand(git(File(root), "push", "-u", target.remote, "$ref:$ref"), GitPushTimeout)
            repositoryChanges.tryEmit(Unit)
            result.failure()?.let { Result.failure(it) } ?: Result.success(Unit)
        }

    private fun changeSignals(): Flow<Unit> = merge(pollingTicks(changesPollInterval), repositoryChanges)

    private suspend fun readStatus(directory: String): GitStatus? {
        val root = readRoot(directory) ?: return null
        // 뒤에서 도는 조회가 index.lock 을 잡아 사용자의 git 명령과 부딪히지 않게 --no-optional-locks 를 준다.
        val result = run(listOf("git", "--no-optional-locks", "-C", root, "status", "--porcelain=v1", "-z", "-b", "--untracked-files=all"))
        if (result.exitCode != 0) return null

        val status = parseGitStatus(root, result.output)
        val header = parseGitBranchHeader(result.output) ?: return status
        return status.copy(pushTarget = readPushTarget(root, header))
    }

    // 비교 대상은 upstream 이 아니라 같은 이름의 원격 브랜치다(docs/common/terminal-side-bar.html 결정). upstream 이 마침
    // 그것이면 머리의 수를 그대로 쓰고, 아닐 때만 따로 센다.
    private suspend fun readPushTarget(root: String, header: GitBranchHeader): GitPushTarget? {
        val branch = header.branch?.takeUnless { header.unborn } ?: return null
        val repository = File(root)
        val remotes = readRemotes(repository)
        // 원격 이름에 `/` 가 들어갈 수 있어서 upstream 앞부분과 겹치는 가장 긴 이름이 그 원격이다.
        val remote = remotes.filter { header.upstream?.startsWith("$it/") == true }.maxByOrNull { it.length }
            ?: DefaultRemote.takeIf { it in remotes }
            ?: remotes.firstOrNull()
            ?: return null

        if (header.upstream == "$remote/$branch") {
            return GitPushTarget(remote, branch, exists = true, ahead = header.ahead, behind = header.behind)
        }

        // `<원격 브랜치>...HEAD` 의 --left-right 는 `뒤진수\t앞선수` 다. ref 가 없으면 128 로 끝난다.
        val counts = run(git(repository, "rev-list", "--count", "--left-right", "refs/remotes/$remote/$branch...HEAD"))
        val numbers = counts.output.trim().split('\t').mapNotNull { it.toIntOrNull() }
        if (counts.exitCode != 0 || numbers.size != 2) return GitPushTarget(remote, branch, exists = false)

        return GitPushTarget(remote, branch, exists = true, ahead = numbers[1], behind = numbers[0])
    }

    private suspend fun readRemotes(repository: File): List<String> =
        run(git(repository, "remote"))
            .takeIf { it.exitCode == 0 }
            ?.output?.lines()?.map { it.trim() }?.filter { it.isNotEmpty() }
            .orEmpty()

    // 원격 추적 브랜치는 마지막 fetch 로 받아 둔 것만 본다. 창이 원격에 접속하지 않는다(docs/common/terminal-worktree-base-branch.html R9).
    private suspend fun readBranches(directory: String): List<GitBranch> {
        val root = readRoot(directory) ?: return emptyList()
        val repository = File(root)
        val refs = run(
            git(repository, "for-each-ref", "--sort=-committerdate", "--format=%(refname)%00%(symref)", "refs/heads", "refs/remotes"),
        )
        if (refs.exitCode != 0) return emptyList()

        return parseGitBranches(refs.output, readRemotes(repository))
    }

    // 시작점은 HEAD 하나다 — 현재 브랜치에서 닿는 커밋만 그린다(공통 R18). `--branches --remotes --tags` 를 주면 다른 워크트리의
    // 커밋까지 섞인다. 커밋이 없는 저장소는 HEAD 를 풀지 못해 git log 가 128 로 끝난다. 그것도 빈 그래프다.
    private suspend fun readGraph(directory: String): List<GitGraphLine> {
        val root = readRoot(directory) ?: return emptyList()
        val result = run(
            git(
                File(root),
                "log", "--graph", "--date-order", "--color=never", "HEAD",
                "-n", GitGraphMaxCommits.toString(), "--date=format:%Y-%m-%d %H:%M", "--format=$GitGraphFormat",
            ),
        )
        if (result.exitCode != 0) return emptyList()

        return parseGitGraph(result.output)
    }

    // --root 가 없으면 첫 커밋이, --diff-merges 가 없으면 병합 커밋이 빈 출력이다. `-m --first-parent` 는 log 와 달리
    // diff-tree 에서는 모든 부모와의 diff 를 이어 붙여서 못 쓴다. -M 은 plumbing 이라 diff.renames 를 따르지 않아 직접 준다
    // (docs/platform/jvm.html#terminal-side-bar).
    private suspend fun readCommitFiles(directory: String, hash: String): List<GitChange>? {
        val folder = File(expandHome(directory, home))
        if (!folder.isDirectory || !hasGitAncestor(folder)) return null

        return readNameStatus(folder, hash)
    }

    private suspend fun readNameStatus(folder: File, hash: String): List<GitChange>? {
        val result = run(git(folder, "diff-tree", "--no-commit-id", "-r", "-M", "--name-status", "-z", "--root", "--diff-merges=first-parent", hash))
        if (result.exitCode != 0) return null

        return parseGitNameStatus(result.output)
    }

    // 이름 바꿈은 옛 경로도 pathspec 에 줘야 한 구획으로 짝지어져서 먼저 목록을 읽는다. 그 커밋에 없는 경로는 show 가 128 이고
    // 그것이 곧 지운 파일이다(docs/platform/jvm.html#terminal-commit-file).
    private suspend fun readCommitFile(path: String, hash: String): GitCommitFile? {
        val file = File(expandHome(path, home)).absoluteFile
        val folder = generateSequence(file.parentFile) { it.parentFile }.firstOrNull { it.isDirectory } ?: return null
        if (!hasGitAncestor(folder)) return null
        val rootResult = run(git(folder, "rev-parse", "--show-toplevel"))
        val root = File(rootResult.output.trim().takeIf { rootResult.exitCode == 0 && it.isNotEmpty() } ?: return null)
        // show-toplevel 은 심볼릭 링크를 푼 경로라 파일 쪽도 같은 방식으로 맞춘다.
        val relative = file.canonicalFile.relativeTo(root.canonicalFile).path.takeIf { !it.startsWith("..") } ?: return null

        val changes = readNameStatus(root, hash) ?: return null
        val paths = listOfNotNull(relative, changes.firstOrNull { it.path == relative }?.originalPath)

        val shown = run(git(root, "show", "$hash:$relative"))
        val content = if (shown.exitCode == 0) {
            val bytes = shown.outputBytes
            val truncated = bytes.size > FileContent.FileViewerMaxBytes
            fileContentOf(if (truncated) bytes.copyOf(FileContent.FileViewerMaxBytes.toInt()) else bytes, truncated)
        } else {
            FileContent.Unreadable
        }

        val patch = run(
            git(
                root,
                "-c", "core.quotePath=false",
                "diff-tree", "--no-commit-id", "-r", "-M", "--root", "--diff-merges=first-parent",
                "-p", "-U0", "--no-color", "--no-ext-diff", hash, "--", *paths.toTypedArray(),
            ),
        )
        if (patch.exitCode != 0) return null

        return GitCommitFile(content = content, diff = parseGitCommitDiff(patch.output, relative))
    }

    // 경로를 있는 가장 가까운 폴더 기준으로 주면 최상위를 따로 읽지 않는다. 폴더째 지운 파일도 그 위 폴더에서 읽는다
    // (docs/platform/jvm.html#terminal-file-diff).
    private suspend fun readFileDiff(path: String): GitFileDiff? {
        val file = File(expandHome(path, home)).absoluteFile
        val folder = generateSequence(file.parentFile) { it.parentFile }.firstOrNull { it.isDirectory } ?: return null
        if (!hasGitAncestor(folder)) return null
        val name = file.relativeTo(folder).path

        val hasHead = run(git(folder, "rev-parse", "--verify", "--quiet", "HEAD")).exitCode == 0
        if (hasHead) {
            val tracked = run(git(folder, "--no-optional-locks", "diff", "--no-color", "--no-ext-diff", "-U0", "HEAD", "--", name))
            if (tracked.exitCode != 0) return null
            if (tracked.output.isNotBlank()) return parseGitDiff(tracked.output)
        }

        // HEAD 에 없는 파일. 커밋이 없는 저장소면 index 에 넣은 파일도 새 파일이다(docs/common/terminal-file-diff.html D3).
        val scope = if (hasHead) listOf("--others") else listOf("--cached", "--others")
        val listed = run(git(folder, "ls-files", *scope.toTypedArray(), "--exclude-standard", "--", name))
        if (listed.exitCode != 0) return null
        if (listed.output.isBlank() || !file.isFile) return GitFileDiff(emptyList())

        // --no-index 는 차이가 있으면 1 로 끝난다.
        val untracked = run(git(folder, "diff", "--no-index", "--no-color", "--no-ext-diff", "-U0", "--", "/dev/null", name))
        return if (untracked.exitCode in 0..1) parseGitDiff(untracked.output) else null
    }

    private suspend fun readRoot(directory: String): String? {
        val folder = File(expandHome(directory, home))
        if (!folder.isDirectory || !hasGitAncestor(folder)) return null

        val result = run(git(folder, "rev-parse", "--show-toplevel"))
        return result.output.trim().takeIf { result.exitCode == 0 && it.isNotEmpty() }
    }

    // .git 을 위로 찾는 것은 파일 stat 몇 번이라 저장소가 아닌 폴더(대부분의 패널)는 프로세스 없이 끝난다.
    private suspend fun readWorktree(directory: String): GitWorktree? {
        val folder = File(expandHome(directory, home))
        if (!folder.isDirectory || !hasGitAncestor(folder)) return null

        val result = run(git(folder, "rev-parse", "--show-toplevel", "--git-common-dir"))
        if (result.exitCode != 0) return null
        val worktree = parseWorktree(result.output, folder) ?: return null

        // rev-parse 에 --abbrev-ref HEAD 를 합치면 커밋 없는 저장소(unborn)에서 명령 전체가 실패해 저장소 판정까지
        // 깨진다. branch --show-current 는 unborn 이면 이름, detached 면 빈 줄이다(docs/platform/jvm.html#terminal-worktree).
        val head = run(git(folder, "branch", "--show-current"))
        val branch = head.output.trim().takeIf { head.exitCode == 0 && it.isNotEmpty() }

        return worktree.copy(branch = branch)
    }
}

private fun git(directory: File, vararg args: String): List<String> = listOf("git", "-C", directory.path, *args)

private fun GitResult.failure(): GitWorktreeException? =
    if (exitCode == 0) null else GitWorktreeException(error.trim().ifEmpty { "git 이 $exitCode 로 끝났습니다" })

private fun hasGitAncestor(folder: File): Boolean =
    generateSequence(folder.absoluteFile) { it.parentFile }.any { File(it, ".git").exists() }

/**
 * `rev-parse --show-toplevel --git-common-dir` 의 두 줄. 공통 디렉터리는 [directory] 기준 상대 경로(`.git`)로
 * 올 수 있다. `.git` 으로 끝나면 그 부모가 main 워크트리, 아니면(bare 저장소) 그 자체다. 최상위는 git 이
 * 심볼릭 링크를 푼 경로로 주므로 공통 디렉터리도 같은 방식(canonical)으로 맞춘다.
 */
internal fun parseWorktree(output: String, directory: File): GitWorktree? {
    val lines = output.lineSequence().map { it.trim() }.filter { it.isNotEmpty() }.toList()
    if (lines.size < 2) return null

    val common = File(lines[1]).let { if (it.isAbsolute) it else File(directory, lines[1]) }.canonicalFile
    val main = if (common.name == ".git") common.parentFile ?: common else common

    return GitWorktree(path = lines[0], mainPath = main.path)
}

// 출력을 파이프로 읽으면 stderr 가 파이프를 채우는 동안 stdout 읽기가 막힐 수 있어 임시 파일로 받는다.
private suspend fun runGit(command: List<String>, timeout: Duration): GitResult =
    runInterruptible(Dispatchers.IO) {
        val output = Files.createTempFile("jarvis-git", ".out").toFile()
        val error = Files.createTempFile("jarvis-git", ".err").toFile()
        try {
            val builder = ProcessBuilder(command)
            // 앱을 터미널에서 띄우면 push 가 /dev/tty 로 사용자 이름·암호를 물으며 시간 초과까지 멈춘다. 묻지 않고 곧바로 실패하게 한다.
            // 자격 증명 도우미(osxkeychain)는 그대로 돈다.
            builder.environment()["GIT_TERMINAL_PROMPT"] = "0"
            val process = builder
                .redirectInput(ProcessBuilder.Redirect.from(File("/dev/null")))
                .redirectOutput(output)
                .redirectError(error)
                .start()

            if (!process.waitFor(timeout.inWholeSeconds, TimeUnit.SECONDS)) {
                process.destroyForcibly()
                return@runInterruptible GitResult(-1, "", "git 이 $timeout 안에 끝나지 않았습니다")
            }

            GitResult(process.exitValue(), output.readBytes(), error.readText())
        } catch (e: IOException) {
            GitResult(-1, "", "git 을 실행할 수 없습니다: ${e.message}")
        } finally {
            output.delete()
            error.delete()
        }
    }
