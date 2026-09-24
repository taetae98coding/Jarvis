package io.github.taetae98coding.jarvis.ui.terminal

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.kdroidfilter.webview.web.WebView
import io.github.kdroidfilter.webview.web.WebViewNavigator
import io.github.kdroidfilter.webview.web.WebViewState
import io.github.kdroidfilter.webview.web.rememberWebViewNavigator
import io.github.kdroidfilter.webview.web.rememberWebViewState
import io.github.taetae98coding.jarvis.domain.terminal.BrowserCookie
import io.github.taetae98coding.jarvis.domain.terminal.TerminalTab
import kotlinx.coroutines.flow.StateFlow

// 웹뷰는 이 컴포지션에만 있다. 탭이 가려지면 버리고 다시 보이면 저장된 주소로 새로 연다(R6).
@Composable
internal actual fun rememberBrowserPage(tab: TerminalTab): BrowserPage {
    // rememberWebViewState 는 리컴포지션마다 넘긴 주소를 content 에 다시 넣고, content 가 바뀌면 웹뷰가 그 주소를
    // 다시 연다. 페이지를 옮길 때마다 저장되는 tab.url 을 넘기면 옮긴 페이지를 한 번 더 부른다. 처음 주소로 고정한다.
    val initialUrl = remember { tab.url ?: TerminalTab.DefaultBrowserUrl }
    val state = rememberWebViewState(initialUrl)
    val navigator = rememberWebViewNavigator()

    return remember(state, navigator) { SystemWebViewPage(state, navigator) }
}

@Composable
internal actual fun BrowserPageView(page: BrowserPage, hidden: Boolean, onFocus: () -> Unit, modifier: Modifier) {
    val webView = page as SystemWebViewPage
    // 페이지를 컴포지션에서 빼면 웹뷰가 닫히고 다시 열린다. 크기만 0 으로 줄여 살려 둔 채 치운다.
    Box(modifier = modifier) {
        WebView(
            state = webView.state,
            navigator = webView.navigator,
            modifier = if (hidden) Modifier.size(0.dp) else Modifier.fillMaxSize(),
        )
    }
}

// 웹뷰가 탭보다 오래 살지 않는다. 가려진 탭은 ViewModel 이 마지막으로 본 제목을 쓴다.
internal actual fun browserTitle(tabId: Long): StateFlow<String?>? = null

internal actual fun closeBrowserPages(tabIds: Collection<Long>) = Unit

@Stable
private class SystemWebViewPage(
    val state: WebViewState,
    val navigator: WebViewNavigator,
) : BrowserPage {
    override val url: String? get() = state.lastLoadedUrl
    override val title: String? get() = state.pageTitle
    override val canGoBack: Boolean get() = navigator.canGoBack
    override val canGoForward: Boolean get() = navigator.canGoForward

    // AndroidView 안의 네이티브 뷰라 Compose 가 그린 메뉴가 그 아래에 가려진다(R9).
    override val coversOverlays: Boolean = true
    override val failed: Boolean = false

    override fun load(url: String) = navigator.loadUrl(url)

    override fun back() = navigator.navigateBack()

    override fun forward() = navigator.navigateForward()

    override fun reload() = navigator.reload()

    // Chrome 쿠키 가져오기는 macOS 데스크톱에만 있다(쿠키 가져오기 R7). 이 타깃에서는 버튼이 없다.
    override suspend fun importCookies(cookies: List<BrowserCookie>): Boolean = false
}
