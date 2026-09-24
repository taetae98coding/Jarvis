package io.github.taetae98coding.jarvis.ui.terminal

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import io.github.taetae98coding.jarvis.domain.terminal.BrowserCookie
import io.github.taetae98coding.jarvis.domain.terminal.TerminalTab
import kotlinx.coroutines.flow.StateFlow

/**
 * 브라우저 탭의 페이지 엔진(docs/common/terminal-browser.html#implementation). 주소 줄은 공통이고 엔진만 타깃마다 다르다.
 * JVM 은 앱 수명 동안 살아 있는 Chromium 페이지, Android 는 컴포지션에만 있는 시스템 웹뷰다.
 */
@Stable
internal interface BrowserPage {
    val url: String?
    val title: String?
    val canGoBack: Boolean
    val canGoForward: Boolean

    /** 페이지가 Compose 위에 뜨는 네이티브 뷰라서 메뉴·Snackbar 를 가린다(R9). 그동안 페이지를 치워야 한다. */
    val coversOverlays: Boolean

    /** 엔진을 띄우지 못했다. "[웹 페이지를 열 수 없습니다]" 를 보인다. */
    val failed: Boolean

    fun load(url: String)

    fun back()

    fun forward()

    fun reload()

    /** 쿠키를 넣고 되읽어 실제로 들어갔는지 준다(docs/common/chrome-cookie-import.html R8a). */
    suspend fun importCookies(cookies: List<BrowserCookie>): Boolean
}

@Composable
internal expect fun rememberBrowserPage(tab: TerminalTab): BrowserPage

/** [hidden] 이면 페이지를 치운다([BrowserPage.coversOverlays] 인 엔진만 뜻이 있다). */
@Composable
internal expect fun BrowserPageView(page: BrowserPage, hidden: Boolean, onFocus: () -> Unit, modifier: Modifier)

/** 엔진이 탭마다 따로 알고 있는 지금 제목. 가려진 탭의 이름에 쓴다. 모르면 null. */
internal expect fun browserTitle(tabId: Long): StateFlow<String?>?

/** 작업 공간에서 사라진 탭의 페이지를 닫는다. 페이지가 탭보다 오래 사는 엔진(JVM)만 할 일이 있다. */
internal expect fun closeBrowserPages(tabIds: Collection<Long>)
