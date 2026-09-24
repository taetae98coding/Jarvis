package io.github.taetae98coding.jarvis.data.terminal

import io.github.taetae98coding.jarvis.data.PlatformContext
import io.github.taetae98coding.jarvis.domain.terminal.TerminalTab
import io.github.taetae98coding.jarvis.domain.terminal.TerminalSession
import io.github.taetae98coding.jarvis.domain.terminal.TerminalSize

internal interface TerminalDataSource {
    val isSupported: Boolean

    val isClaudeSupported: Boolean

    val isBrowserSupported: Boolean

    suspend fun open(size: TerminalSize, tab: TerminalTab): TerminalSession?

    suspend fun stopClaude(sessionId: String)
}

internal object UnsupportedTerminalDataSource : TerminalDataSource {
    override val isSupported: Boolean = false

    override val isClaudeSupported: Boolean = false

    // 터미널 화면에 들어갈 수 없는 타깃이다. 브라우저 탭을 열 자리가 없다.
    override val isBrowserSupported: Boolean = false

    override suspend fun open(size: TerminalSize, tab: TerminalTab): TerminalSession? = null

    override suspend fun stopClaude(sessionId: String) = Unit
}

/**
 * 셸 프로세스를 띄운다. JVM 은 pty, Android 는 파이프이고 iOS·Web 은 [UnsupportedTerminalDataSource] 다.
 * 판정 근거는 docs/common/terminal.html#platforms 에 있다.
 */
internal expect fun createTerminalDataSource(context: PlatformContext): TerminalDataSource
