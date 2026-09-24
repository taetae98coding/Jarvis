package io.github.taetae98coding.jarvis.domain.terminal

import kotlinx.coroutines.flow.Flow

class ObserveClaudeActivitiesUseCase(
    private val repository: ClaudeActivityRepository,
) {
    operator fun invoke(sessionIds: Set<String>): Flow<Map<String, ClaudeActivity>> = repository.observeActivities(sessionIds)
}
