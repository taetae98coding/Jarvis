package io.github.taetae98coding.jarvis.data.terminal

import io.github.taetae98coding.jarvis.domain.terminal.GitChange
import io.github.taetae98coding.jarvis.domain.terminal.GitGraphLine
import io.github.taetae98coding.jarvis.domain.terminal.GitStatus
import io.github.taetae98coding.jarvis.domain.terminal.GitWorktree
import io.github.taetae98coding.jarvis.domain.terminal.GitWorktreeException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

internal interface GitDataSource {
    fun observeWorktree(directory: String): Flow<GitWorktree?>

    suspend fun addWorktree(repositoryDirectory: String, branch: String, path: String, baseBranch: String?): Result<GitWorktree>

    suspend fun removeWorktree(directory: String, deleteDirectory: Boolean): Result<Unit>

    fun observeStatus(directory: String): Flow<GitStatus?>

    fun observeGraph(directory: String): Flow<List<GitGraphLine>>

    suspend fun stage(root: String, changes: List<GitChange>): Result<Unit>

    suspend fun unstage(root: String, changes: List<GitChange>): Result<Unit>
}

/** git 을 실행할 수 없는 타깃. 어떤 폴더도 저장소가 아니라서 패널에 + 가 없고 사이드 바의 Git 구획은 "git 저장소가 아닙니다" 다. */
internal object UnsupportedGitDataSource : GitDataSource {
    override fun observeWorktree(directory: String): Flow<GitWorktree?> = flowOf(null)

    override suspend fun addWorktree(repositoryDirectory: String, branch: String, path: String, baseBranch: String?): Result<GitWorktree> =
        unsupported()

    override suspend fun removeWorktree(directory: String, deleteDirectory: Boolean): Result<Unit> = unsupported()

    override fun observeStatus(directory: String): Flow<GitStatus?> = flowOf(null)

    override fun observeGraph(directory: String): Flow<List<GitGraphLine>> = flowOf(emptyList())

    override suspend fun stage(root: String, changes: List<GitChange>): Result<Unit> = unsupported()

    override suspend fun unstage(root: String, changes: List<GitChange>): Result<Unit> = unsupported()

    private fun <T> unsupported(): Result<T> = Result.failure(GitWorktreeException("이 플랫폼에서는 git 을 쓸 수 없습니다"))
}

/**
 * `git` 을 띄운다. JVM 만 실제로 띄우고 Android(기기에 git 이 없다)·iOS·Web 은 [UnsupportedGitDataSource] 다.
 * 판정 근거는 docs/common/terminal-worktree.html#platforms 에 있다.
 */
internal expect fun createGitDataSource(): GitDataSource
