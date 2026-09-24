package io.github.taetae98coding.jarvis.data.terminal

import io.github.taetae98coding.jarvis.domain.terminal.BrowserCookie
import io.github.taetae98coding.jarvis.domain.terminal.ChromeProfile
import io.github.taetae98coding.jarvis.domain.terminal.TerminalTab
import io.github.taetae98coding.jarvis.domain.terminal.TerminalRepository
import io.github.taetae98coding.jarvis.domain.terminal.TerminalSession
import io.github.taetae98coding.jarvis.domain.terminal.TerminalSize
import kotlinx.coroutines.flow.Flow

internal class DefaultTerminalRepository(
    private val dataSource: TerminalDataSource,
) : TerminalRepository {
    override val isSupported: Boolean get() = dataSource.isSupported

    override val isClaudeSupported: Boolean get() = dataSource.isClaudeSupported

    override val isBrowserSupported: Boolean get() = dataSource.isBrowserSupported

    override val isChromeImportSupported: Boolean get() = dataSource.isChromeImportSupported

    override suspend fun open(size: TerminalSize, tab: TerminalTab): TerminalSession? = dataSource.open(size, tab)

    override suspend fun stopClaude(sessionId: String) = dataSource.stopClaude(sessionId)

    override fun observeChromeProfiles(): Flow<List<ChromeProfile>> = dataSource.observeChromeProfiles()

    override suspend fun importChromeCookies(profileDirectory: String): List<BrowserCookie> =
        dataSource.importChromeCookies(profileDirectory)
}
