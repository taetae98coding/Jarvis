package io.github.taetae98coding.jarvis.domain.terminal

class OpenTerminalSessionUseCase(
    private val repository: TerminalRepository,
) {
    suspend operator fun invoke(size: TerminalSize, program: TerminalProgram = TerminalProgram.Shell): TerminalSession? {
        if (!repository.isSupported) return null
        if (program == TerminalProgram.Claude && !repository.isClaudeSupported) return null

        return repository.open(size, program)
    }
}
