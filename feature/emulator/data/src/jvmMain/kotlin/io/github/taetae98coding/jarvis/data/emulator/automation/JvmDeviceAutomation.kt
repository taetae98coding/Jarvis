package io.github.taetae98coding.jarvis.data.emulator.automation

import io.github.taetae98coding.jarvis.automation.AutomationDevice
import io.github.taetae98coding.jarvis.automation.AutomationException
import io.github.taetae98coding.jarvis.automation.AutomationImage
import io.github.taetae98coding.jarvis.automation.AutomationPlatform
import io.github.taetae98coding.jarvis.automation.DeviceAutomation
import io.github.taetae98coding.jarvis.automation.DeviceKey
import io.github.taetae98coding.jarvis.data.emulator.EmulatorDataSource
import io.github.taetae98coding.jarvis.data.emulator.PhysicalIosPrefix
import io.github.taetae98coding.jarvis.data.emulator.SimulatorUdid
import io.github.taetae98coding.jarvis.data.emulator.StoppedAvdPrefix
import io.github.taetae98coding.jarvis.data.emulator.isAdbSerial
import io.github.taetae98coding.jarvis.domain.emulator.EmulatorDevice
import io.github.taetae98coding.jarvis.domain.emulator.EmulatorPlatform
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

/**
 * 기기 식별자의 모양으로 도구를 가른다(docs/common/emulator-control.html#ids). adb 시리얼은 [AndroidAutomation],
 * 시뮬레이터 UUID 와 `ios:` 실물은 [IosAutomation] 이다. SDK·Xcode 가 없어 한쪽이 null 이면 그 기기의 도구가 오류다.
 */
internal class JvmDeviceAutomation(
    private val dataSource: EmulatorDataSource,
    private val android: AndroidAutomation?,
    private val ios: IosAutomation?,
) : DeviceAutomation {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val watch = Mutex()
    private var watcher: Job? = null
    private var watchUntil = TimeSource.Monotonic.markNow()

    // 목록을 한 번 세는 데 7초쯤 걸린다(xctrace·devicectl·기기마다 adb). 도구마다 이름을 찾으려고 새로 세지 않도록,
    // 도구가 쓰이는 동안 목록을 구독해 둔다. 공유 Flow 의 replay 가 다음 호출에 곧바로 답한다.
    override suspend fun devices(): List<AutomationDevice> {
        watch.withLock {
            watchUntil = TimeSource.Monotonic.markNow() + DeviceListLease
            if (watcher?.isActive != true) {
                watcher = scope.launch {
                    val collector = launch { dataSource.observeDevices().collect {} }
                    while (!watch.withLock { watchUntil }.hasPassedNow()) delay(1.seconds)
                    collector.cancel()
                }
            }
        }

        return dataSource.observeDevices().first().map { device ->
            AutomationDevice(
                id = device.id,
                name = device.name,
                platform = if (device.platform == EmulatorPlatform.ANDROID) AutomationPlatform.ANDROID else AutomationPlatform.IOS,
                isPhysical = device.isPhysical,
                isRunning = device.isRunning,
                canControl = canControl(device),
            )
        }
    }

    // 기기 목록 화면의 canControl 과 다르다. 그쪽은 사람의 제스처이고, 여기는 WebDriverAgent 로 iOS 도 된다.
    private fun canControl(device: EmulatorDevice): Boolean =
        device.isRunning && when (device.platform) {
            EmulatorPlatform.ANDROID -> android != null
            EmulatorPlatform.IOS -> ios != null
        }

    override suspend fun boot(deviceId: String) {
        dataSource.launch(deviceId)
    }

    override suspend fun screenshot(deviceId: String): AutomationImage =
        route(deviceId,
            android = { screenshot(it) },
            ios = { udid, physical -> screenshot(udid, physical) },
        )

    override suspend fun tap(deviceId: String, x: Int, y: Int, durationMs: Long) =
        route(deviceId,
            android = { tap(it, x, y, durationMs) },
            ios = { udid, physical -> tap(udid, physical, x, y, durationMs) },
        )

    override suspend fun swipe(deviceId: String, fromX: Int, fromY: Int, toX: Int, toY: Int, durationMs: Long) =
        route(deviceId,
            android = { swipe(it, fromX, fromY, toX, toY, durationMs) },
            ios = { udid, physical -> swipe(udid, physical, fromX, fromY, toX, toY, durationMs) },
        )

    override suspend fun type(deviceId: String, text: String) =
        route(deviceId,
            android = { type(it, text) },
            ios = { udid, physical -> type(udid, physical, text) },
        )

    override suspend fun press(deviceId: String, key: DeviceKey) =
        route(deviceId,
            android = { press(it, key) },
            ios = { udid, physical -> press(udid, physical, key) },
        )

    override suspend fun uiTree(deviceId: String): String =
        route(deviceId,
            android = { uiTree(it) },
            ios = { udid, physical -> uiTree(udid, physical) },
        )

    override suspend fun launchApp(deviceId: String, appId: String) =
        route(deviceId,
            android = { launchApp(it, appId) },
            ios = { udid, physical -> launchApp(udid, physical, appId) },
        )

    private suspend fun <T> route(
        deviceId: String,
        android: suspend AndroidAutomation.(String) -> T,
        ios: suspend IosAutomation.(udid: String, physical: Boolean) -> T,
    ): T =
        when {
            deviceId.startsWith(StoppedAvdPrefix) -> throw AutomationException("꺼진 에뮬레이터입니다: $deviceId. device_boot 로 켜세요.")
            deviceId.startsWith(PhysicalIosPrefix) -> requireIos().ios(deviceId.removePrefix(PhysicalIosPrefix), true)
            SimulatorUdid.matches(deviceId) -> requireIos().ios(deviceId, false)
            isAdbSerial(deviceId) -> (this.android ?: throw AutomationException("Android SDK(adb)를 찾지 못했습니다.")).android(deviceId)
            else -> throw AutomationException("모르는 기기입니다: $deviceId")
        }

    private fun requireIos(): IosAutomation = ios ?: throw AutomationException("iOS 기기를 조작하려면 Xcode 가 있어야 합니다.")

    private companion object {
        val DeviceListLease = 60.seconds
    }
}
