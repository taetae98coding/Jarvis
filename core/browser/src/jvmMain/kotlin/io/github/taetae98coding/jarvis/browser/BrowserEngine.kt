package io.github.taetae98coding.jarvis.browser

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import me.friwi.jcefmaven.CefAppBuilder
import org.cef.CefApp
import org.cef.CefClient
import org.cef.browser.CefBrowser
import org.cef.browser.CefFrame
import org.cef.handler.CefDisplayHandlerAdapter
import org.cef.handler.CefLifeSpanHandlerAdapter
import org.cef.handler.CefLoadHandlerAdapter
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * 앱 안 브라우저의 Chromium 엔진 한 벌과 탭 id 별 페이지(docs/platform/jvm.html#terminal-browser). 엔진은 처음
 * 페이지가 필요할 때 띄우고, 페이지는 [retain]·[close] 가 닫을 때까지 산다 — 화면에 보이는지와 상관없다.
 */
object BrowserEngine {
    // 데스크톱은 macOS 만 지원한다. 다른 OS 에서는 엔진을 띄우지 않고 브라우저 항목을 뺀다.
    val isSupported: Boolean = System.getProperty("os.name").orEmpty().startsWith("Mac")

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val pages = ConcurrentHashMap<Long, BrowserPage>()
    private val root = File(System.getProperty("user.home"), "Library/Application Support/Jarvis/jcef")

    @Volatile
    private var app: CefApp? = null

    private val client: Deferred<CefClient> by lazy { scope.async { start() } }

    /** [tabId] 의 페이지. 없으면 [initialUrl] 로 만든다. 이미 있으면 주소는 무시한다. */
    fun page(tabId: Long, initialUrl: String): BrowserPage =
        pages.computeIfAbsent(tabId) {
            BrowserPage(tabId, initialUrl.ifBlank { BlankUrl }, scope).also { page ->
                scope.launch {
                    runCatching { client.await() }
                        .onSuccess(page::attach)
                        .onFailure(page::fail)
                }
            }
        }

    /** 이미 있는 페이지. 만들지 않는다. */
    fun existing(tabId: Long): BrowserPage? = pages[tabId]

    /** [tabIds] 에 없는 탭의 페이지를 닫는다. 작업 공간에서 사라진 탭이다. */
    fun retain(tabIds: Set<Long>) {
        pages.keys.filterNot { it in tabIds }.forEach(::close)
    }

    fun close(tabId: Long) {
        pages.remove(tabId)?.close()
    }

    /** 앱이 끝날 때. 엔진을 닫지 않고 끝내면 macOS 에서 도우미 프로세스가 남는다. */
    fun shutdown() {
        pages.keys.toList().forEach(::close)
        app?.dispose()
        app = null
    }

    private fun start(): CefClient {
        check(isSupported) { "macOS 가 아니다" }

        val builder = CefAppBuilder().apply {
            setInstallDir(File(root, "bundle"))
            // 설치 폴더에 이미 풀려 있으면 곧바로 끝난다. 진행 상황은 쓰지 않는다.
            setProgressHandler { _, _ -> }
            cefSettings.windowless_rendering_enabled = true
            // 쿠키·저장소가 앱을 다시 켜도 남게 한다(docs/common/chrome-cookie-import.html R4).
            cefSettings.root_cache_path = File(root, "cache").path
            cefSettings.cache_path = File(root, "cache/default").path
            cefSettings.persist_session_cookies = true
        }
        val started = builder.build()
        app = started

        return started.createClient().apply {
            addDisplayHandler(
                object : CefDisplayHandlerAdapter() {
                    override fun onAddressChange(browser: CefBrowser?, frame: CefFrame?, url: String?) {
                        if (frame?.isMain != false && url != null) pageOf(browser)?.onAddress(url)
                    }

                    override fun onTitleChange(browser: CefBrowser?, title: String?) {
                        pageOf(browser)?.onTitle(title)
                    }
                },
            )
            addLoadHandler(
                object : CefLoadHandlerAdapter() {
                    override fun onLoadingStateChange(browser: CefBrowser?, isLoading: Boolean, canGoBack: Boolean, canGoForward: Boolean) {
                        pageOf(browser)?.onLoadingState(isLoading, canGoBack, canGoForward)
                    }
                },
            )
            addLifeSpanHandler(
                object : CefLifeSpanHandlerAdapter() {
                    override fun onAfterCreated(browser: CefBrowser?) {
                        pageOf(browser)?.onCreated()
                    }

                    // 새 창(window.open, target="_blank")은 띄우지 않고 같은 탭에서 연다. 새 탭으로 여는 것은 스코프 밖이다.
                    override fun onBeforePopup(browser: CefBrowser?, frame: CefFrame?, targetUrl: String?, targetFrameName: String?): Boolean {
                        if (targetUrl != null) browser?.loadURL(targetUrl)
                        return true
                    }
                },
            )
        }
    }

    private fun pageOf(browser: CefBrowser?): BrowserPage? = pages.values.firstOrNull { it.isFor(browser) }

    private const val BlankUrl = "about:blank"
}
