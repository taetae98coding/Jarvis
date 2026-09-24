package io.github.taetae98coding.jarvis.domain.terminal

import kotlinx.coroutines.flow.first

/**
 * 워크트리 패널을 닫는다. [removeWorktree] 면 먼저 그 폴더의 워크트리와 브랜치를 지우고([deleteDirectory] 면 폴더도),
 * git 이 실패하면 패널은 그대로다. 닫기 규칙은 [TerminalWorkspace.closePanel] 과 같다.
 */
class CloseWorktreePanelUseCase(
    private val workspaceRepository: TerminalWorkspaceRepository,
    private val gitRepository: GitWorktreeRepository,
    private val updateWorkspace: UpdateTerminalWorkspaceUseCase,
) {
    suspend operator fun invoke(panelId: Long, removeWorktree: Boolean, deleteDirectory: Boolean): Result<TerminalWorkspaceChange> {
        val panel = workspaceRepository.observeWorkspace().first().panels.firstOrNull { it.id == panelId }
            ?: return failure("패널이 없습니다")

        if (removeWorktree) {
            if (panel.parentId == null) return failure("워크트리 패널이 아닙니다")
            val directory = panel.directory ?: return failure("패널에 폴더가 없습니다")
            gitRepository.removeWorktree(directory, deleteDirectory).onFailure { return Result.failure(it) }
        }

        return Result.success(updateWorkspace { it.closePanel(panelId) })
    }

    private fun failure(message: String): Result<TerminalWorkspaceChange> = Result.failure(GitWorktreeException(message))
}
