package io.github.taetae98coding.jarvis.data.terminal

import io.github.taetae98coding.jarvis.domain.terminal.FileContent
import io.github.taetae98coding.jarvis.domain.terminal.GitChange
import io.github.taetae98coding.jarvis.domain.terminal.GitChangeKind
import io.github.taetae98coding.jarvis.domain.terminal.GitDiffHunk
import io.github.taetae98coding.jarvis.domain.terminal.GitFileDiff
import io.github.taetae98coding.jarvis.domain.terminal.GitPushTarget
import io.github.taetae98coding.jarvis.domain.terminal.GitWorktree
import io.github.taetae98coding.jarvis.domain.terminal.GitWorktreeException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.produceIn
import kotlinx.coroutines.test.runTest
import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.hours

/** 임시 폴더에 진짜 저장소를 만들어 `git` 과 왕복한다. `git` 이 없는 머신에서는 확인할 것이 없어 곧바로 끝낸다. */
class GitDataSourceTest {
    private val gitAvailable: Boolean = runCatching {
        ProcessBuilder("git", "--version").redirectErrorStream(true).start().waitFor() == 0
    }.getOrDefault(false)

    private fun newDirectory(): File = Files.createTempDirectory("jarvis-git").toFile().canonicalFile

    private fun git(directory: File, vararg args: String) {
        val process = ProcessBuilder("git", "-C", directory.path, *args).redirectErrorStream(true).start()
        val output = process.inputStream.bufferedReader().readText()
        check(process.waitFor() == 0) { "git ${args.joinToString(" ")} failed: $output" }
    }

    private fun newRepository(): File =
        newDirectory().also {
            git(it, "init", "-q", "-b", "main")
            git(it, "-c", "user.name=test", "-c", "user.email=test@example.com", "commit", "-q", "--allow-empty", "-m", "init")
        }

    private val source = ProcessGitDataSource(home = System.getProperty("user.home"))

    @Test
    fun aRepositoryFolderIsItsOwnMainWorktree() = runTest {
        if (!gitAvailable) return@runTest
        val repository = newRepository()
        val nested = File(repository, "src/main").apply { mkdirs() }

        assertEquals(GitWorktree(repository.path, repository.path, "main"), source.observeWorktree(repository.path).first())
        assertEquals(GitWorktree(repository.path, repository.path, "main"), source.observeWorktree(nested.path).first())
    }

    @Test
    fun theCurrentBranchFollowsHeadAndIsNullWhenDetached() = runTest {
        if (!gitAvailable) return@runTest
        val repository = newRepository()
        git(repository, "switch", "-q", "-c", "work")

        assertEquals("work", source.observeWorktree(repository.path).first()!!.branch)

        git(repository, "switch", "-q", "--detach")

        assertNull(source.observeWorktree(repository.path).first()!!.branch)
    }

    // 커밋이 없는 새 저장소도 저장소이고 브랜치 이름이 있다. rev-parse HEAD 는 여기서 실패한다.
    @Test
    fun aRepositoryWithoutCommitsStillReportsItsBranch() = runTest {
        if (!gitAvailable) return@runTest
        val repository = newDirectory().also { git(it, "init", "-q", "-b", "main") }

        assertEquals(GitWorktree(repository.path, repository.path, "main"), source.observeWorktree(repository.path).first())
    }

    @Test
    fun foldersOutsideARepositoryAreNull() = runTest {
        if (!gitAvailable) return@runTest

        assertNull(source.observeWorktree(newDirectory().path).first())
        assertNull(source.observeWorktree(File(newDirectory(), "missing").path).first())
    }

    @Test
    fun addWorktreeCreatesANewBranchFromHeadAndReportsTheMainWorktree() = runTest {
        if (!gitAvailable) return@runTest
        val repository = newRepository()
        val path = File(repository.parentFile, "${repository.name}-worktrees/feature/login")

        val worktree = source.addWorktree(repository.path, "feature/login", path.path, baseBranch = null).getOrThrow()

        assertEquals(path.canonicalPath, worktree.path)
        assertEquals(repository.path, worktree.mainPath)
        assertEquals("feature/login", worktree.branch)
        assertTrue(File(path, ".git").isFile)
        assertEquals(worktree, source.observeWorktree(path.path).first())
        assertEquals("feature/login", currentBranch(path))
    }

    @Test
    fun addWorktreeStartsTheNewBranchAtTheBaseBranch() = runTest {
        if (!gitAvailable) return@runTest
        val repository = newRepository()
        git(repository, "branch", "release")
        git(repository, "-c", "user.name=test", "-c", "user.email=test@example.com", "commit", "-q", "--allow-empty", "-m", "after release")
        val path = File(repository.parentFile, "${repository.name}-worktrees/hotfix")

        val worktree = source.addWorktree(repository.path, "hotfix", path.path, baseBranch = "release").getOrThrow()

        assertEquals("hotfix", worktree.branch)
        assertEquals(revision(repository, "release"), revision(path, "HEAD"))
        assertTrue(revision(repository, "main") != revision(path, "HEAD"))
    }

    @Test
    fun anUnknownBaseBranchIsGitsErrorMessage() = runTest {
        if (!gitAvailable) return@runTest
        val repository = newRepository()
        val path = File(repository.parentFile, "${repository.name}-worktrees/fix")

        val result = source.addWorktree(repository.path, "fix", path.path, baseBranch = "nope")

        val error = assertIs<GitWorktreeException>(result.exceptionOrNull())
        assertTrue(error.message!!.contains("nope"), error.message)
        assertTrue(!path.exists())
    }

    @Test
    fun addWorktreeChecksOutAnExistingBranchIgnoringTheBaseBranch() = runTest {
        if (!gitAvailable) return@runTest
        val repository = newRepository()
        git(repository, "branch", "existing")
        val path = File(repository.parentFile, "${repository.name}-existing")

        val worktree = source.addWorktree(repository.path, "existing", path.path, baseBranch = "nope").getOrThrow()

        assertEquals(path.canonicalPath, worktree.path)
        assertEquals("existing", worktree.branch)
        assertEquals("existing", currentBranch(path))
    }

    @Test
    fun gitRefusalIsReturnedAsTheErrorMessage() = runTest {
        if (!gitAvailable) return@runTest
        val repository = newRepository()

        // main 은 저장소 자신이 체크아웃하고 있어 다른 워크트리에 다시 낼 수 없다.
        val result = source.addWorktree(repository.path, "main", File(repository.parentFile, "${repository.name}-main").path, baseBranch = null)

        val error = assertIs<GitWorktreeException>(result.exceptionOrNull())
        assertTrue(error.message!!.contains("main"), error.message)
    }

    /** [branch] 를 새로 만든 연결된 워크트리. */
    private fun newWorktree(repository: File, branch: String): File =
        File(repository.parentFile, "${repository.name}-worktrees/$branch").also { git(repository, "worktree", "add", "-q", "-b", branch, it.path) }

    private fun worktreePaths(repository: File): List<String> =
        read(repository, "worktree", "list", "--porcelain").lines().filter { it.startsWith("worktree ") }.map { it.removePrefix("worktree ") }

    private fun branchExists(repository: File, branch: String): Boolean =
        ProcessBuilder("git", "-C", repository.path, "rev-parse", "--verify", "--quiet", "refs/heads/$branch").start().waitFor() == 0

    @Test
    fun removeWorktreeDeletesTheFolderTheRecordAndTheCurrentBranch() = runTest {
        if (!gitAvailable) return@runTest
        val repository = newRepository()
        val path = newWorktree(repository, "feature")
        // 셸에서 바꾼 것처럼 지금 체크아웃한 브랜치가 지워진다.
        git(path, "switch", "-q", "-c", "switched")
        git(path, "-c", "user.name=test", "-c", "user.email=test@example.com", "commit", "-q", "--allow-empty", "-m", "unmerged")

        source.removeWorktree(path.path, deleteDirectory = true).getOrThrow()

        assertTrue(!path.exists())
        assertEquals(listOf(repository.path), worktreePaths(repository))
        assertTrue(!branchExists(repository, "switched"))
        assertTrue(branchExists(repository, "feature"))
    }

    @Test
    fun removeWorktreeCanKeepTheFilesAsAPlainFolder() = runTest {
        if (!gitAvailable) return@runTest
        val repository = newRepository()
        val path = newWorktree(repository, "keep")
        File(path, "notes.txt").writeText("draft")

        source.removeWorktree(path.path, deleteDirectory = false).getOrThrow()

        assertEquals("draft", File(path, "notes.txt").readText())
        assertTrue(!File(path, ".git").exists())
        assertEquals(listOf(repository.path), worktreePaths(repository))
        assertTrue(!branchExists(repository, "keep"))
    }

    @Test
    fun aFolderWithChangesIsNotDeletedAndNothingIsRemoved() = runTest {
        if (!gitAvailable) return@runTest
        val repository = newRepository()
        val path = newWorktree(repository, "dirty")
        File(path, "untracked.txt").writeText("work")

        val result = source.removeWorktree(path.path, deleteDirectory = true)

        val error = assertIs<GitWorktreeException>(result.exceptionOrNull())
        assertTrue(error.message!!.contains("untracked"), error.message)
        assertTrue(File(path, "untracked.txt").exists())
        assertEquals(listOf(repository.path, path.canonicalPath), worktreePaths(repository))
        assertTrue(branchExists(repository, "dirty"))
    }

    @Test
    fun aDetachedWorktreeIsRemovedWithoutTouchingBranches() = runTest {
        if (!gitAvailable) return@runTest
        val repository = newRepository()
        val path = newWorktree(repository, "detached")
        git(path, "switch", "-q", "--detach")

        source.removeWorktree(path.path, deleteDirectory = true).getOrThrow()

        assertTrue(!path.exists())
        assertTrue(branchExists(repository, "detached"))
        assertTrue(branchExists(repository, "main"))
    }

    @Test
    fun theMainWorktreeAndFoldersOutsideARepositoryAreRefused() = runTest {
        if (!gitAvailable) return@runTest
        val repository = newRepository()

        assertIs<GitWorktreeException>(source.removeWorktree(repository.path, deleteDirectory = true).exceptionOrNull())
        assertIs<GitWorktreeException>(source.removeWorktree(repository.path, deleteDirectory = false).exceptionOrNull())
        assertIs<GitWorktreeException>(source.removeWorktree(newDirectory().path, deleteDirectory = true).exceptionOrNull())
        assertTrue(File(repository, ".git").isDirectory)
        assertTrue(branchExists(repository, "main"))
    }

    private fun commit(directory: File, message: String) =
        git(directory, "-c", "user.name=test", "-c", "user.email=test@example.com", "commit", "-q", "--allow-empty", "-m", message)

    @Test
    fun statusSplitsStagedAndUnstagedChangesOfTheWholeRepository() = runTest {
        if (!gitAvailable) return@runTest
        val repository = newRepository()
        File(repository, "a.txt").writeText("a")
        git(repository, "add", "a.txt")
        commit(repository, "a")
        File(repository, "a.txt").writeText("changed")
        File(repository, "b.txt").writeText("b")
        git(repository, "add", "b.txt")
        val nested = File(repository, "src/new").apply { mkdirs() }
        File(nested, "c.txt").writeText("c")

        val status = source.observeStatus(nested.path).first()!!

        assertEquals(repository.path, status.root)
        assertEquals("main", status.branch)
        assertEquals(listOf(GitChange("b.txt", GitChangeKind.Added)), status.staged)
        assertEquals(listOf(GitChange("a.txt", GitChangeKind.Modified), GitChange("src/new/c.txt", GitChangeKind.Untracked)), status.unstaged)
    }

    @Test
    fun stageAndUnstageChangeTheIndexAndTheObservationFollowsWithoutWaitingForPolling() = runTest {
        if (!gitAvailable) return@runTest
        val repository = newRepository()
        File(repository, "a.txt").writeText("a")
        val slow = ProcessGitDataSource(home = System.getProperty("user.home"), changesPollInterval = 1.hours)
        val values = slow.observeStatus(repository.path).produceIn(backgroundScope)
        assertEquals(listOf("a.txt"), values.receive()!!.unstaged.map { it.path })

        assertTrue(slow.stage(repository.path, listOf(GitChange("a.txt", GitChangeKind.Untracked))).isSuccess)
        assertEquals(listOf(GitChange("a.txt", GitChangeKind.Added)), values.receive()!!.staged)

        assertTrue(slow.unstage(repository.path, listOf(GitChange("a.txt", GitChangeKind.Added))).isSuccess)
        val unstaged = values.receive()!!
        assertEquals(emptyList(), unstaged.staged)
        assertEquals(listOf(GitChange("a.txt", GitChangeKind.Untracked)), unstaged.unstaged)
    }

    @Test
    fun unstagingARenameRestoresBothPaths() = runTest {
        if (!gitAvailable) return@runTest
        val repository = newRepository()
        File(repository, "a.txt").writeText("a")
        git(repository, "add", "a.txt")
        commit(repository, "a")
        git(repository, "mv", "a.txt", "b.txt")
        val rename = source.observeStatus(repository.path).first()!!.staged.single()
        assertEquals(GitChange("b.txt", GitChangeKind.Renamed, originalPath = "a.txt"), rename)

        assertTrue(source.unstage(repository.path, listOf(rename)).isSuccess)

        val status = source.observeStatus(repository.path).first()!!
        assertEquals(emptyList(), status.staged)
        assertEquals(listOf(GitChange("a.txt", GitChangeKind.Deleted), GitChange("b.txt", GitChangeKind.Untracked)), status.unstaged)
    }

    // restore --staged 는 HEAD 가 없으면 실패한다. 커밋 전의 첫 stage 도 되돌릴 수 있어야 한다.
    @Test
    fun unstagingWorksBeforeTheFirstCommit() = runTest {
        if (!gitAvailable) return@runTest
        val repository = newDirectory().also { git(it, "init", "-q", "-b", "main") }
        File(repository, "a.txt").writeText("a")
        git(repository, "add", "a.txt")

        assertTrue(source.unstage(repository.path, listOf(GitChange("a.txt", GitChangeKind.Added))).isSuccess)

        val status = source.observeStatus(repository.path).first()!!
        assertEquals("main", status.branch)
        assertEquals(emptyList(), status.staged)
        assertEquals(listOf(GitChange("a.txt", GitChangeKind.Untracked)), status.unstaged)
    }

    @Test
    fun stageFailureIsGitsMessage() = runTest {
        if (!gitAvailable) return@runTest
        val repository = newRepository()

        val failure = source.stage(repository.path, listOf(GitChange("missing.txt", GitChangeKind.Modified))).exceptionOrNull()

        assertIs<GitWorktreeException>(failure)
        assertTrue(failure.message!!.contains("missing.txt"), failure.message)
    }

    @Test
    fun graphShowsOnlyCommitsReachableFromHead() = runTest {
        if (!gitAvailable) return@runTest
        val repository = newRepository()
        git(repository, "switch", "-q", "-c", "side")
        commit(repository, "side work")
        git(repository, "switch", "-q", "-c", "unmerged", "main")
        commit(repository, "unmerged work")
        git(repository, "tag", "unreachable-tag")
        git(repository, "switch", "-q", "main")
        commit(repository, "main work")
        git(repository, "-c", "user.name=test", "-c", "user.email=test@example.com", "merge", "-q", "--no-ff", "side", "-m", "merge side")

        val lines = source.observeGraph(repository.path).first()
        val titles = lines.filter { it.commit != null && !it.isDetail }

        // 같은 초에 만든 두 가지 커밋은 날짜순이 정해지지 않는다. 처음과 끝, 모인 것만 본다.
        // 다른 브랜치·태그에서만 닿는 "unmerged work" 는 없다(R18).
        assertEquals(setOf("merge side", "main work", "side work", "init"), titles.map { it.commit!!.subject }.toSet())
        assertEquals("merge side", titles.first().commit!!.subject)
        assertEquals("init", titles.last().commit!!.subject)
        assertEquals(listOf("HEAD -> main"), titles.first().commit!!.refs)
        assertTrue(titles.first().commit!!.isHead)
        assertTrue(titles.first().graph.startsWith("*"))
        assertEquals(titles.size, lines.count { it.isDetail })
        assertTrue(lines.any { it.graph.contains("\\") })
        assertEquals(listOf("side"), titles.single { it.commit!!.subject == "side work" }.commit!!.refs)
    }

    @Test
    fun foldersOutsideARepositoryHaveNoStatusAndNoGraph() = runTest {
        if (!gitAvailable) return@runTest
        val outside = newDirectory()
        val unborn = newDirectory().also { git(it, "init", "-q", "-b", "main") }

        assertNull(source.observeStatus(outside.path).first())
        assertEquals(emptyList(), source.observeGraph(outside.path).first())
        assertEquals(emptyList(), source.observeGraph(unborn.path).first())
    }

    private fun head(directory: File): String = revision(directory, "HEAD")

    @Test
    fun commitFilesAreTheChangesAgainstTheFirstParentInPathOrder() = runTest {
        if (!gitAvailable) return@runTest
        val repository = newDirectory().also { git(it, "init", "-q", "-b", "main") }
        File(repository, "a.txt").writeText("a")
        File(repository, "old.txt").writeText("old")
        git(repository, "add", "-A")
        commit(repository, "root")
        val root = head(repository)

        File(repository, "a.txt").writeText("changed")
        git(repository, "mv", "old.txt", "new.txt")
        File(repository, "dir").mkdirs()
        File(repository, "dir/b.txt").writeText("b")
        git(repository, "add", "-A")
        commit(repository, "work")
        val work = head(repository)

        // 첫 커밋은 빈 트리 대비라 모두 추가다.
        assertEquals(
            listOf(GitChange("a.txt", GitChangeKind.Added), GitChange("old.txt", GitChangeKind.Added)),
            source.observeCommitFiles(repository.path, root).first(),
        )
        assertEquals(
            listOf(
                GitChange("a.txt", GitChangeKind.Modified),
                GitChange("dir/b.txt", GitChangeKind.Added),
                GitChange("new.txt", GitChangeKind.Renamed, "old.txt"),
            ),
            source.observeCommitFiles(File(repository, "dir").path, work).first(),
        )
    }

    @Test
    fun aMergeCommitListsWhatItBroughtInAndAnEmptyCommitNothing() = runTest {
        if (!gitAvailable) return@runTest
        val repository = newRepository()
        git(repository, "switch", "-q", "-c", "side")
        File(repository, "side.txt").writeText("side")
        git(repository, "add", "-A")
        commit(repository, "side work")
        git(repository, "switch", "-q", "main")
        File(repository, "main.txt").writeText("main")
        git(repository, "add", "-A")
        commit(repository, "main work")
        git(repository, "merge", "-q", "--no-ff", "-m", "merge side", "side")

        assertEquals(listOf(GitChange("side.txt", GitChangeKind.Added)), source.observeCommitFiles(repository.path, head(repository)).first())
        assertEquals(emptyList(), source.observeCommitFiles(repository.path, revision(repository, "main~2")).first())
    }

    // docs/common/terminal-commit-file.html K3–K6
    @Test
    fun aCommitFileIsItsContentAtThatCommitWithTheDiffAgainstTheFirstParent() = runTest {
        if (!gitAvailable) return@runTest
        val repository = newDirectory().also { git(it, "init", "-q", "-b", "main") }
        File(repository, "a.txt").writeText("a\nb\nc\n")
        File(repository, "old.txt").writeText("l1\nl2\nl3\nl4\nl5\n")
        File(repository, "gone.txt").writeText("x\ny\n")
        git(repository, "add", "-A")
        commit(repository, "root")
        val root = head(repository)

        File(repository, "a.txt").writeText("a\nB\nc\nd\n")
        git(repository, "mv", "old.txt", "새 이름.txt")
        File(repository, "새 이름.txt").appendText("l6\n")
        git(repository, "rm", "-q", "gone.txt")
        git(repository, "add", "-A")
        commit(repository, "work")
        val work = head(repository)
        // 작업 트리를 더 바꿔도 커밋 시점의 내용이다.
        File(repository, "a.txt").writeText("changed again\n")

        val modified = source.observeCommitFile(File(repository, "a.txt").path, work).first()!!
        assertEquals(FileContent.Text("a\nB\nc\nd\n", truncated = false), modified.content)
        assertEquals(listOf(GitDiffHunk(2, 1, 2, 1, listOf("b")), GitDiffHunk(3, 0, 4, 1, emptyList())), modified.diff.hunks)

        val renamed = source.observeCommitFile(File(repository, "새 이름.txt").path, work).first()!!
        assertEquals(FileContent.Text("l1\nl2\nl3\nl4\nl5\nl6\n", truncated = false), renamed.content)
        assertEquals(listOf(GitDiffHunk(5, 0, 6, 1, emptyList())), renamed.diff.hunks)

        val deleted = source.observeCommitFile(File(repository, "gone.txt").path, work).first()!!
        assertEquals(FileContent.Unreadable, deleted.content)
        assertEquals(listOf(GitDiffHunk(1, 2, 0, 0, listOf("x", "y"))), deleted.diff.hunks)

        val first = source.observeCommitFile(File(repository, "a.txt").path, root).first()!!
        assertEquals(FileContent.Text("a\nb\nc\n", truncated = false), first.content)
        assertEquals(listOf(GitDiffHunk(0, 0, 1, 3, emptyList())), first.diff.hunks)

        assertNull(source.observeCommitFile(File(repository, "a.txt").path, "0123456789012345678901234567890123456789").first())
        assertNull(source.observeCommitFile(File(newDirectory(), "a.txt").path, work).first())
    }

    @Test
    fun anUnknownCommitOrAFolderOutsideARepositoryHasNoCommitFiles() = runTest {
        if (!gitAvailable) return@runTest
        val repository = newRepository()

        assertNull(source.observeCommitFiles(repository.path, "0123456789012345678901234567890123456789").first())
        assertNull(source.observeCommitFiles(newDirectory().path, head(repository)).first())
    }

    /** [repository] 에 `origin` 으로 붙인 빈 bare 저장소. */
    private fun addRemote(repository: File, name: String = "origin"): File =
        newDirectory().also {
            git(it, "init", "-q", "--bare")
            git(repository, "remote", "add", name, it.path)
        }

    private suspend fun pushTarget(directory: File): GitPushTarget? = source.observeStatus(directory.path).first()!!.pushTarget

    @Test
    fun aBranchNotYetOnTheRemoteIsPublishedAndTracksIt() = runTest {
        if (!gitAvailable) return@runTest
        val repository = newRepository()
        val remote = addRemote(repository)
        val target = pushTarget(repository)
        assertEquals(GitPushTarget("origin", "main", exists = false), target)

        assertTrue(source.push(repository.path, target!!).isSuccess)

        assertEquals(revision(repository, "HEAD"), revision(remote, "main"))
        assertEquals("origin/main", read(repository, "rev-parse", "--abbrev-ref", "main@{upstream}"))
        assertEquals(GitPushTarget("origin", "main", exists = true), pushTarget(repository))
    }

    @Test
    fun newCommitsAreAheadAndPushingCatchesUp() = runTest {
        if (!gitAvailable) return@runTest
        val repository = newRepository()
        val remote = addRemote(repository)
        git(repository, "push", "-q", "-u", "origin", "main")
        commit(repository, "one")
        commit(repository, "two")

        val target = pushTarget(repository)!!
        assertEquals(GitPushTarget("origin", "main", exists = true, ahead = 2), target)
        assertTrue(target.canPush)

        assertTrue(source.push(repository.path, target).isSuccess)

        assertEquals(revision(repository, "HEAD"), revision(remote, "main"))
        assertEquals(GitPushTarget("origin", "main", exists = true, ahead = 0), pushTarget(repository))
    }

    @Test
    fun aNewWorktreeBranchIsPublishedFromTheWorktree() = runTest {
        if (!gitAvailable) return@runTest
        val repository = newRepository()
        val remote = addRemote(repository)
        git(repository, "push", "-q", "-u", "origin", "main")
        val worktree = newWorktree(repository, "feature/login")
        commit(worktree, "login")

        val target = pushTarget(worktree)!!
        assertEquals(GitPushTarget("origin", "feature/login", exists = false), target)

        assertTrue(source.push(worktree.path, target).isSuccess)

        assertEquals(revision(worktree, "HEAD"), revision(remote, "feature/login"))
        assertEquals(revision(repository, "main"), revision(remote, "main"))
        assertEquals("origin/feature/login", read(worktree, "rev-parse", "--abbrev-ref", "feature/login@{upstream}"))
    }

    // 원격 추적 브랜치에서 갈라 만든 워크트리는 upstream 이 origin/main 이다. 비교·push 는 같은 이름 브랜치로 한다.
    @Test
    fun aBranchTrackingAnotherNameIsComparedWithItsOwnNameAndRetracked() = runTest {
        if (!gitAvailable) return@runTest
        val repository = newRepository()
        addRemote(repository)
        git(repository, "push", "-q", "-u", "origin", "main")
        val worktree = File(repository.parentFile, "${repository.name}-worktrees/feat")
        git(repository, "worktree", "add", "-q", "-b", "feat", worktree.path, "origin/main")
        commit(worktree, "feat")
        assertEquals("origin/main", read(worktree, "rev-parse", "--abbrev-ref", "feat@{upstream}"))

        val target = pushTarget(worktree)!!
        assertEquals(GitPushTarget("origin", "feat", exists = false), target)
        assertTrue(source.push(worktree.path, target).isSuccess)

        assertEquals("origin/feat", read(worktree, "rev-parse", "--abbrev-ref", "feat@{upstream}"))
        commit(worktree, "more")
        assertEquals(GitPushTarget("origin", "feat", exists = true, ahead = 1), pushTarget(worktree))
    }

    @Test
    fun aRemoteThatMovedAheadRejectsThePushWithGitsMessage() = runTest {
        if (!gitAvailable) return@runTest
        val repository = newRepository()
        val remote = addRemote(repository)
        git(repository, "push", "-q", "-u", "origin", "main")
        val other = newDirectory().also { git(it.parentFile, "clone", "-q", remote.path, it.path) }
        commit(other, "elsewhere")
        git(other, "push", "-q", "origin", "main")
        commit(repository, "here")
        git(repository, "fetch", "-q")

        val target = pushTarget(repository)!!
        assertEquals(GitPushTarget("origin", "main", exists = true, ahead = 1, behind = 1), target)

        val failure = source.push(repository.path, target).exceptionOrNull()

        assertIs<GitWorktreeException>(failure)
        assertTrue(failure.message!!.contains("rejected"), failure.message)
        assertEquals(revision(other, "HEAD"), revision(remote, "main"))
    }

    @Test
    fun theUpstreamRemoteWinsOverOrigin() = runTest {
        if (!gitAvailable) return@runTest
        val repository = newRepository()
        addRemote(repository)
        addRemote(repository, name = "fork")
        git(repository, "push", "-q", "-u", "fork", "main")

        assertEquals(GitPushTarget("fork", "main", exists = true), pushTarget(repository))
    }

    @Test
    fun noRemoteDetachedAndUnbornHaveNoPushTarget() = runTest {
        if (!gitAvailable) return@runTest
        assertNull(pushTarget(newRepository()))

        val detached = newRepository().also { addRemote(it) }
        git(detached, "switch", "-q", "--detach")
        assertNull(pushTarget(detached))

        val unborn = newDirectory().also { git(it, "init", "-q", "-b", "main") }
        addRemote(unborn)
        assertNull(pushTarget(unborn))
    }

    @Test
    fun pushingRefreshesTheObservationWithoutWaitingForPolling() = runTest {
        if (!gitAvailable) return@runTest
        val repository = newRepository()
        addRemote(repository)
        val slow = ProcessGitDataSource(home = System.getProperty("user.home"), changesPollInterval = 1.hours)
        val values = slow.observeStatus(repository.path).produceIn(backgroundScope)
        val target = values.receive()!!.pushTarget!!

        assertTrue(slow.push(repository.path, target).isSuccess)

        assertEquals(GitPushTarget("origin", "main", exists = true), values.receive()!!.pushTarget)
    }

    @Test
    fun fileDiffIsAgainstHeadIncludingStagedChanges() = runTest {
        if (!gitAvailable) return@runTest
        val repository = newRepository()
        val file = File(repository, "src/f.txt").apply { parentFile.mkdirs() }
        file.writeText("a\nb\nc\nd\n")
        git(repository, "add", ".")
        commit(repository, "f")
        assertEquals(GitFileDiff(emptyList()), source.observeFileDiff(file.path).first())

        file.writeText("a\nB\nc\nd\ne\n")
        git(repository, "add", ".")

        assertEquals(
            GitFileDiff(
                listOf(
                    GitDiffHunk(oldStart = 2, oldCount = 1, newStart = 2, newCount = 1, removed = listOf("b")),
                    GitDiffHunk(oldStart = 4, oldCount = 0, newStart = 5, newCount = 1, removed = emptyList()),
                ),
            ),
            source.observeFileDiff(file.path).first(),
        )
    }

    @Test
    fun deletedFilesAreAllRemovedLinesEvenWhenTheirFolderIsGone() = runTest {
        if (!gitAvailable) return@runTest
        val repository = newRepository()
        val file = File(repository, "gone/f.txt").apply { parentFile.mkdirs() }
        file.writeText("a\nb\n")
        git(repository, "add", ".")
        commit(repository, "f")
        file.parentFile.deleteRecursively()

        assertEquals(
            GitFileDiff(listOf(GitDiffHunk(oldStart = 1, oldCount = 2, newStart = 0, newCount = 0, removed = listOf("a", "b")))),
            source.observeFileDiff(file.path).first(),
        )
    }

    @Test
    fun untrackedFilesAndFilesBeforeTheFirstCommitAreAllAddedLines() = runTest {
        if (!gitAvailable) return@runTest
        val allAdded = GitFileDiff(listOf(GitDiffHunk(oldStart = 0, oldCount = 0, newStart = 1, newCount = 2, removed = emptyList())))
        val repository = newRepository()
        val untracked = File(repository, "new.txt").apply { writeText("a\nb\n") }
        val unborn = newDirectory().also { git(it, "init", "-q", "-b", "main") }
        val staged = File(unborn, "staged.txt").apply { writeText("a\nb\n") }
        git(unborn, "add", "staged.txt")

        assertEquals(allAdded, source.observeFileDiff(untracked.path).first())
        assertEquals(allAdded, source.observeFileDiff(staged.path).first())
    }

    @Test
    fun ignoredFilesHaveNoDiffAndFilesOutsideARepositoryAreNull() = runTest {
        if (!gitAvailable) return@runTest
        val repository = newRepository()
        File(repository, ".gitignore").writeText("*.log\n")
        val ignored = File(repository, "app.log").apply { writeText("x\n") }
        val outside = File(newDirectory(), "f.txt").apply { writeText("x\n") }

        assertEquals(GitFileDiff(emptyList()), source.observeFileDiff(ignored.path).first())
        assertNull(source.observeFileDiff(outside.path).first())
    }

    @Test
    fun revParseOutputIsParsedWithRelativeAndAbsoluteCommonDirs() {
        val directory = newDirectory()
        val dotGit = File(directory, ".git").apply { mkdirs() }

        assertEquals(
            GitWorktree(directory.path, directory.path),
            parseWorktree("${directory.path}\n.git\n", directory),
        )
        assertEquals(
            GitWorktree("/elsewhere/linked", directory.path),
            parseWorktree("/elsewhere/linked\n${dotGit.path}\n", File("/elsewhere/linked")),
        )
        assertNull(parseWorktree("${directory.path}\n", directory))
    }

    private fun currentBranch(directory: File): String = read(directory, "branch", "--show-current")

    private fun revision(directory: File, ref: String): String = read(directory, "rev-parse", ref)

    private fun read(directory: File, vararg args: String): String {
        val process = ProcessBuilder("git", "-C", directory.path, *args).start()
        return process.inputStream.bufferedReader().readText().trim().also { process.waitFor() }
    }
}
