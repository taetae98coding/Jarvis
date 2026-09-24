package io.github.taetae98coding.jarvis.browser

import io.github.taetae98coding.jarvis.automation.AutomationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.future.await
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import org.cef.CefClient
import org.cef.browser.CefFrame
import org.cef.browser.JarvisOsrBrowser
import org.cef.browser.OsrSink
import java.awt.Rectangle
import java.nio.ByteBuffer
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * 탭 하나의 페이지. 탭이 보이든 말든 앱 수명 동안 살아 있고(docs/common/terminal-browser.html R6), 화면과 Claude 가
 * 같은 페이지를 본다. 주소·제목·뒤로 가기 여부·그린 픽셀은 엔진 콜백이 채운다.
 */
class BrowserPage internal constructor(
    val tabId: Long,
    initialUrl: String,
    private val scope: CoroutineScope,
) : OsrSink {
    private val _url = MutableStateFlow(initialUrl)
    val url: StateFlow<String> = _url.asStateFlow()

    private val _title = MutableStateFlow<String?>(null)
    val title: StateFlow<String?> = _title.asStateFlow()

    private val _canGoBack = MutableStateFlow(false)
    val canGoBack: StateFlow<Boolean> = _canGoBack.asStateFlow()

    private val _canGoForward = MutableStateFlow(false)
    val canGoForward: StateFlow<Boolean> = _canGoForward.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _frame = MutableStateFlow<BrowserFrame?>(null)

    /** 가장 최근에 그린 한 장. 픽셀 버퍼는 돌려 쓰므로 받은 자리에서 복사한다([BrowserFrame]). */
    val frame: StateFlow<BrowserFrame?> = _frame.asStateFlow()

    private val _popup = MutableStateFlow<BrowserPopup?>(null)

    /** `<select>` 처럼 따로 그리는 펼침 목록. 없으면 null. */
    val popup: StateFlow<BrowserPopup?> = _popup.asStateFlow()

    private val _cursor = MutableStateFlow(0)

    /** java.awt.Cursor 의 종류 번호. */
    val cursor: StateFlow<Int> = _cursor.asStateFlow()

    private val _failed = MutableStateFlow(false)

    /** 엔진을 띄우지 못했다. 화면은 "[웹 페이지를 열 수 없습니다]" 를 보인다. */
    val failed: StateFlow<Boolean> = _failed.asStateFlow()

    private val ready = CompletableDeferred<JarvisOsrBrowser>()
    private val inputs = Channel<Input>(Channel.UNLIMITED)
    private val pool = arrayOfNulls<ByteArray>(FramePoolSize)
    private var poolIndex = 0
    private var popupRect: Rectangle? = null
    private var sequence = 0L

    @Volatile
    private var browser: JarvisOsrBrowser? = null

    init {
        scope.launch { dispatchInputs() }
    }

    fun load(url: String) {
        _url.value = url
        browser?.loadURL(url)
    }

    fun back() {
        browser?.goBack()
    }

    fun forward() {
        browser?.goForward()
    }

    fun reload() {
        browser?.reload()
    }

    /** 화면이 잰 크기. [width]·[height] 는 CSS 픽셀(dp), [scale] 은 창의 밀도다. */
    fun resize(width: Int, height: Int, scale: Double) {
        browser?.resize(width, height, scale)
    }

    /** 지금 뷰포트의 CSS 크기. */
    val viewport: Pair<Int, Int>
        get() = (browser?.viewSize ?: Rectangle(0, 0, JarvisOsrBrowser.DefaultWidth, JarvisOsrBrowser.DefaultHeight)).let { it.width to it.height }

    fun setFocus(focused: Boolean) {
        browser?.setFocus(focused)
    }

    /** macOS 편집 명령. 화면 밖 렌더링에서는 ⌘C 같은 키를 보내도 불리지 않는다. */
    fun edit(command: EditCommand) {
        val frame: CefFrame = browser?.focusedFrame ?: browser?.mainFrame ?: return
        when (command) {
            EditCommand.Copy -> frame.copy()
            EditCommand.Cut -> frame.cut()
            EditCommand.Paste -> frame.paste()
            EditCommand.SelectAll -> frame.selectAll()
            EditCommand.Undo -> frame.undo()
            EditCommand.Redo -> frame.redo()
        }
    }

    /**
     * 입력을 보낸 순서대로 넣는다. 쌓인 `mouseMoved` 는 마지막 하나만 보낸다 — 누름·뗌·키는 버리지 않는다
     * (기기 화면의 제스처 큐와 같은 규칙, docs/common/device-mirroring.html#behavior).
     */
    fun send(method: String, params: JsonObject) {
        inputs.trySend(Input(method, params))
    }

    /** CDP 메서드를 불러 결과를 준다. 오류 응답은 [AutomationException] 이다. */
    suspend fun cdp(method: String, params: JsonObject = JsonObject(emptyMap()), timeout: Duration = CdpTimeout): JsonObject {
        val browser = awaitBrowser()
        val response = withTimeout(timeout) {
            runCatching { browser.devToolsClient.executeDevToolsMethod(method, params.toString()).await() }
                .getOrElse { throw AutomationException("$method 실패: ${it.message}") }
        }

        return runCatching { Json.parseToJsonElement(response).jsonObject }.getOrDefault(JsonObject(emptyMap()))
    }

    /** 페이지가 한 장이라도 그릴 때까지 기다린다. 그린 적이 없으면 다시 그리게 한다. */
    suspend fun awaitFrame(timeout: Duration = FrameTimeout): BrowserFrame {
        awaitBrowser().repaint()
        return withTimeout(timeout) { frame.first { it != null }!! }
    }

    /** 엔진이 이 페이지의 브라우저를 다 만들 때까지 기다린다. */
    suspend fun awaitReady() {
        awaitBrowser()
    }

    internal suspend fun awaitBrowser(timeout: Duration = StartTimeout): JarvisOsrBrowser =
        runCatching { withTimeout(timeout) { ready.await() } }
            .getOrElse { throw AutomationException("브라우저 엔진이 준비되지 않았습니다: ${it.message}") }

    internal fun attach(client: CefClient) {
        val created = JarvisOsrBrowser(client, _url.value, this)
        browser = created
        created.createImmediately()
    }

    internal fun fail(error: Throwable) {
        _failed.value = true
        ready.completeExceptionally(error)
    }

    internal fun isFor(other: org.cef.browser.CefBrowser?): Boolean = other != null && other === browser

    internal fun onCreated() {
        val created = browser ?: return
        ready.complete(created)
        // 가려진 탭에서도 포커스·캐럿이 살아 있어야 Claude 가 입력칸에 쓸 수 있다.
        scope.launch { runCatching { cdp("Emulation.setFocusEmulationEnabled", JsonObject(mapOf("enabled" to JsonPrimitive(true)))) } }
        created.setFocus(true)
    }

    internal fun onAddress(url: String) {
        _url.value = url
    }

    internal fun onTitle(title: String?) {
        _title.value = title?.takeIf { it.isNotBlank() }
    }

    internal fun onLoadingState(loading: Boolean, canGoBack: Boolean, canGoForward: Boolean) {
        _isLoading.value = loading
        _canGoBack.value = canGoBack
        _canGoForward.value = canGoForward
    }

    internal fun close() {
        inputs.close()
        browser?.let { created ->
            created.setCloseAllowed()
            created.close(true)
        }
        browser = null
    }

    override fun onPaint(popup: Boolean, buffer: ByteBuffer, width: Int, height: Int) {
        val size = width * height * 4
        val pixels = obtain(size)
        buffer.rewind()
        buffer.get(pixels, 0, size)
        val painted = BrowserFrame(width, height, pixels, ++sequence)

        if (popup) {
            _popup.value = popupRect?.let { BrowserPopup(it, painted) }
        } else {
            _frame.value = painted
        }
    }

    override fun onPopupShow(show: Boolean) {
        if (!show) {
            popupRect = null
            _popup.value = null
        }
    }

    override fun onPopupSize(rect: Rectangle) {
        popupRect = Rectangle(rect)
    }

    override fun onCursor(type: Int) {
        _cursor.value = type
    }

    // 장마다 새로 할당하면 60fps × 수 MB 가 GC 를 기다린다. 기기 화면의 디코더와 같은 링을 돌려 쓴다.
    private fun obtain(size: Int): ByteArray {
        val existing = pool[poolIndex]
        val buffer = if (existing != null && existing.size == size) existing else ByteArray(size).also { pool[poolIndex] = it }
        poolIndex = (poolIndex + 1) % FramePoolSize
        return buffer
    }

    private suspend fun dispatchInputs() {
        var pending: Input? = null
        while (true) {
            val next = pending ?: inputs.receiveCatching().getOrNull() ?: return
            pending = null

            var input = next
            if (input.isMove) {
                while (true) {
                    val following = inputs.tryReceive().getOrNull() ?: break
                    if (following.isMove) {
                        input = following
                    } else {
                        pending = following
                        break
                    }
                }
            }

            runCatching { cdp(input.method, input.params) }
        }
    }

    private class Input(val method: String, val params: JsonObject) {
        val isMove: Boolean
            get() = method == "Input.dispatchMouseEvent" && (params["type"] as? JsonPrimitive)?.content == "mouseMoved"
    }

    private companion object {
        const val FramePoolSize = 3
        val CdpTimeout = 15.seconds
        val FrameTimeout = 5.seconds
        val StartTimeout = 30.seconds
    }
}

/** BGRA 8888, 행 간격 width × 4. [pixels] 는 몇 장 뒤에 덮어써진다 — 받은 자리에서 복사한다. */
class BrowserFrame(
    val width: Int,
    val height: Int,
    val pixels: ByteArray,
    val sequence: Long,
)

/** [rect] 는 뷰의 CSS 좌표, [frame] 은 그 사각형을 그린 픽셀이다. */
class BrowserPopup(
    val rect: Rectangle,
    val frame: BrowserFrame,
)

enum class EditCommand { Copy, Cut, Paste, SelectAll, Undo, Redo }
