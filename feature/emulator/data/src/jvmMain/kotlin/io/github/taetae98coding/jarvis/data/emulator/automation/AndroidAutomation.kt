package io.github.taetae98coding.jarvis.data.emulator.automation

import io.github.taetae98coding.jarvis.automation.AutomationException
import io.github.taetae98coding.jarvis.automation.AutomationImage
import io.github.taetae98coding.jarvis.automation.DeviceKey
import io.github.taetae98coding.jarvis.data.emulator.adbBinary
import io.github.taetae98coding.jarvis.data.emulator.mirror.ControlMessages
import io.github.taetae98coding.jarvis.data.emulator.mirror.ScreenMirror
import io.github.taetae98coding.jarvis.data.emulator.runCommandBytes
import io.github.taetae98coding.jarvis.data.emulator.runCommandOutput
import io.github.taetae98coding.jarvis.domain.emulator.EmulatorFrame
import io.github.taetae98coding.jarvis.domain.emulator.EmulatorGesture
import io.github.taetae98coding.jarvis.domain.emulator.TouchAction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

/**
 * adb 시리얼 기기(에뮬레이터·실물 Android)의 도구(docs/platform/jvm.html#mcp-server). 화면과 입력은 기기 화면이
 * 쓰는 scrcpy 세션을 같이 쓴다 — 사람이 보고 있으면 그 스트림을, 아니면 도구가 세션을 잠시 붙잡는다.
 */
internal class AndroidAutomation(
    private val sdk: File,
    private val mirror: ScreenMirror,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val leases = ConcurrentHashMap<String, Lease>()

    suspend fun screenshot(serial: String): AutomationImage {
        val frame = frame(serial)
        if (frame != null) {
            val (width, height) = imageSize(frame.width, frame.height)
            return bgraToImage(frame.width, frame.height, frame.pixels, width, height)
        }

        // 스트림을 올리지 못한 기기(서버를 거부하는 기기 등)라도 한 장은 찍는다.
        val png = withContext(Dispatchers.IO) { runCommandBytes(listOf(adb(), "-s", serial, "exec-out", "screencap", "-p"), timeoutSeconds = 15) }
            ?: throw AutomationException("화면을 가져오지 못했습니다: $serial")
        val (sourceWidth, sourceHeight) = imageSize(png) ?: throw AutomationException("화면을 가져오지 못했습니다: $serial")
        val (width, height) = imageSize(sourceWidth, sourceHeight)

        return resizeImage(png, width, height) ?: throw AutomationException("화면을 가져오지 못했습니다: $serial")
    }

    suspend fun tap(serial: String, x: Int, y: Int, durationMs: Long) {
        val size = frameSize(serial)
        touch(serial, TouchAction.DOWN, x, y, size)
        delay(durationMs.coerceAtLeast(TapHoldMillis).milliseconds)
        touch(serial, TouchAction.UP, x, y, size)
    }

    suspend fun swipe(serial: String, fromX: Int, fromY: Int, toX: Int, toY: Int, durationMs: Long) {
        val size = frameSize(serial)
        val steps = (durationMs / SwipeStepMillis).toInt().coerceAtLeast(1)

        touch(serial, TouchAction.DOWN, fromX, fromY, size)
        for (step in 1..steps) {
            delay(SwipeStepMillis.milliseconds)
            val t = step.toDouble() / steps
            touch(serial, TouchAction.MOVE, lerp(fromX, toX, t), lerp(fromY, toY, t), size)
        }
        touch(serial, TouchAction.UP, toX, toY, size)
    }

    // INJECT_TEXT 는 기기 키맵에 있는 글자만 넣는다. 그 밖(한글 등)은 클립보드에 넣고 붙여 넣는다.
    suspend fun type(serial: String, text: String) {
        lease(serial)
        if (text.all { it == '\n' || it in ' '..'~' }) {
            // ASCII 라 글자 수가 곧 UTF-8 바이트 수다.
            text.chunked(ControlMessages.InjectTextMaxBytes).forEach { send(serial, ControlMessages.encodeText(it)) }
        } else {
            send(serial, ControlMessages.encodeSetClipboard(text, paste = true))
        }
    }

    suspend fun press(serial: String, key: DeviceKey) {
        lease(serial)
        val keycode = AndroidKeycodes[key] ?: throw AutomationException("Android 에 없는 키입니다: ${key.wireName}")
        send(serial, ControlMessages.encodeKeycode(ControlMessages.KeyActionDown, keycode))
        send(serial, ControlMessages.encodeKeycode(ControlMessages.KeyActionUp, keycode))
    }

    suspend fun uiTree(serial: String): String {
        val xml = withContext(Dispatchers.IO) {
            runCommandOutput(listOf(adb(), "-s", serial, "exec-out", "uiautomator", "dump", "/dev/tty"), timeoutSeconds = 20)
        } ?: throw AutomationException("UI 트리를 가져오지 못했습니다: $serial")
        if ("<hierarchy" !in xml) throw AutomationException("UI 트리를 가져오지 못했습니다: ${xml.trim().take(200)}")

        // bounds 는 디스플레이 픽셀이다. 캡처 이미지는 디스플레이와 비율이 같으므로 긴 변끼리 맞춘다. 루트 노드의
        // bounds 는 앱 창이라(화면 분할이면 반쪽) 디스플레이 크기는 `wm size` 로 따로 읽는다.
        val display = withContext(Dispatchers.IO) { runCommandOutput(listOf(adb(), "-s", serial, "shell", "wm", "size"), timeoutSeconds = 10) }
            ?.let(::parseWmSize)
            ?: throw AutomationException("화면 크기를 읽지 못했습니다: $serial")
        val (imageWidth, imageHeight) = frameSize(serial).let { imageSize(it.first, it.second) }
        val scale = maxOf(imageWidth, imageHeight).toDouble() / maxOf(display.first, display.second)

        return parseUiAutomatorTree(xml, scale)
    }

    suspend fun launchApp(serial: String, packageName: String) {
        val output = withContext(Dispatchers.IO) {
            runCommandOutput(
                listOf(adb(), "-s", serial, "shell", "monkey", "-p", packageName, "-c", "android.intent.category.LAUNCHER", "1"),
                timeoutSeconds = 20,
            )
        }.orEmpty()

        if ("Events injected" !in output) {
            throw AutomationException("앱을 띄우지 못했습니다: $packageName (${output.lineSequence().firstOrNull { it.isNotBlank() }?.trim().orEmpty()})")
        }
    }

    // 도구의 좌표는 캡처 이미지 픽셀이다. 지금 프레임 크기에 같은 배율 규칙을 거꾸로 적용해 기기 영상 좌표로 바꾼다.
    private suspend fun touch(serial: String, action: TouchAction, x: Int, y: Int, frame: Pair<Int, Int>) {
        val (frameWidth, frameHeight) = frame
        val scale = fitScale(frameWidth, frameHeight)
        val gesture = EmulatorGesture.Touch(
            action = action,
            x = (x / scale).roundToInt().coerceIn(0, frameWidth - 1),
            y = (y / scale).roundToInt().coerceIn(0, frameHeight - 1),
            frameWidth = frameWidth,
            frameHeight = frameHeight,
        )
        send(serial, ControlMessages.encode(gesture))
    }

    private suspend fun send(serial: String, message: ByteArray) {
        if (!mirror.send(serial, message)) throw AutomationException("기기에 입력 경로를 열지 못했습니다: $serial. 기기가 연결돼 있고 화면이 켜져 있는지 확인하세요.")
    }

    private suspend fun frameSize(serial: String): Pair<Int, Int> {
        val frame = frame(serial) ?: throw AutomationException("기기 화면을 받지 못했습니다: $serial")
        return frame.width to frame.height
    }

    /** 최신 프레임의 복사본. 버퍼는 디코더가 돌려 쓰므로 받은 자리에서 복사한다. */
    private suspend fun frame(serial: String): EmulatorFrame.Pixels? {
        lease(serial)

        return withTimeoutOrNull(FirstFrameTimeout) {
            mirror.observeScreen(serial).filterIsInstance<EmulatorFrame.Pixels>().first()
        }?.let { EmulatorFrame.Pixels(it.width, it.height, it.pixels.copyOf()) }
    }

    private fun imageSize(width: Int, height: Int): Pair<Int, Int> {
        val scale = fitScale(width, height)
        return scaled(width, scale) to scaled(height, scale)
    }

    /**
     * 도구가 연달아 불리는 동안 세션을 붙잡아 둔다. 붙잡지 않으면 호출 사이마다 서버가 내려갔다가(MirrorSessionLinger)
     * 다시 올라와 첫 프레임을 기다린다. 마지막 호출에서 [LeaseDuration] 이 지나면 놓는다.
     */
    private fun lease(serial: String) {
        val now = TimeSource.Monotonic.markNow()
        leases.compute(serial) { _, existing ->
            if (existing != null && existing.job.isActive) {
                existing.also { it.until = now + LeaseDuration }
            } else {
                Lease(now + LeaseDuration).also { lease ->
                    lease.job = scope.launch {
                        val collector = launch { mirror.observeScreen(serial).collect {} }
                        while (!lease.until.hasPassedNow()) delay(LeaseCheckInterval)
                        collector.cancel()
                        leases.remove(serial, lease)
                    }
                }
            }
        }
    }

    private fun adb(): String = adbBinary(sdk)

    private class Lease(@Volatile var until: TimeSource.Monotonic.ValueTimeMark) {
        lateinit var job: Job
    }

    private companion object {
        val LeaseDuration = 30.seconds
        val LeaseCheckInterval = 1.seconds

        // 에이전트 올리기부터 첫 키프레임까지 0.5~1.5초다(docs/common/device-mirroring.html#behavior).
        val FirstFrameTimeout = 8.seconds

        // 누르고 곧바로 떼면 일부 앱이 누름을 놓친다. 사람의 짧은 탭 정도로 둔다.
        const val TapHoldMillis = 60L
        const val SwipeStepMillis = 16L

        // android.view.KeyEvent 의 KEYCODE_*.
        val AndroidKeycodes = mapOf(
            DeviceKey.BACK to 4,
            DeviceKey.HOME to 3,
            DeviceKey.APP_SWITCH to 187,
            DeviceKey.ENTER to 66,
            DeviceKey.DELETE to 67,
            DeviceKey.POWER to 26,
            DeviceKey.VOLUME_UP to 24,
            DeviceKey.VOLUME_DOWN to 25,
        )
    }
}

private fun lerp(from: Int, to: Int, t: Double): Int = (from + (to - from) * t).roundToInt()
