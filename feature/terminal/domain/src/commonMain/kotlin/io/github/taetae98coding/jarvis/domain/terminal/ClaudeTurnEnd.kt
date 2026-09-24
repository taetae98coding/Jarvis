package io.github.taetae98coding.jarvis.domain.terminal

/** Claude 가 한 턴을 끝냈다. [sessionId] 는 탭의 [TerminalTab.claudeSessionId] 다. */
data class ClaudeTurnEnd(
    val sessionId: String,
    val result: ClaudeActivity.Finished,
)

/**
 * 바로 앞에 [ClaudeActivity.Working]·[ClaudeActivity.Monitoring] 이던 세션이 [ClaudeActivity.Finished] 가 된 것만
 * 턴의 끝이다. 처음 보는 세션은 앞 상태가 없어 알리지 않는다 — 앱을 켤 때 이미 끝나 있던 세션이 알림을 띄우지 않게
 * (docs/common/claude-notification.html#implementation). 모니터링으로 넘어가는 것은 Claude 가 스스로 다시 깨어나므로 끝이 아니다.
 */
fun claudeTurnEnds(previous: Map<String, ClaudeActivity>, current: Map<String, ClaudeActivity>): List<ClaudeTurnEnd> =
    current.mapNotNull { (sessionId, activity) ->
        val before = previous[sessionId]
        val wasRunning = before == ClaudeActivity.Working || before == ClaudeActivity.Monitoring
        if (activity is ClaudeActivity.Finished && wasRunning) ClaudeTurnEnd(sessionId, activity) else null
    }
