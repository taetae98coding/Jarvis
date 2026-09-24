package io.github.taetae98coding.jarvis.data.terminal

import io.github.taetae98coding.jarvis.domain.terminal.TerminalProgram
import io.github.taetae98coding.jarvis.domain.terminal.TerminalRepository
import io.github.taetae98coding.jarvis.domain.terminal.TerminalSession
import io.github.taetae98coding.jarvis.domain.terminal.TerminalSize

internal class DefaultTerminalRepository(
    private val dataSource: TerminalDataSource,
) : TerminalRepository {
    override val isSupported: Boolean get() = dataSource.isSupported

    override val isClaudeSupported: Boolean get() = dataSource.isClaudeSupported

    override suspend fun open(size: TerminalSize, program: TerminalProgram): TerminalSession? = dataSource.open(size, program)
}
