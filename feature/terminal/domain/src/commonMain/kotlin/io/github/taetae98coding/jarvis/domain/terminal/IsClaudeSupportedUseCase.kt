package io.github.taetae98coding.jarvis.domain.terminal

class IsClaudeSupportedUseCase(
    private val repository: TerminalRepository,
) {
    operator fun invoke(): Boolean = repository.isSupported && repository.isClaudeSupported
}
