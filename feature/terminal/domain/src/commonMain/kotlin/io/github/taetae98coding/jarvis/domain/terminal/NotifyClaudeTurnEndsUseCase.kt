package io.github.taetae98coding.jarvis.domain.terminal

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map

/**
 * 작업 공간의 Claude 탭 세션을 모으며 턴이 끝날 때마다 알린다. 취소될 때까지 돌아온다.
 *
 * [isWatching] 은 그 세션의 탭을 사용자가 지금 보고 있는지다. 창 포커스와 떠 있는 화면은 UI 만 알아서 함수로
 * 받고, 턴이 끝난 순간에 한 번만 묻는다.
 */
class NotifyClaudeTurnEndsUseCase(
    private val activityRepository: ClaudeActivityRepository,
    private val workspaceRepository: TerminalWorkspaceRepository,
    private val terminalRepository: TerminalRepository,
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    suspend operator fun invoke(isWatching: (sessionId: String) -> Boolean) {
        if (!terminalRepository.isClaudeSupported) return

        // 탭이 늘고 줄어 조회를 다시 묶어도 앞 상태는 이어 간다. 그 사이에 끝난 턴을 놓치지 않는다.
        var previous: Map<String, ClaudeActivity>? = null

        workspaceRepository.observeWorkspace()
            .map { it.claudeSessionIds }
            .distinctUntilChanged()
            .flatMapLatest { activityRepository.observeActivities(it) }
            .collect { current ->
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
