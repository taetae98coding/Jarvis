package io.github.taetae98coding.jarvis.data.terminal

import io.github.taetae98coding.jarvis.domain.terminal.GitWorktree
import io.github.taetae98coding.jarvis.domain.terminal.GitWorktreeException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

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
