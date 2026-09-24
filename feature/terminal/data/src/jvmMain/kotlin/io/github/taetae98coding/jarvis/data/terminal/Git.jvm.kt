package io.github.taetae98coding.jarvis.data.terminal

import io.github.taetae98coding.jarvis.data.state.observeByPolling
import io.github.taetae98coding.jarvis.domain.terminal.GitWorktree
import io.github.taetae98coding.jarvis.domain.terminal.GitWorktreeException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.util.concurrent.TimeUnit
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

internal actual fun createGitDataSource(): GitDataSource = ProcessGitDataSource()

/** 저장소가 되고 안 되는 일은 드물다. + 하나가 이만큼 늦게 뜬다(docs/platform/jvm.html#terminal-worktree). */
internal val GitWorktreePollInterval: Duration = 10.seconds

internal val GitCommandTimeout: Duration = 30.seconds

internal class GitResult(
    val exitCode: Int,
    val output: String,
    val error: String,
)

/**
 * `git` CLI 로 저장소를 판정하고 워크트리를 만든다. JGit 을 버린 이유는 docs/platform/jvm.html#terminal-worktree 에 있다.
 * [run] 은 테스트가 갈아 끼운다.
 */
internal class ProcessGitDataSource(
    private val home: String = System.getProperty("user.home"),
    private val run: suspend (command: List<String>) -> GitResult = ::runGit,
) : GitDataSource {
    override fun observeWorktree(directory: String): Flow<GitWorktree?> =
        observeByPolling(interval = GitWorktreePollInterval) { readWorktree(directory) }.flowOn(Dispatchers.IO)

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
private suspend fun runGit(command: List<String>): GitResult =
    runInterruptible(Dispatchers.IO) {
        val output = Files.createTempFile("jarvis-git", ".out").toFile()
        val error = Files.createTempFile("jarvis-git", ".err").toFile()
        try {
            val process = ProcessBuilder(command)
                .redirectInput(ProcessBuilder.Redirect.from(File("/dev/null")))
                .redirectOutput(output)
                .redirectError(error)
                .start()

            if (!process.waitFor(GitCommandTimeout.inWholeSeconds, TimeUnit.SECONDS)) {
                process.destroyForcibly()
                return@runInterruptible GitResult(-1, "", "git 이 $GitCommandTimeout 안에 끝나지 않았습니다")
            }

            GitResult(process.exitValue(), output.readText(), error.readText())
        } catch (e: IOException) {
            GitResult(-1, "", "git 을 실행할 수 없습니다: ${e.message}")
        } finally {
            output.delete()
            error.delete()
        }
    }
