package io.github.taetae98coding.jarvis.data.terminal

import io.github.taetae98coding.jarvis.domain.terminal.GitWorktree
import io.github.taetae98coding.jarvis.domain.terminal.GitWorktreeRepository
import kotlinx.coroutines.flow.Flow

internal class DefaultGitWorktreeRepository(
    private val dataSource: GitDataSource,
) : GitWorktreeRepository {
    override fun observeWorktree(directory: String): Flow<GitWorktree?> = dataSource.observeWorktree(directory)

    override suspend fun addWorktree(repositoryDirectory: String, branch: String, path: String, baseBranch: String?): Result<GitWorktree> =
        dataSource.addWorktree(repositoryDirectory, branch, path, baseBranch)

    override suspend fun removeWorktree(directory: String, deleteDirectory: Boolean): Result<Unit> =
        dataSource.removeWorktree(directory, deleteDirectory)
}
