package io.github.taetae98coding.jarvis.domain.terminal

class OpenTerminalSessionUseCase(
    private val repository: TerminalRepository,
) {
    suspend operator fun invoke(size: TerminalSize, pane: PaneNode.Leaf): TerminalSession? {
        if (!repository.isSupported) return null
        if (pane.program == TerminalProgram.Claude && (!repository.isClaudeSupported || pane.claudeSessionId == null)) return null

        return repository.open(size, pane)
    }
}
