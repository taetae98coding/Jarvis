package io.github.taetae98coding.jarvis.domain.mcp

import io.github.taetae98coding.jarvis.automation.AgentBrowserTab
import io.github.taetae98coding.jarvis.automation.AgentPanel
import io.github.taetae98coding.jarvis.automation.AgentTabs
import io.github.taetae98coding.jarvis.automation.AutomationDevice
import io.github.taetae98coding.jarvis.automation.AutomationException
import io.github.taetae98coding.jarvis.automation.AutomationImage
import io.github.taetae98coding.jarvis.automation.AutomationPlatform
import io.github.taetae98coding.jarvis.automation.BrowserAutomation
import io.github.taetae98coding.jarvis.automation.BrowserPageInfo
import io.github.taetae98coding.jarvis.automation.DeviceAutomation
import io.github.taetae98coding.jarvis.automation.DeviceKey
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

class McpToolboxTest {
    private val tabs = FakeTabs()
    private val browser = FakeBrowser()
    private val devices = FakeDevices()
    private val toolbox = McpToolbox(tabs, browser, devices)

    @Test
    fun listsBrowserToolsOnlyWithTabsAndEngine() {
        assertTrue(toolbox.tools.any { it.name == "browser_open" })
        assertTrue(McpToolbox(tabs = null, browser = browser, devices = devices).tools.none { it.name.startsWith("browser_") })
        assertTrue(McpToolbox(tabs = tabs, browser = null, devices = null).tools.isEmpty())
    }

    @Test
    fun browserToolsNeedACallerTab() = runTest {
        val result = toolbox.call(sessionId = null, name = "browser_open", arguments = mapOf("url" to "example.com"))

        assertTrue(result.isError)
        assertEquals(emptyList(), tabs.opened)
    }

    @Test
    fun openAddsATabAndLaterCallsUseIt() = runTest {
        val opened = toolbox.call(Session, "browser_open", mapOf("url" to "example.com"))
        assertFalse(opened.isError, opened.toString())
        assertEquals(listOf("https://example.com"), tabs.opened)

        toolbox.call(Session, "browser_click", mapOf("x" to 10L, "y" to 20.0))

        assertEquals(listOf(Click(OpenedTab, 10, 20, 1)), browser.clicks)
    }

    @Test
    fun explicitTabMustBelongToTheCallerPanel() = runTest {
        val result = toolbox.call(Session, "browser_click", mapOf("tabId" to 999L, "x" to 1L, "y" to 1L))

        assertTrue(result.isError)
        assertTrue(browser.clicks.isEmpty())
    }

    @Test
    fun withoutTabIdTheFirstPanelTabIsUsed() = runTest {
        tabs.existing += AgentBrowserTab(7, "https://a.test")

        toolbox.call(Session, "browser_type", mapOf("text" to "안녕"))

        assertEquals(listOf(7L to "안녕"), browser.typed)
        assertEquals(listOf(7L to "https://a.test"), browser.pages)
    }

    @Test
    fun deviceToolsShowTheDeviceInTheCallerPanel() = runTest {
        val result = toolbox.call(Session, "device_tap", mapOf("deviceId" to "emulator-5554", "x" to 5L, "y" to 6L))

        assertFalse(result.isError, result.toString())
        assertEquals(listOf("emulator-5554" to "Pixel"), tabs.shown)
        assertEquals(listOf(Triple("emulator-5554", 5, 6)), devices.taps)
    }

    @Test
    fun deviceToolsWorkWithoutACallerButAddNoTab() = runTest {
        toolbox.call(sessionId = null, name = "device_press", arguments = mapOf("deviceId" to "emulator-5554", "key" to "home"))

        assertTrue(tabs.shown.isEmpty())
        assertEquals(listOf(DeviceKey.HOME), devices.keys)
    }

    @Test
    fun deviceListAddsNoTab() = runTest {
        val result = toolbox.call(Session, "device_list", emptyMap())

        assertTrue(result.content.single().let { it is ToolContent.Text && "emulator-5554" in it.text })
        assertTrue(tabs.shown.isEmpty())
    }

    @Test
    fun unknownDeviceIsAnError() = runTest {
        val result = toolbox.call(Session, "device_tap", mapOf("deviceId" to "nope", "x" to 1L, "y" to 1L))

        assertTrue(result.isError)
        assertTrue(tabs.shown.isEmpty())
    }

    @Test
    fun screenshotReturnsImageAndSize() = runTest {
        val result = toolbox.call(Session, "device_screenshot", mapOf("deviceId" to "emulator-5554"))

        assertIs<ToolContent.Image>(result.content.first())
        assertTrue((result.content.last() as ToolContent.Text).text.startsWith("640x1280"))
    }

    @Test
    fun failuresBecomeToolErrors() = runTest {
        devices.failure = AutomationException("기기가 잠겨 있습니다")

        val result = toolbox.call(Session, "device_type", mapOf("deviceId" to "emulator-5554", "text" to "a"))

        assertTrue(result.isError)
        assertEquals(ToolContent.Text("기기가 잠겨 있습니다"), result.content.single())
    }

    @Test
    fun missingArgumentIsAnError() = runTest {
        assertTrue(toolbox.call(Session, "device_tap", mapOf("deviceId" to "emulator-5554")).isError)
    }

    @Test
    fun slowCallTimesOut() = runTest {
        val slow = McpToolbox(tabs, browser, object : DeviceAutomation by devices {
            override suspend fun devices(): List<AutomationDevice> = awaitCancellation()
        }, deviceTimeout = 1.seconds)

        assertTrue(slow.call(Session, "device_list", emptyMap()).isError)
    }

    @Test
    fun deviceHeldByAnotherPanelIsRefused() = runTest {
        assertFalse(toolbox.call(Session, "device_tap", tap()).isError)

        val refused = toolbox.call(OtherSession, "device_tap", tap())

        assertTrue(refused.isError)
        assertTrue("feature-a" in (refused.content.single() as ToolContent.Text).text)
        assertEquals(1, devices.taps.size)
        assertEquals(listOf(Session), tabs.shownBy)
    }

    @Test
    fun callerWithoutPanelUsesOnlyFreeDevicesAndHoldsNothing() = runTest {
        assertFalse(toolbox.call(sessionId = null, name = "device_tap", arguments = tap()).isError)
        assertFalse(toolbox.call(OtherSession, "device_tap", tap()).isError)

        assertTrue(toolbox.call(sessionId = null, name = "device_tap", arguments = tap()).isError)
        assertTrue(toolbox.call(sessionId = null, name = "device_acquire", arguments = mapOf("platform" to "android")).isError)
    }

    @Test
    fun bootedAvdKeepsItsLease() = runTest {
        devices.list = listOf(StoppedPixel)

        val acquired = toolbox.call(Session, "device_acquire", mapOf("deviceId" to "avd:Pixel_9"))
        val serial = devices.list.single().id

        assertEquals(listOf("avd:Pixel_9"), devices.booted)
        assertTrue("deviceId=$serial" in (acquired.content.single() as ToolContent.Text).text)
        assertEquals(listOf(serial to "Pixel_9"), tabs.shown)
        assertTrue(toolbox.call(OtherSession, "device_screenshot", mapOf("deviceId" to serial)).isError)
    }

    @Test
    fun acquirePrefersPhysicalThenRunningThenStoppedEmulator() = runTest {
        devices.list = listOf(StoppedPixel, RunningPixel, Galaxy)

        assertEquals("deviceId=R3CY705Y62R", acquiredId(Session))
        assertEquals("deviceId=R3CY705Y62R", acquiredId(Session))
        assertEquals("deviceId=emulator-5554", acquiredId(OtherSession))
        assertEquals("deviceId=emulator-5556", acquiredId(ThirdSession))
        assertEquals(listOf("avd:Pixel_9"), devices.booted)
        assertTrue(devices.created.isEmpty())
    }

    @Test
    fun acquireCreatesAnEmulatorWhenNothingIsFree() = runTest {
        toolbox.call(OtherSession, "device_tap", tap())

        val acquired = acquiredId(Session)

        assertEquals(listOf(AutomationPlatform.ANDROID), devices.created)
        assertEquals(listOf("avd:Jarvis_1"), devices.booted)
        assertEquals("deviceId=${devices.list.last().id}", acquired)
        assertTrue(toolbox.call(OtherSession, "device_tap", tap(devices.list.last().id)).isError)
    }

    @Test
    fun releaseLetsAnotherPanelUseTheDevice() = runTest {
        toolbox.call(Session, "device_tap", tap())
        assertTrue(toolbox.call(OtherSession, "device_release", mapOf("deviceId" to "emulator-5554")).isError)

        toolbox.call(Session, "device_release", emptyMap())

        assertFalse(toolbox.call(OtherSession, "device_tap", tap()).isError)
    }

    @Test
    fun leaseEndsWhenThePanelHasNoClaudeTab() = runTest {
        toolbox.call(Session, "device_tap", tap())

        tabs.livePanels.remove(PanelA)

        assertFalse(toolbox.call(OtherSession, "device_tap", tap()).isError)
    }

    @Test
    fun deviceListShowsWhoHoldsEachDevice() = runTest {
        devices.list = listOf(RunningPixel, Galaxy, StoppedPixel)
        toolbox.call(Session, "device_tap", tap())
        toolbox.call(OtherSession, "device_tap", tap(Galaxy.id))

        val lines = (toolbox.call(Session, "device_list", emptyMap()).content.single() as ToolContent.Text).text.lines()

        assertEquals(listOf("mine", "in-use:feature-b", "free"), lines.map { it.substringAfterLast('\t') })
    }

    private suspend fun acquiredId(session: String): String =
        (toolbox.call(session, "device_acquire", mapOf("platform" to "android")).content.single() as ToolContent.Text).text
            .lineSequence().first().substringBefore('\t')

    private fun tap(deviceId: String = "emulator-5554"): Map<String, Any?> = mapOf("deviceId" to deviceId, "x" to 1L, "y" to 1L)

    private data class Click(val tabId: Long, val x: Int, val y: Int, val count: Int)

    private class FakeTabs : AgentTabs {
        val opened = mutableListOf<String>()
        val shown = mutableListOf<Pair<String, String>>()
        val shownBy = mutableListOf<String>()
        val existing = mutableListOf<AgentBrowserTab>()

        val sessions = mutableMapOf(Session to PanelA, OtherSession to PanelB, ThirdSession to PanelC)
        val livePanels = mutableListOf(PanelA, PanelB, PanelC)

        override suspend fun callerPanel(sessionId: String): AgentPanel? = sessions[sessionId]

        override suspend fun claudePanels(): List<AgentPanel> = livePanels.toList()

        override suspend fun openBrowserTab(sessionId: String, url: String): Long {
            opened += url
            existing += AgentBrowserTab(OpenedTab, url)
            return OpenedTab
        }

        override suspend fun browserTabs(sessionId: String): List<AgentBrowserTab> = existing.toList()

        override suspend fun showDevice(sessionId: String, deviceId: String, deviceName: String, platform: AutomationPlatform) {
            shown += deviceId to deviceName
            shownBy += sessionId
        }

        override suspend fun closeTab(sessionId: String, tabId: Long): Boolean = existing.removeAll { it.tabId == tabId }
    }

    private class FakeBrowser : BrowserAutomation {
        val clicks = mutableListOf<Click>()
        val typed = mutableListOf<Pair<Long, String>>()
        val pages = mutableListOf<Pair<Long, String>>()

        override suspend fun page(tabId: Long, initialUrl: String): BrowserPageInfo {
            if (initialUrl.isNotEmpty()) pages += tabId to initialUrl
            return BrowserPageInfo("https://example.com", "Example")
        }

        override suspend fun navigate(tabId: Long, url: String) = Unit

        override suspend fun back(tabId: Long) = Unit

        override suspend fun screenshot(tabId: Long): AutomationImage = AutomationImage(ByteArray(1), "image/png", 800, 600)

        override suspend fun snapshot(tabId: Long): String = ""

        override suspend fun click(tabId: Long, x: Int, y: Int, clickCount: Int) {
            clicks += Click(tabId, x, y, clickCount)
        }

        override suspend fun type(tabId: Long, text: String) {
            typed += tabId to text
        }

        override suspend fun pressKey(tabId: Long, key: String) = Unit

        override suspend fun scroll(tabId: Long, x: Int?, y: Int?, deltaY: Int) = Unit

        override suspend fun evaluate(tabId: Long, expression: String): String = "null"

        override fun close(tabId: Long) = Unit
    }

    private class FakeDevices : DeviceAutomation {
        val taps = mutableListOf<Triple<String, Int, Int>>()
        val keys = mutableListOf<DeviceKey>()
        var failure: Exception? = null

        var list = listOf(RunningPixel)
        val booted = mutableListOf<String>()
        val created = mutableListOf<AutomationPlatform>()

        override suspend fun devices(): List<AutomationDevice> = list

        // 꺼진 AVD 를 켜면 시리얼이 붙는다. 이름은 그대로라 같은 할당이다.
        override suspend fun boot(deviceId: String): String {
            booted += deviceId
            val device = list.first { it.id == deviceId }
            val serial = "emulator-${5554 + booted.size * 2}"
            list = list - device + device.copy(id = serial, isRunning = true, canControl = true)
            return serial
        }

        override suspend fun create(platform: AutomationPlatform): AutomationDevice {
            created += platform
            return AutomationDevice("avd:Jarvis_1", "Jarvis_1", platform, isPhysical = false, isRunning = false, canControl = false)
                .also { list = list + it }
        }

        override suspend fun screenshot(deviceId: String): AutomationImage = AutomationImage(ByteArray(1), "image/png", 640, 1280)

        override suspend fun tap(deviceId: String, x: Int, y: Int, durationMs: Long) {
            taps += Triple(deviceId, x, y)
        }

        override suspend fun swipe(deviceId: String, fromX: Int, fromY: Int, toX: Int, toY: Int, durationMs: Long) = Unit

        override suspend fun type(deviceId: String, text: String) {
            failure?.let { throw it }
        }

        override suspend fun press(deviceId: String, key: DeviceKey) {
            keys += key
        }

        override suspend fun uiTree(deviceId: String): String = ""

        override suspend fun launchApp(deviceId: String, appId: String) = Unit
    }

    private companion object {
        const val Session = "session-1"
        const val OtherSession = "session-2"
        const val ThirdSession = "session-3"
        const val OpenedTab = 42L

        val PanelA = AgentPanel(1, "feature-a")
        val PanelB = AgentPanel(2, "feature-b")
        val PanelC = AgentPanel(3, "feature-c")

        val RunningPixel = AutomationDevice("emulator-5554", "Pixel", AutomationPlatform.ANDROID, isPhysical = false, isRunning = true, canControl = true)
        val StoppedPixel = AutomationDevice("avd:Pixel_9", "Pixel_9", AutomationPlatform.ANDROID, isPhysical = false, isRunning = false, canControl = false)
        val Galaxy = AutomationDevice("R3CY705Y62R", "SM-S928N", AutomationPlatform.ANDROID, isPhysical = true, isRunning = true, canControl = true)
    }
}
