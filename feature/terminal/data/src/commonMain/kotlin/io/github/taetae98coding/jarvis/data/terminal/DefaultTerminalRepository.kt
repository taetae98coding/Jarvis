package io.github.taetae98coding.jarvis.data.terminal

import io.github.taetae98coding.jarvis.domain.terminal.TerminalTab
import io.github.taetae98coding.jarvis.domain.terminal.TerminalRepository
import io.github.taetae98coding.jarvis.domain.terminal.TerminalSession
import io.github.taetae98coding.jarvis.domain.terminal.TerminalSize

internal class DefaultTerminalRepository(
    private val dataSource: TerminalDataSource,
) : TerminalRepository {
    override val isSupported: Boolean get() = dataSource.isSupported

    override val isClaudeSupported: Boolean get() = dataSource.isClaudeSupported

    override val isBrowserSupported: Boolean get() = dataSource.isBrowserSupported

    override suspend fun open(size: TerminalSize, tab: TerminalTab): TerminalSession? = dataSource.open(size, tab)

    override suspend fun stopClaude(sessionId: String) = dataSource.stopClaude(sessionId)
}
