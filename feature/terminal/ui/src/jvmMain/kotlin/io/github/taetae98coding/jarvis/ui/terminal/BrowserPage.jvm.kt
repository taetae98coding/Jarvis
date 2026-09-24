package io.github.taetae98coding.jarvis.ui.terminal

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import io.github.taetae98coding.jarvis.browser.BrowserCookieParam
import io.github.taetae98coding.jarvis.browser.BrowserEngine
import io.github.taetae98coding.jarvis.browser.BrowserSurface
import io.github.taetae98coding.jarvis.browser.importCookies
import io.github.taetae98coding.jarvis.domain.terminal.BrowserCookie
import io.github.taetae98coding.jarvis.domain.terminal.TerminalTab
import kotlinx.coroutines.flow.StateFlow
import io.github.taetae98coding.jarvis.browser.BrowserPage as EnginePage

// 페이지는 탭 id 로 엔진에 있다. 화면을 다시 그리거나 탭을 옮겨도 같은 페이지다(R6).
@Composable
internal actual fun rememberBrowserPage(tab: TerminalTab): BrowserPage {
    val engine = remember(tab.id) { BrowserEngine.page(tab.id, tab.url ?: TerminalTab.DefaultBrowserUrl) }
    val url by engine.url.collectAsState()
    val title by engine.title.collectAsState()
    val canGoBack by engine.canGoBack.collectAsState()
    val canGoForward by engine.canGoForward.collectAsState()
    val failed by engine.failed.collectAsState()

    return remember(engine) { JcefBrowserPage(engine) }.apply {
        this.url = url
        this.title = title
        this.canGoBack = canGoBack
        this.canGoForward = canGoForward
        this.failed = failed
    }
}

@Composable
internal actual fun BrowserPageView(page: BrowserPage, hidden: Boolean, onFocus: () -> Unit, modifier: Modifier) {
    BrowserSurface(page = (page as JcefBrowserPage).engine, onFocus = onFocus, modifier = modifier)
}

internal actual fun browserTitle(tabId: Long): StateFlow<String?>? = BrowserEngine.existing(tabId)?.title

internal actual fun closeBrowserPages(tabIds: Collection<Long>) {
    tabIds.forEach(BrowserEngine::close)
}

@Stable
private class JcefBrowserPage(val engine: EnginePage) : BrowserPage {
    // 공통 주소 줄이 snapshotFlow 로 읽으므로 Compose 상태여야 한다.
    override var url: String? by mutableStateOf(null)
    override var title: String? by mutableStateOf(null)
    override var canGoBack: Boolean by mutableStateOf(false)
    override var canGoForward: Boolean by mutableStateOf(false)
    override var failed: Boolean by mutableStateOf(false)

    // Compose 가 페이지를 그리므로 메뉴·Snackbar 가 페이지 위에 그대로 뜬다.
    override val coversOverlays: Boolean = false

    override fun load(url: String) = engine.load(url)

    override fun back() = engine.back()

    override fun forward() = engine.forward()

    override fun reload() = engine.reload()

    override suspend fun importCookies(cookies: List<BrowserCookie>): Boolean =
        engine.importCookies(
            cookies.map { cookie ->
                BrowserCookieParam(
                    name = cookie.name,
                    value = cookie.value,
                    domain = cookie.domain,
                    path = cookie.path,
                    isSecure = cookie.isSecure,
                    isHttpOnly = cookie.isHttpOnly,
                    sameSite = cookie.sameSite,
                    expiresEpochSeconds = if (cookie.isSessionOnly) null else cookie.expiresEpochSeconds,
                )
            },
        )
}
