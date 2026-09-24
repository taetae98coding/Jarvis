package io.github.taetae98coding.jarvis.data.terminal

import io.github.taetae98coding.jarvis.data.PlatformContext
import io.github.taetae98coding.jarvis.domain.terminal.BrowserCookie
import io.github.taetae98coding.jarvis.domain.terminal.ChromeProfile
import io.github.taetae98coding.jarvis.domain.terminal.TerminalTab
import io.github.taetae98coding.jarvis.domain.terminal.TerminalSession
import io.github.taetae98coding.jarvis.domain.terminal.TerminalSize
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

internal interface TerminalDataSource {
    val isSupported: Boolean

    val isClaudeSupported: Boolean

    val isBrowserSupported: Boolean

    val isChromeImportSupported: Boolean

    suspend fun open(size: TerminalSize, tab: TerminalTab): TerminalSession?

    suspend fun stopClaude(sessionId: String)

    fun observeChromeProfiles(): Flow<List<ChromeProfile>>

    suspend fun importChromeCookies(profileDirectory: String): List<BrowserCookie>
}

internal object UnsupportedTerminalDataSource : TerminalDataSource {
    override val isSupported: Boolean = false

    override val isClaudeSupported: Boolean = false

    // 터미널 화면에 들어갈 수 없는 타깃이다. 브라우저 탭을 열 자리가 없다.
    override val isBrowserSupported: Boolean = false

    // 브라우저 탭이 없으니 가져올 곳도 없고, PC 의 Chrome 에 접근할 수도 없다.
    override val isChromeImportSupported: Boolean = false

    override suspend fun open(size: TerminalSize, tab: TerminalTab): TerminalSession? = null

    override suspend fun stopClaude(sessionId: String) = Unit

    override fun observeChromeProfiles(): Flow<List<ChromeProfile>> = flowOf(emptyList())

    override suspend fun importChromeCookies(profileDirectory: String): List<BrowserCookie> = emptyList()
}

/**
 * 셸 프로세스를 띄운다. JVM 은 pty, Android 는 파이프이고 iOS·Web 은 [UnsupportedTerminalDataSource] 다.
 * 판정 근거는 docs/common/terminal.html#platforms 에 있다.
 */
internal expect fun createTerminalDataSource(context: PlatformContext): TerminalDataSource
