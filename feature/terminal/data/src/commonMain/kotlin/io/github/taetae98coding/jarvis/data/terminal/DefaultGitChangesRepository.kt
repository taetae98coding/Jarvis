package io.github.taetae98coding.jarvis.data.terminal

import io.github.taetae98coding.jarvis.domain.terminal.GitChange
import io.github.taetae98coding.jarvis.domain.terminal.GitChangesRepository
import io.github.taetae98coding.jarvis.domain.terminal.GitGraphLine
import io.github.taetae98coding.jarvis.domain.terminal.GitPushTarget
import io.github.taetae98coding.jarvis.domain.terminal.GitStatus
import kotlinx.coroutines.flow.Flow

internal class DefaultGitChangesRepository(
    private val dataSource: GitDataSource,
) : GitChangesRepository {
    override fun observeStatus(directory: String): Flow<GitStatus?> = dataSource.observeStatus(directory)

    override fun observeGraph(directory: String): Flow<List<GitGraphLine>> = dataSource.observeGraph(directory)

    override suspend fun stage(root: String, changes: List<GitChange>): Result<Unit> = dataSource.stage(root, changes)

    override suspend fun unstage(root: String, changes: List<GitChange>): Result<Unit> = dataSource.unstage(root, changes)

    override suspend fun push(root: String, target: GitPushTarget): Result<Unit> = dataSource.push(root, target)
}
