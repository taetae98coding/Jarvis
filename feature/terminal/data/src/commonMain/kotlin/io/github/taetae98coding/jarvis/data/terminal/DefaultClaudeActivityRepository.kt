package io.github.taetae98coding.jarvis.data.terminal

import io.github.taetae98coding.jarvis.domain.terminal.ClaudeActivity
import io.github.taetae98coding.jarvis.domain.terminal.ClaudeActivityRepository
import kotlinx.coroutines.flow.Flow

internal class DefaultClaudeActivityRepository(
    private val dataSource: ClaudeActivityDataSource,
) : ClaudeActivityRepository {
    override fun observeActivities(sessionIds: Set<String>): Flow<Map<String, ClaudeActivity>> = dataSource.observeActivities(sessionIds)
}
