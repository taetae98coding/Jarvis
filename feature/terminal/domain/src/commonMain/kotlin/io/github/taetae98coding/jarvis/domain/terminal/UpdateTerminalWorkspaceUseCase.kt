package io.github.taetae98coding.jarvis.domain.terminal

/**
 * 작업 공간을 바꾸고, 그 변경으로 사라진 Claude 탭의 백그라운드 세션을 멈춘다. 탭·그룹·패널 어느 쪽을
 * 닫든 이 한 곳을 지나므로 멈추는 규칙이 화면마다 흩어지지 않는다.
 */
class UpdateTerminalWorkspaceUseCase(
    private val workspaceRepository: TerminalWorkspaceRepository,
    private val terminalRepository: TerminalRepository,
) {
    suspend operator fun invoke(transform: (TerminalWorkspace) -> TerminalWorkspace): TerminalWorkspaceChange {
        val change = workspaceRepository.updateWorkspace(transform)

        change.removedTabs
            .mapNotNull { it.claudeSessionId }
            .forEach { terminalRepository.stopClaude(it) }

        return change
    }
}
