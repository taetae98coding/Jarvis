package org.cef.browser

import org.cef.CefClient
import org.cef.callback.CefDragData
import org.cef.handler.CefRenderHandler
import org.cef.handler.CefScreenInfo
import java.awt.Canvas
import java.awt.Component
import java.awt.Point
import java.awt.Rectangle
import java.awt.image.BufferedImage
import java.nio.ByteBuffer
import java.util.concurrent.CompletableFuture
import java.util.function.Consumer

/**
 * 화면 밖에서 그리는 브라우저. 그린 픽셀을 [OsrSink] 로 넘기기만 하고 어디에도 붙지 않는다.
 *
 * jcefmaven 의 `CefBrowserOsr` 는 JOGL `GLCanvas` 에 그려서 캔버스가 화면에 없으면 `onPaint` 를 버린다(GL 문맥이
 * 없으면 곧바로 return). `CefBrowser_N` 이 package-private 이라 같은 패키지에 두고 잇는다
 * (docs/platform/jvm.html#terminal-browser). jcef-api jar 는 봉인(Sealed)되지 않았다.
 */
internal class JarvisOsrBrowser(
    client: CefClient,
    url: String,
    private val sink: OsrSink,
) : CefBrowser_N(client, url, null, null, null, null), CefRenderHandler {
    // CefClient 가 포커스를 넘길 때 부모를 찾으려고 부른다. 어디에도 붙지 않으므로 부모가 없다.
    private val component = Canvas()

    @Volatile
    private var view = Rectangle(0, 0, DefaultWidth, DefaultHeight)

    @Volatile
    private var scale = DefaultScale

    @Volatile
    private var created = false

    /** 창 핸들 없이 만든다. `CefBrowserOsr.createImmediately` 가 쓰는 경로다. */
    override fun createImmediately() {
        // getURL() 도 같은 속성 이름(url)이 돼서 생성자에 준 주소(getUrl())를 직접 부른다.
        createBrowser(getClient(), 0, getUrl(), true, false, null, getRequestContext())
        created = true
    }

    /** [width]·[height] 는 CSS 픽셀, [scale] 은 창의 밀도다. 바뀌었을 때만 엔진에 알린다. */
    fun resize(width: Int, height: Int, scale: Double) {
        val next = Rectangle(0, 0, width.coerceAtLeast(1), height.coerceAtLeast(1))
        if (next == view && scale == this.scale) return

        view = next
        this.scale = scale
        if (created) wasResized(next.width, next.height)
    }

    val viewSize: Rectangle get() = view

    /** 그린 것이 아직 없을 때 한 장을 다시 그리게 한다. */
    fun repaint() {
        if (created) invalidate()
    }

    override fun getUIComponent(): Component = component

    override fun getRenderHandler(): CefRenderHandler = this

    // DevTools 창은 열지 않는다. CDP 는 getDevToolsClient() 로 보낸다.
    override fun createDevToolsBrowser(
        client: CefClient?,
        url: String?,
        context: CefRequestContext?,
        parent: CefBrowser_N?,
        inspectAt: Point?,
    ): CefBrowser_N? = null

    override fun createScreenshot(nativeResolution: Boolean): CompletableFuture<BufferedImage> =
        CompletableFuture.failedFuture(UnsupportedOperationException("캡처는 BrowserPage 의 최신 프레임으로 한다"))

    override fun getViewRect(browser: CefBrowser?): Rectangle = view

    override fun getScreenInfo(browser: CefBrowser?, screenInfo: CefScreenInfo): Boolean {
        screenInfo.Set(scale, 32, 8, false, view, view)
        return true
    }

    override fun getScreenPoint(browser: CefBrowser?, viewPoint: Point): Point = Point(viewPoint)

    override fun onPopupShow(browser: CefBrowser?, show: Boolean) = sink.onPopupShow(show)

    override fun onPopupSize(browser: CefBrowser?, size: Rectangle) = sink.onPopupSize(size)

    override fun onPaint(browser: CefBrowser?, popup: Boolean, dirtyRects: Array<out Rectangle>?, buffer: ByteBuffer, width: Int, height: Int) =
        sink.onPaint(popup, buffer, width, height)

    // 번호는 java.awt.Cursor 의 종류다. JCEF 가 CEF 커서를 AWT 종류로 바꿔 넘긴다.
    override fun onCursorChange(browser: CefBrowser?, cursorType: Int): Boolean {
        sink.onCursor(cursorType)
        return true
    }

    override fun startDragging(browser: CefBrowser?, dragData: CefDragData?, mask: Int, x: Int, y: Int): Boolean = false

    override fun updateDragCursor(browser: CefBrowser?, operation: Int) = Unit

    override fun addOnPaintListener(listener: Consumer<CefPaintEvent>?) = Unit

    override fun setOnPaintListener(listener: Consumer<CefPaintEvent>?) = Unit

    override fun removeOnPaintListener(listener: Consumer<CefPaintEvent>?) = Unit

    companion object {
        // 탭이 한 번도 보이지 않았을 때(Claude 가 가려진 탭을 연 경우)의 뷰포트.
        const val DefaultWidth = 1280
        const val DefaultHeight = 800
        const val DefaultScale = 2.0
    }
}

/** [JarvisOsrBrowser] 가 그린 것과 커서를 받는 쪽. CEF 의 UI 스레드에서 불리므로 오래 붙잡지 않는다. */
internal interface OsrSink {
    fun onPaint(popup: Boolean, buffer: ByteBuffer, width: Int, height: Int)

    fun onPopupShow(show: Boolean)

    fun onPopupSize(rect: Rectangle)

    fun onCursor(type: Int)
}
