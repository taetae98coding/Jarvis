package io.github.taetae98coding.jarvis.domain.terminal

class IsTerminalSupportedUseCase(
    private val repository: TerminalRepository,
) {
    operator fun invoke(): Boolean = repository.isSupported
}
