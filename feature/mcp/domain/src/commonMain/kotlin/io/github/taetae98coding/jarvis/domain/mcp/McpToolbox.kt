package io.github.taetae98coding.jarvis.domain.mcp

import io.github.taetae98coding.jarvis.automation.AgentTabs
import io.github.taetae98coding.jarvis.automation.AutomationDevice
import io.github.taetae98coding.jarvis.automation.AutomationException
import io.github.taetae98coding.jarvis.automation.AutomationImage
import io.github.taetae98coding.jarvis.automation.BrowserAutomation
import io.github.taetae98coding.jarvis.automation.DeviceAutomation
import io.github.taetae98coding.jarvis.automation.DeviceKey
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * MCP 도구 목록과 호출(docs/common/mcp-server.html#tools). 도구 하나를 이음새 셋([AgentTabs]·[BrowserAutomation]·
 * [DeviceAutomation])에 나눠 준다. 이음새가 없는 타깃·조립에서는 그 도구들이 목록에서 빠진다.
 *
 * [sessionId] 는 요청의 `X-Jarvis-Session` 이다. 없거나 모르는 값이면 호출한 패널이 없다(R3).
 */
class McpToolbox(
    private val tabs: AgentTabs?,
    private val browser: BrowserAutomation?,
    private val devices: DeviceAutomation?,
    private val browserTimeout: Duration = BrowserToolTimeout,
    private val deviceTimeout: Duration = DeviceToolTimeout,
) {
    private val lastBrowserTab = mutableMapOf<String, Long>()
    private val lastBrowserTabLock = Mutex()

    val tools: List<McpTool> =
        (if (tabs != null && browser != null) BrowserTools else emptyList()) +
            (if (devices != null) DeviceTools else emptyList())

    suspend fun call(sessionId: String?, name: String, arguments: Map<String, Any?>): ToolResult {
        val tool = tools.firstOrNull { it.name == name } ?: return ToolResult.error("모르는 도구입니다: $name")
        val args = Arguments(arguments)

        return try {
            val timeout = if (tool in DeviceTools) deviceTimeout else browserTimeout
            withTimeout(timeout) { dispatch(sessionId, name, args) }
        } catch (e: TimeoutCancellationException) {
            ToolResult.error("시간 안에 끝나지 않았습니다(${name})")
        } catch (e: CancellationException) {
            throw e
        } catch (e: AutomationException) {
            ToolResult.error(e.message.orEmpty())
        } catch (e: Exception) {
            // 이음새 구현의 예상하지 못한 실패도 도구 오류로 끝낸다. 서버와 다른 도구는 계속 돈다(R19).
            ToolResult.error("${name} 실패: ${e.message ?: e::class.simpleName}")
        }
    }

    private suspend fun dispatch(sessionId: String?, name: String, args: Arguments): ToolResult =
        when (name) {
            "browser_open" -> browserOpen(sessionId, args)
            "browser_tabs" -> browserTabs(sessionId)
            "browser_close" -> browserClose(sessionId, args)
            "browser_navigate" -> withBrowserTab(sessionId, args) { tabId ->
                browser().navigate(tabId, address(args.string("url")))
                pageText(tabId)
            }
            "browser_back" -> withBrowserTab(sessionId, args) { tabId ->
                browser().back(tabId)
                pageText(tabId)
            }
            "browser_screenshot" -> withBrowserTab(sessionId, args) { tabId -> image(browser().screenshot(tabId)) }
            "browser_snapshot" -> withBrowserTab(sessionId, args) { tabId -> ToolResult.text(browser().snapshot(tabId)) }
            "browser_click" -> withBrowserTab(sessionId, args) { tabId ->
                val count = if (args.booleanOrNull("doubleClick") == true) 2 else 1
                browser().click(tabId, args.int("x"), args.int("y"), count)
                Done
            }
            "browser_type" -> withBrowserTab(sessionId, args) { tabId ->
                browser().type(tabId, args.string("text"))
                Done
            }
            "browser_press_key" -> withBrowserTab(sessionId, args) { tabId ->
                browser().pressKey(tabId, args.string("key"))
                Done
            }
            "browser_scroll" -> withBrowserTab(sessionId, args) { tabId ->
                browser().scroll(tabId, args.intOrNull("x"), args.intOrNull("y"), args.int("deltaY"))
                Done
            }
            "browser_evaluate" -> withBrowserTab(sessionId, args) { tabId ->
                ToolResult.text(browser().evaluate(tabId, args.string("expression")))
            }

            "device_list" -> ToolResult.text(devices().devices().joinToString("\n", transform = ::deviceLine).ifEmpty { "연결된 기기가 없습니다." })
            "device_boot" -> {
                val device = device(args)
                devices().boot(device.id)
                ToolResult.text("켜는 중입니다. 켜진 뒤의 식별자는 device_list 로 다시 확인하세요.")
            }
            "device_screenshot" -> withDevice(sessionId, args) { id -> image(devices().screenshot(id)) }
            "device_tap" -> withDevice(sessionId, args) { id ->
                devices().tap(id, args.int("x"), args.int("y"), args.longOrNull("durationMs") ?: 0L)
                Done
            }
            "device_swipe" -> withDevice(sessionId, args) { id ->
                devices().swipe(
                    id,
                    args.int("fromX"), args.int("fromY"), args.int("toX"), args.int("toY"),
                    args.longOrNull("durationMs") ?: DefaultSwipeMillis,
                )
                Done
            }
            "device_type" -> withDevice(sessionId, args) { id ->
                devices().type(id, args.string("text"))
                Done
            }
            "device_press" -> withDevice(sessionId, args) { id ->
                val key = args.string("key")
                devices().press(id, DeviceKey.fromWireName(key) ?: throw AutomationException("모르는 키입니다: $key"))
                Done
            }
            "device_ui_tree" -> withDevice(sessionId, args) { id -> ToolResult.text(devices().uiTree(id)) }
            "device_launch_app" -> withDevice(sessionId, args) { id ->
                devices().launchApp(id, args.string("appId"))
                Done
            }

            else -> ToolResult.error("모르는 도구입니다: $name")
        }

    private suspend fun browserOpen(sessionId: String?, args: Arguments): ToolResult {
        val caller = requireCaller(sessionId)
        val url = address(args.string("url"))
        val tabId = tabs().openBrowserTab(caller, url) ?: throw AutomationException(NoCallerMessage)
        remember(caller, tabId)
        browser().page(tabId, url)

        return ToolResult.text("tabId=$tabId 로 열었습니다.\n${pageText(tabId).text()}")
    }

    private suspend fun browserTabs(sessionId: String?): ToolResult {
        val caller = requireCaller(sessionId)
        val lines = tabs().browserTabs(caller).map { tab ->
            val page = browser().page(tab.tabId, tab.url)
            "tabId=${tab.tabId}\t${page.title.orEmpty()}\t${page.url}"
        }

        return ToolResult.text(lines.joinToString("\n").ifEmpty { "이 패널에 브라우저 탭이 없습니다." })
    }

    private suspend fun browserClose(sessionId: String?, args: Arguments): ToolResult {
        val caller = requireCaller(sessionId)
        val tabId = args.long("tabId")
        if (!tabs().closeTab(caller, tabId)) throw AutomationException(tabGoneMessage(tabId))
        browser().close(tabId)

        return Done
    }

    /** R9. 탭 id 를 정하고, 그 탭이 호출한 패널에 있는지 보고(R8), 페이지를 살린 뒤 [block] 을 부른다. */
    private suspend fun withBrowserTab(sessionId: String?, args: Arguments, block: suspend (Long) -> ToolResult): ToolResult {
        val caller = requireCaller(sessionId)
        val open = tabs().browserTabs(caller)
        val requested = args.longOrNull("tabId")
        val remembered = lastBrowserTabLock.withLock { lastBrowserTab[caller] }

        val tab = when {
            requested != null -> open.firstOrNull { it.tabId == requested } ?: throw AutomationException(tabGoneMessage(requested))
            else -> open.firstOrNull { it.tabId == remembered } ?: open.firstOrNull()
                ?: throw AutomationException("이 패널에 브라우저 탭이 없습니다. browser_open 을 먼저 부르세요.")
        }

        remember(caller, tab.tabId)
        browser().page(tab.tabId, tab.url)

        return block(tab.tabId)
    }

    /** R6. 기기를 찾고, 호출한 패널에 기기 탭을 붙인 뒤 [block] 을 부른다. */
    private suspend fun withDevice(sessionId: String?, args: Arguments, block: suspend (String) -> ToolResult): ToolResult {
        val device = device(args)
        if (sessionId != null) tabs?.showDevice(sessionId, device.id, device.name, device.platform)

        return block(device.id)
    }

    private suspend fun device(args: Arguments): AutomationDevice {
        val id = args.string("deviceId")

        return devices().devices().firstOrNull { it.id == id }
            ?: throw AutomationException("기기가 없습니다: $id. device_list 로 식별자를 확인하세요.")
    }

    private suspend fun requireCaller(sessionId: String?): String {
        if (sessionId == null || !tabs().hasCaller(sessionId)) throw AutomationException(NoCallerMessage)

        return sessionId
    }

    private suspend fun remember(sessionId: String, tabId: Long) {
        lastBrowserTabLock.withLock { lastBrowserTab[sessionId] = tabId }
    }

    private suspend fun pageText(tabId: Long): ToolResult {
        // 페이지는 이미 살아 있다. 주소는 무시된다.
        val page = browser().page(tabId, "")

        return ToolResult.text("url=${page.url}\ntitle=${page.title.orEmpty()}")
    }

    private fun tabs(): AgentTabs = tabs ?: throw AutomationException(NoCallerMessage)

    private fun browser(): BrowserAutomation = browser ?: throw AutomationException("이 앱에는 브라우저 엔진이 없습니다.")

    private fun devices(): DeviceAutomation = devices ?: throw AutomationException("이 앱에서는 기기를 조작할 수 없습니다.")

    private companion object {
        val BrowserToolTimeout = 30.seconds

        // iOS 는 처음 부를 때 WebDriverAgent 를 내려받고 빌드한다(R20).
        val DeviceToolTimeout = 180.seconds

        const val DefaultSwipeMillis = 300L

        const val NoCallerMessage = "Jarvis 터미널의 Claude 탭에서만 브라우저를 쓸 수 있습니다"

        val Done = ToolResult.text("완료")

        fun tabGoneMessage(tabId: Long): String = "탭이 닫혔습니다(tabId=$tabId). browser_tabs 로 열린 탭을 확인하세요."
    }
}

/** 주소가 아니면 https 를 붙인다. 검색어 규칙(웹 브라우저 R4)은 주소 줄의 것이라 여기서는 따르지 않는다. */
internal fun address(input: String): String {
    val trimmed = input.trim()
    if (trimmed.isEmpty()) throw AutomationException("주소가 비어 있습니다.")

    return when {
        "://" in trimmed || trimmed.startsWith("about:") || trimmed.startsWith("data:") -> trimmed
        trimmed.startsWith("localhost") || trimmed.startsWith("127.0.0.1") -> "http://$trimmed"
        else -> "https://$trimmed"
    }
}

private fun image(image: AutomationImage): ToolResult =
    ToolResult(
        listOf(
            ToolContent.Image(image.bytes, image.mimeType),
            ToolContent.Text("${image.width}x${image.height} px. 좌표는 이 이미지의 픽셀 기준입니다."),
        ),
    )

private fun ToolResult.text(): String = content.filterIsInstance<ToolContent.Text>().joinToString("\n") { it.text }

private fun deviceLine(device: AutomationDevice): String =
    listOf(
        device.id,
        device.name,
        device.platform.name.lowercase(),
        if (device.isPhysical) "physical" else "virtual",
        if (device.isRunning) "running" else "off",
        if (device.canControl) "controllable" else "view-only",
    ).joinToString("\t")

/** JSON 을 푼 인자. 숫자는 Long·Double·String 어느 쪽으로 와도 받는다. */
private class Arguments(private val values: Map<String, Any?>) {
    fun string(name: String): String = values[name] as? String ?: throw missing(name)

    fun int(name: String): Int = intOrNull(name) ?: throw missing(name)

    fun intOrNull(name: String): Int? = longOrNull(name)?.toInt()

    fun long(name: String): Long = longOrNull(name) ?: throw missing(name)

    fun longOrNull(name: String): Long? =
        when (val value = values[name]) {
            null -> null
            is Number -> value.toDouble().toLong()
            is String -> value.trim().toDoubleOrNull()?.toLong() ?: throw AutomationException("$name 은 숫자여야 합니다.")
            else -> throw AutomationException("$name 은 숫자여야 합니다.")
        }

    fun booleanOrNull(name: String): Boolean? =
        when (val value = values[name]) {
            null -> null
            is Boolean -> value
            is String -> value.toBooleanStrictOrNull()
            else -> null
        }

    private fun missing(name: String) = AutomationException("인자 $name 이 필요합니다.")
}
