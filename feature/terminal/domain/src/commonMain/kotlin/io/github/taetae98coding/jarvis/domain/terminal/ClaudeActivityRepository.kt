package io.github.taetae98coding.jarvis.domain.terminal

import kotlinx.coroutines.flow.Flow

interface ClaudeActivityRepository {
    /**
     * 창 sessionId([TerminalTab.claudeSessionId])마다 그 백그라운드 세션의 활동. 세션을 찾지 못했거나 Claude 를
     * 띄울 수 없는 플랫폼이면 맵에 없다. 수집하는 동안만 다시 읽는다(cold).
     */
    fun observeActivities(sessionIds: Set<String>): Flow<Map<String, ClaudeActivity>>
}
