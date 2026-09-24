package io.github.taetae98coding.jarvis.browser

import io.github.taetae98coding.jarvis.automation.AutomationException
import io.github.taetae98coding.jarvis.automation.AutomationImage
import io.github.taetae98coding.jarvis.automation.BrowserAutomation
import io.github.taetae98coding.jarvis.automation.BrowserPageInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.put
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * Claude 의 브라우저 도구(docs/common/mcp-server.html R9–R12). 입력은 CDP `Input.*` 라 페이지에는 사람의 입력과
 * 같은 신뢰된 이벤트로 들어간다. 좌표는 CSS 픽셀이다.
 */
internal class CefBrowserAutomation : BrowserAutomation {
    override suspend fun page(tabId: Long, initialUrl: String): BrowserPageInfo {
        val page = BrowserEngine.page(tabId, initialUrl)
        page.awaitBrowser()

        return BrowserPageInfo(page.url.value, page.title.value)
    }

    override suspend fun navigate(tabId: Long, url: String) {
        val page = page(tabId)
        page.load(url)
        awaitLoad(page)
    }

    override suspend fun back(tabId: Long) {
        val page = page(tabId)
        if (!page.canGoBack.value) throw AutomationException("뒤로 갈 곳이 없습니다.")
        page.back()
        awaitLoad(page)
    }

    // CDP 캡처 대신 엔진이 방금 그린 픽셀을 쓴다. 사용자가 보는 것과 같은 장이고, 화면 밖 렌더링이라 가려진 탭도 그린다.
    override suspend fun screenshot(tabId: Long): AutomationImage {
        val page = page(tabId)
        val frame = page.awaitFrame()
        val pixels = frame.pixels.copyOf()
        val (width, height) = page.viewport

        return withContext(Dispatchers.Default) { encodeJpeg(frame.width, frame.height, pixels, width, height) }
    }

    override suspend fun snapshot(tabId: Long): String = evaluateString(page(tabId), SnapshotScript)

    override suspend fun click(tabId: Long, x: Int, y: Int, clickCount: Int) {
        val page = page(tabId)
        mouse(page, "mouseMoved", x, y, button = "none", clickCount = 0)
        for (count in 1..clickCount.coerceIn(1, 3)) {
            mouse(page, "mousePressed", x, y, button = "left", clickCount = count)
            mouse(page, "mouseReleased", x, y, button = "left", clickCount = count)
        }
    }

    override suspend fun type(tabId: Long, text: String) {
        page(tabId).cdp("Input.insertText", buildJsonObject { put("text", text) })
    }

    override suspend fun pressKey(tabId: Long, key: String) {
        val spec = CdpKeys[key] ?: throw AutomationException("모르는 키입니다: $key. ${CdpKeys.keys.joinToString()} 중 하나를 쓰세요.")
        pressKey(page(tabId), spec, modifiers = 0)
    }

    override suspend fun scroll(tabId: Long, x: Int?, y: Int?, deltaY: Int) {
        val page = page(tabId)
        val (width, height) = page.viewport
        page.cdp(
            "Input.dispatchMouseEvent",
            buildJsonObject {
                put("type", "mouseWheel")
                put("x", x ?: (width / 2))
                put("y", y ?: (height / 2))
                put("deltaX", 0)
                put("deltaY", deltaY)
            },
        )
    }

    override suspend fun evaluate(tabId: Long, expression: String): String {
        val result = page(tabId).cdp(
            "Runtime.evaluate",
            buildJsonObject {
                put("expression", expression)
                put("returnByValue", true)
                put("awaitPromise", true)
            },
        )

        (result["exceptionDetails"] as? JsonObject)?.let { details ->
            val description = ((details["exception"] as? JsonObject)?.get("description") as? JsonPrimitive)?.contentOrNull
            throw AutomationException("스크립트 오류: ${description ?: (details["text"] as? JsonPrimitive)?.contentOrNull.orEmpty()}")
        }

        return (result["result"] as? JsonObject)?.get("value")?.toString() ?: "undefined"
    }

    override fun close(tabId: Long) {
        BrowserEngine.close(tabId)
    }

    private suspend fun page(tabId: Long): BrowserPage {
        val page = BrowserEngine.existing(tabId) ?: throw AutomationException("탭이 닫혔습니다(tabId=$tabId).")
        page.awaitBrowser()
        return page
    }

    // 불러오기가 시작됐다가 끝날 때까지. 시작 알림이 오기 전에 끝난 것으로 보지 않게 잠깐 기다린다.
    private suspend fun awaitLoad(page: BrowserPage) {
        withTimeoutOrNull(LoadStartWait) { page.isLoading.first { it } }
        withTimeoutOrNull(LoadTimeout) { page.isLoading.first { !it } }
        // 첫 장이 그려질 틈을 준다. 곧바로 캡처하면 앞 페이지가 찍힌다.
        delay(PaintSettle)
    }

    private suspend fun evaluateString(page: BrowserPage, script: String): String {
        val result = page.cdp(
            "Runtime.evaluate",
            buildJsonObject {
                put("expression", script)
                put("returnByValue", true)
            },
        )

        return ((result["result"] as? JsonObject)?.get("value") as? JsonPrimitive)?.contentOrNull
            ?: throw AutomationException("페이지를 읽지 못했습니다")
    }

    private companion object {
        val LoadStartWait = 1.seconds
        val LoadTimeout = 15.seconds
        val PaintSettle = 150.milliseconds
    }
}

internal suspend fun mouse(page: BrowserPage, type: String, x: Int, y: Int, button: String, clickCount: Int, modifiers: Int = 0) {
    page.cdp(
        "Input.dispatchMouseEvent",
        buildJsonObject {
            put("type", type)
            put("x", x)
            put("y", y)
            put("button", button)
            put("clickCount", clickCount)
            put("modifiers", modifiers)
        },
    )
}

internal suspend fun pressKey(page: BrowserPage, spec: CdpKey, modifiers: Int) {
    page.cdp("Input.dispatchKeyEvent", spec.event("rawKeyDown", modifiers))
    spec.text?.let { text ->
        page.cdp("Input.dispatchKeyEvent", buildJsonObject { put("type", "char"); put("text", text); put("modifiers", modifiers) })
    }
    page.cdp("Input.dispatchKeyEvent", spec.event("keyUp", modifiers))
}

/** CDP `Input.dispatchKeyEvent` 에 싣는 키. [text] 가 있으면 `char` 도 보낸다(Enter 가 폼을 보내려면 필요하다). */
internal class CdpKey(
    val key: String,
    val code: String,
    val keyCode: Int,
    val text: String? = null,
) {
    fun event(type: String, modifiers: Int): JsonObject =
        buildJsonObject {
            put("type", type)
            put("key", key)
            put("code", code)
            put("windowsVirtualKeyCode", keyCode)
            put("nativeVirtualKeyCode", keyCode)
            put("modifiers", modifiers)
        }
}

internal val CdpKeys: Map<String, CdpKey> = listOf(
    CdpKey("Enter", "Enter", 13, "\r"),
    CdpKey("Tab", "Tab", 9),
    CdpKey("Backspace", "Backspace", 8),
    CdpKey("Delete", "Delete", 46),
    CdpKey("Escape", "Escape", 27),
    CdpKey("ArrowUp", "ArrowUp", 38),
    CdpKey("ArrowDown", "ArrowDown", 40),
    CdpKey("ArrowLeft", "ArrowLeft", 37),
    CdpKey("ArrowRight", "ArrowRight", 39),
    CdpKey("Home", "Home", 36),
    CdpKey("End", "End", 35),
    CdpKey("PageUp", "PageUp", 33),
    CdpKey("PageDown", "PageDown", 34),
).associateBy { it.key }

// 보이는 상호작용 요소마다 역할·이름·가운데 좌표(CSS 픽셀). Accessibility.getFullAXTree 는 좌표가 없어 요소마다
// DOM.getBoxModel 을 더 불러야 한다(docs/platform/jvm.html#mcp-server).
private val SnapshotScript = """
    (() => {
      const vw = innerWidth, vh = innerHeight, out = [];
      const selector = 'a[href],button,input,select,textarea,summary,[role],[onclick],[contenteditable="true"],[tabindex]:not([tabindex="-1"])';
      for (const el of document.querySelectorAll(selector)) {
        const r = el.getBoundingClientRect();
        if (r.width < 1 || r.height < 1 || r.bottom < 0 || r.right < 0 || r.top > vh || r.left > vw) continue;
        const style = getComputedStyle(el);
        if (style.visibility === 'hidden' || style.display === 'none') continue;
        const tag = el.tagName.toLowerCase();
        const role = el.getAttribute('role') || (el.type && tag === 'input' ? 'input[' + el.type + ']' : tag);
        const name = (el.getAttribute('aria-label') || el.innerText || el.value || el.placeholder || el.title || el.alt || '')
          .trim().replace(/\s+/g, ' ').slice(0, 80);
        out.push(role + ' "' + name + '" (' + Math.round(r.left + r.width / 2) + ', ' + Math.round(r.top + r.height / 2) + ')');
        if (out.length >= 300) break;
      }
      const text = (document.body ? document.body.innerText : '').replace(/\n\s*\n+/g, '\n').slice(0, 3000);
      return 'url=' + location.href + '\ntitle=' + document.title + '\nviewport=' + vw + 'x' + vh +
        '\n\n[elements]\n' + out.join('\n') + '\n\n[text]\n' + text;
    })()
""".trimIndent()
