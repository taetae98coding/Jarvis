package io.github.taetae98coding.jarvis.domain.terminal

class ImportChromeCookiesUseCase(
    private val repository: TerminalRepository,
) {
    suspend operator fun invoke(profileDirectory: String): List<BrowserCookie> = repository.importChromeCookies(profileDirectory)
}
