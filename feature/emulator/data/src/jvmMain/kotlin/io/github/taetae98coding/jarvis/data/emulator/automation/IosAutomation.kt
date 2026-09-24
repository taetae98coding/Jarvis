package io.github.taetae98coding.jarvis.data.emulator.automation

import io.github.taetae98coding.jarvis.automation.AutomationException
import io.github.taetae98coding.jarvis.automation.AutomationImage
import io.github.taetae98coding.jarvis.automation.DeviceKey
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap

/**
 * iOS 시뮬레이터·실물 기기의 도구. 전부 WebDriverAgent 로 간다. 좌표는 포인트이고, 캡처도 포인트 크기로 줄여서
 * 이미지 픽셀과 포인트가 같다(docs/common/mcp-server.html R17).
 */
internal class IosAutomation(
    private val runner: WdaRunner,
) {
    private val locks = ConcurrentHashMap<String, Mutex>()

    suspend fun screenshot(udid: String, physical: Boolean): AutomationImage {
        val agent = agent(udid, physical)
        val (width, height) = agent.windowSize()

        return resizeImage(agent.screenshotPng(), width, height) ?: throw AutomationException("iOS 화면을 읽지 못했습니다")
    }

    suspend fun tap(udid: String, physical: Boolean, x: Int, y: Int, durationMs: Long) {
        val agent = agent(udid, physical)
        if (durationMs >= LongPressMillis) agent.touchAndHold(x, y, durationMs / 1000.0) else agent.tap(x, y)
    }

    suspend fun swipe(udid: String, physical: Boolean, fromX: Int, fromY: Int, toX: Int, toY: Int, durationMs: Long) {
        agent(udid, physical).swipe(fromX, fromY, toX, toY, durationMs)
    }

    suspend fun type(udid: String, physical: Boolean, text: String) {
        agent(udid, physical).type(text)
    }

    suspend fun press(udid: String, physical: Boolean, key: DeviceKey) {
        val agent = agent(udid, physical)
        when (key) {
            DeviceKey.HOME -> agent.home()
            DeviceKey.VOLUME_UP -> agent.pressButton("volumeUp")
            DeviceKey.VOLUME_DOWN -> agent.pressButton("volumeDown")
            // XCUIKeyboardKey.return / .delete 의 글자다.
            DeviceKey.ENTER -> agent.type("\n")
            DeviceKey.DELETE -> agent.type("\b")
            DeviceKey.BACK, DeviceKey.APP_SWITCH, DeviceKey.POWER -> throw AutomationException("iOS 에 없는 키입니다: ${key.wireName}")
        }
    }

    suspend fun uiTree(udid: String, physical: Boolean): String = parseWdaTree(agent(udid, physical).source())

    suspend fun launchApp(udid: String, physical: Boolean, bundleId: String) {
        agent(udid, physical).launch(bundleId)
    }

    // 같은 기기를 두 도구가 동시에 준비하면 러너를 두 번 띄운다. 기기마다 한 줄로 세운다.
    private suspend fun agent(udid: String, physical: Boolean): WebDriverAgent =
        locks.getOrPut(udid) { Mutex() }.withLock { runner.start(udid, physical) }

    private companion object {
        // 이보다 짧으면 XCUITest 의 탭으로 충분하다.
        const val LongPressMillis = 400L
    }
}
