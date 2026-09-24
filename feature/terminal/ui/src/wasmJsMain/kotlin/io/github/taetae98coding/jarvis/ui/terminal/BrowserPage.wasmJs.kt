package io.github.taetae98coding.jarvis.ui.terminal

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.taetae98coding.jarvis.domain.terminal.BrowserCookie
import io.github.taetae98coding.jarvis.domain.terminal.TerminalTab
import kotlinx.coroutines.flow.StateFlow

// 이 타깃에는 터미널이 없다. 저장된 브라우저 탭은 "[웹 페이지를 열 수 없습니다]" 만 보인다(R8).
@Composable
internal actual fun rememberBrowserPage(tab: TerminalTab): BrowserPage = UnsupportedBrowserPage

@Composable
internal actual fun BrowserPageView(page: BrowserPage, hidden: Boolean, onFocus: () -> Unit, modifier: Modifier) = Unit

internal actual fun browserTitle(tabId: Long): StateFlow<String?>? = null

internal actual fun closeBrowserPages(tabIds: Collection<Long>) = Unit

private object UnsupportedBrowserPage : BrowserPage {
    override val url: String? = null
    override val title: String? = null
    override val canGoBack: Boolean = false
    override val canGoForward: Boolean = false
    override val coversOverlays: Boolean = false
    override val failed: Boolean = true

    override fun load(url: String) = Unit

    override fun back() = Unit

    override fun forward() = Unit

    override fun reload() = Unit

    override suspend fun importCookies(cookies: List<BrowserCookie>): Boolean = false
}
