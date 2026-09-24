package io.github.taetae98coding.jarvis.ui.emulator

import io.github.taetae98coding.jarvis.domain.emulator.DevicePairingRepository
import io.github.taetae98coding.jarvis.domain.emulator.EmulatorDevice
import io.github.taetae98coding.jarvis.domain.emulator.EmulatorGesture
import io.github.taetae98coding.jarvis.domain.emulator.EmulatorPlatform
import io.github.taetae98coding.jarvis.domain.emulator.EmulatorRepository
import io.github.taetae98coding.jarvis.domain.emulator.EmulatorStatus
import io.github.taetae98coding.jarvis.domain.emulator.EmulatorSummary
import io.github.taetae98coding.jarvis.domain.emulator.PairingResult
import io.github.taetae98coding.jarvis.domain.emulator.PairingService
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow

// 앱 셸의 화면 테스트에도 같은 모양의 가짜가 있다. KMP 에는 테스트 코드를 모듈 사이에 공유하는
// 깔끔한 수단이 없어서, 기능 테스트가 필요한 만큼만 여기에 따로 둔다.
internal class FakeEmulatorRepository(
    android: EmulatorSummary? = null,
    ios: EmulatorSummary? = null,
    devices: List<EmulatorDevice> = emptyList(),
) : EmulatorRepository {
    val status = MutableStateFlow(EmulatorStatus(android = android, ios = ios))

    val devices = MutableStateFlow(devices)

    val launched = mutableListOf<String>()

    val woken = mutableListOf<String>()

    override fun observeStatus() = status

    override fun observeDevices() = devices

    override fun observeScreen(deviceId: String) = emptyFlow<ByteArray?>()

    override suspend fun sendGesture(deviceId: String, gesture: EmulatorGesture) = Unit

    override suspend fun launch(deviceId: String) {
        launched += deviceId
    }

    override suspend fun wake(deviceId: String) {
        woken += deviceId
    }
}

// 결과는 테스트가 정할 때까지 돌려주지 않는다. 그동안이 "페어링하는 중" 이다.
internal class FakeDevicePairingRepository(
    services: List<PairingService>? = emptyList(),
) : DevicePairingRepository {
    val services = MutableStateFlow(services)

    val paired = mutableListOf<Pair<PairingService, String>>()

    val result = CompletableDeferred<PairingResult>()

    override fun observePairingServices() = services

    override suspend fun pair(service: PairingService, code: String): PairingResult {
        paired += service to code
        return result.await()
    }
}

internal val WaitingPairingService = PairingService(name = "adb-R54T202XEHN-Y2yH0N", host = "172.30.1.47", port = 37123)

internal val RunningAndroidDevice = EmulatorDevice(
    id = "emulator-5554",
    name = "Pixel_9_API_37",
    platform = EmulatorPlatform.ANDROID,
    isRunning = true,
    canStream = true,
    canControl = true,
)

internal val StoppedAndroidDevice = EmulatorDevice(
    id = "avd:Pixel_Tablet_API_36",
    name = "Pixel_Tablet_API_36",
    platform = EmulatorPlatform.ANDROID,
    canLaunch = true,
)

// 연결됐지만 화면이 꺼진 실물 기기.
internal val SleepingAndroidDevice = EmulatorDevice(
    id = "39061FDJH00CNS",
    name = "Pixel 9 Pro",
    platform = EmulatorPlatform.ANDROID,
    isPhysical = true,
    isRunning = true,
    isAsleep = true,
    canStream = true,
    canControl = true,
)
