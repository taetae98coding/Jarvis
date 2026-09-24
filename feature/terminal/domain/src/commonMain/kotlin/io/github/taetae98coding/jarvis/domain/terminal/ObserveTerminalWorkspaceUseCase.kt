package io.github.taetae98coding.jarvis.domain.terminal

import kotlinx.coroutines.flow.Flow

class ObserveTerminalWorkspaceUseCase(
    private val repository: TerminalWorkspaceRepository,
) {
    operator fun invoke(): Flow<TerminalWorkspace> = repository.observeWorkspace()
}
