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

        assertEquals(GitWorktree(repository.path, repository.path), source.observeWorktree(repository.path).first())
        assertEquals(GitWorktree(repository.path, repository.path), source.observeWorktree(nested.path).first())
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

        val worktree = source.addWorktree(repository.path, "feature/login", path.path).getOrThrow()

        assertEquals(path.canonicalPath, worktree.path)
        assertEquals(repository.path, worktree.mainPath)
        assertTrue(File(path, ".git").isFile)
        assertEquals(worktree, source.observeWorktree(path.path).first())
        assertEquals("feature/login", currentBranch(path))
    }

    @Test
    fun addWorktreeChecksOutAnExistingBranch() = runTest {
        if (!gitAvailable) return@runTest
        val repository = newRepository()
        git(repository, "branch", "existing")
        val path = File(repository.parentFile, "${repository.name}-existing")

        val worktree = source.addWorktree(repository.path, "existing", path.path).getOrThrow()

        assertEquals(path.canonicalPath, worktree.path)
        assertEquals("existing", currentBranch(path))
    }

    @Test
    fun gitRefusalIsReturnedAsTheErrorMessage() = runTest {
        if (!gitAvailable) return@runTest
        val repository = newRepository()

        // main 은 저장소 자신이 체크아웃하고 있어 다른 워크트리에 다시 낼 수 없다.
        val result = source.addWorktree(repository.path, "main", File(repository.parentFile, "${repository.name}-main").path)

        val error = assertIs<GitWorktreeException>(result.exceptionOrNull())
        assertTrue(error.message!!.contains("main"), error.message)
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

    private fun currentBranch(directory: File): String {
        val process = ProcessBuilder("git", "-C", directory.path, "branch", "--show-current").start()
        return process.inputStream.bufferedReader().readText().trim().also { process.waitFor() }
    }
}
