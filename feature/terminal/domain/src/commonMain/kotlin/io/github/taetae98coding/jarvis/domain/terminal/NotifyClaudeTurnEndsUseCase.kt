package io.github.taetae98coding.jarvis.domain.terminal

import kotlinx.coroutines.flow.first

/**
 * Claude 탭 세션의 상태를 모으며 턴이 끝날 때마다 알린다. 취소될 때까지 돌아온다.
 *
 * [isWatching] 은 그 세션의 탭을 사용자가 지금 보고 있는지다. 창 포커스와 떠 있는 화면은 UI 만 알아서 함수로
 * 받고, 턴이 끝난 순간에 한 번만 묻는다.
 */
class NotifyClaudeTurnEndsUseCase(
    private val terminalRepository: TerminalRepository,
    private val workspaceRepository: TerminalWorkspaceRepository,
) {
    suspend operator fun invoke(isWatching: (sessionId: String) -> Boolean) {
        var previous: Map<String, ClaudeStatus>? = null

        terminalRepository.observeClaudeStatuses().collect { current ->
            val ends = previous?.let { claudeTurnEnds(it, current) }.orEmpty().filterNot { isWatching(it.sessionId) }
            previous = current
            if (ends.isEmpty()) return@collect

            val workspace = workspaceRepository.observeWorkspace().first()
            ends.forEach { end ->
                val panel = workspace.panels.firstOrNull { panel -> panel.tabs.any { it.claudeSessionId == end.sessionId } }
                    ?: return@forEach
                val tab = panel.tabs.first { it.claudeSessionId == end.sessionId }

                terminalRepository.showNotification(claudeNotification(end, panel, tab))
            }
        }
    }
}
