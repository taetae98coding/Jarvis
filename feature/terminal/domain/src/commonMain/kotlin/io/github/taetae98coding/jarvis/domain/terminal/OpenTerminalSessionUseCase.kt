package io.github.taetae98coding.jarvis.domain.terminal

class OpenTerminalSessionUseCase(
    private val repository: TerminalRepository,
) {
    suspend operator fun invoke(size: TerminalSize, tab: TerminalTab): TerminalSession? {
        if (!repository.isSupported) return null
        if (tab.program == TerminalProgram.Claude && (!repository.isClaudeSupported || tab.claudeSessionId == null)) return null

        return repository.open(size, tab)
    }
}
