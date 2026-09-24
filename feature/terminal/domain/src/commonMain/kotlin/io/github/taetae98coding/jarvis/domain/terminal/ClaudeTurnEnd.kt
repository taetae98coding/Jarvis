package io.github.taetae98coding.jarvis.domain.terminal

/** Claude 가 한 턴을 끝냈다. [sessionId] 는 탭의 [TerminalTab.claudeSessionId] 다. */
data class ClaudeTurnEnd(
    val sessionId: String,
    val activity: ClaudeActivity,
    val summary: String?,
)

/**
 * 바로 앞에 [ClaudeActivity.Working] 이던 세션이 [ClaudeActivity.Finished]·[ClaudeActivity.WaitingForInput] 이 된 것만
 * 턴의 끝이다. 멈춘 세션에 다시 붙거나 처음 읽을 때 알림이 뜨지 않게 하려고 앞 상태를 따진다
 * (docs/common/claude-notification.html#implementation).
 */
fun claudeTurnEnds(previous: Map<String, ClaudeStatus>, current: Map<String, ClaudeStatus>): List<ClaudeTurnEnd> =
    current.mapNotNull { (sessionId, status) ->
        val ended = status.activity == ClaudeActivity.Finished || status.activity == ClaudeActivity.WaitingForInput
        if (ended && previous[sessionId]?.activity == ClaudeActivity.Working) {
            ClaudeTurnEnd(sessionId, status.activity, status.summary)
        } else {
            null
        }
    }
