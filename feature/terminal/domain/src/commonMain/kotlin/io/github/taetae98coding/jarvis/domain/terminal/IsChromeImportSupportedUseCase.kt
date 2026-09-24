package io.github.taetae98coding.jarvis.domain.terminal

class IsChromeImportSupportedUseCase(
    private val repository: TerminalRepository,
) {
    operator fun invoke(): Boolean = repository.isSupported && repository.isBrowserSupported && repository.isChromeImportSupported
}
