package io.github.taetae98coding.jarvis.domain.terminal

import kotlinx.coroutines.flow.first

/**
 * [parentId] 패널의 폴더가 속한 저장소에 워크트리를 만들고, 성공하면 그 폴더를 가진 패널을 부모 아래에 붙인다.
 * git 이 실패하면 작업 공간은 그대로다.
 */
class AddWorktreePanelUseCase(
    private val workspaceRepository: TerminalWorkspaceRepository,
    private val gitRepository: GitWorktreeRepository,
    private val updateWorkspace: UpdateTerminalWorkspaceUseCase,
) {
    suspend operator fun invoke(
        parentId: Long,
        branch: String,
        path: String,
        program: TerminalProgram = TerminalProgram.Shell,
    ): Result<TerminalWorkspaceChange> {
        val parent = workspaceRepository.observeWorkspace().first().panels.firstOrNull { it.id == parentId }
            ?: return failure("패널이 없습니다")
        val repository = parent.directory ?: return failure("패널에 폴더가 없습니다")
        val name = branch.trim().ifEmpty { return failure("브랜치 이름이 비어 있습니다") }
        val directory = path.trim().ifEmpty { return failure("폴더가 비어 있습니다") }

        return gitRepository.addWorktree(repository, name, directory).map { worktree ->
            val sessionId = if (program == TerminalProgram.Claude) newClaudeSessionId() else null
            updateWorkspace { it.addWorktreePanel(parentId, name, worktree.path, program, sessionId) }
        }
    }

    private fun failure(message: String): Result<TerminalWorkspaceChange> = Result.failure(GitWorktreeException(message))
}
