package io.github.taetae98coding.jarvis.data.terminal

import io.github.taetae98coding.jarvis.data.PlatformContext
import io.github.taetae98coding.jarvis.domain.terminal.TerminalProgram
import io.github.taetae98coding.jarvis.domain.terminal.TerminalSession
import io.github.taetae98coding.jarvis.domain.terminal.TerminalSize

internal interface TerminalDataSource {
    val isSupported: Boolean

    val isClaudeSupported: Boolean

    suspend fun open(size: TerminalSize, program: TerminalProgram): TerminalSession?
}

internal object UnsupportedTerminalDataSource : TerminalDataSource {
    override val isSupported: Boolean = false

    override val isClaudeSupported: Boolean = false

    override suspend fun open(size: TerminalSize, program: TerminalProgram): TerminalSession? = null
}

/**
 * 셸 프로세스를 띄운다. JVM 은 pty, Android 는 파이프이고 iOS·Web 은 [UnsupportedTerminalDataSource] 다.
 * 판정 근거는 docs/common/terminal.html#platforms 에 있다.
 */
internal expect fun createTerminalDataSource(context: PlatformContext): TerminalDataSource
