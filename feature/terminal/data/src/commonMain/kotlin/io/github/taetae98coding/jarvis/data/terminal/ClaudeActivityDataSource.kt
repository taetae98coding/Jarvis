package io.github.taetae98coding.jarvis.data.terminal

import io.github.taetae98coding.jarvis.domain.terminal.ClaudeActivity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

internal interface ClaudeActivityDataSource {
    fun observeActivities(sessionIds: Set<String>): Flow<Map<String, ClaudeActivity>>
}

/** Claude 를 띄울 수 없는 타깃. 어떤 세션도 찾지 못해 패널 줄에 표시가 없다. */
internal object UnsupportedClaudeActivityDataSource : ClaudeActivityDataSource {
    override fun observeActivities(sessionIds: Set<String>): Flow<Map<String, ClaudeActivity>> = flowOf(emptyMap())
}

/**
 * Claude Code 가 세션마다 쓰는 기록을 읽는다. JVM 만 실제로 읽고 Android·iOS·Web 은 [UnsupportedClaudeActivityDataSource] 다.
 * 판정 근거는 docs/common/terminal-claude-status.html#platforms 에 있다.
 */
internal expect fun createClaudeActivityDataSource(): ClaudeActivityDataSource
