package io.github.taetae98coding.jarvis.domain.terminal

class OpenTerminalSessionUseCase(
    private val repository: TerminalRepository,
) {
    suspend operator fun invoke(size: TerminalSize): TerminalSession? {
        if (!repository.isSupported) return null

        return repository.open(size)
    }
}
