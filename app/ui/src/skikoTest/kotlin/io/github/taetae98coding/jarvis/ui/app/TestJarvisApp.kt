package io.github.taetae98coding.jarvis.ui.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import io.github.taetae98coding.jarvis.domain.appinfo.AppInfo
import io.github.taetae98coding.jarvis.domain.appinfo.AppInfoRepository
import io.github.taetae98coding.jarvis.domain.emulator.DevicePairingRepository
import io.github.taetae98coding.jarvis.domain.emulator.EmulatorDevice
import io.github.taetae98coding.jarvis.domain.emulator.EmulatorGesture
import io.github.taetae98coding.jarvis.domain.emulator.EmulatorRepository
import io.github.taetae98coding.jarvis.domain.emulator.EmulatorStatus
import io.github.taetae98coding.jarvis.domain.emulator.EmulatorSummary
import io.github.taetae98coding.jarvis.domain.emulator.PairingResult
import io.github.taetae98coding.jarvis.domain.emulator.PairingService
import io.github.taetae98coding.jarvis.domain.rotation.DeviceRotationRepository
import io.github.taetae98coding.jarvis.domain.rotation.DeviceRotationStatus
import io.github.taetae98coding.jarvis.domain.rotation.RotationAngle
import io.github.taetae98coding.jarvis.domain.screen.ScreenAwakeRepository
import io.github.taetae98coding.jarvis.domain.screen.ScreenAwakeSettingsRepository
import io.github.taetae98coding.jarvis.domain.screen.SystemScreenAwakeRepository
import io.github.taetae98coding.jarvis.domain.screen.SystemScreenAwakeStatus
import io.github.taetae98coding.jarvis.domain.appinfo.appInfoDomainModule
import io.github.taetae98coding.jarvis.domain.emulator.emulatorDomainModule
import io.github.taetae98coding.jarvis.domain.rotation.rotationDomainModule
import io.github.taetae98coding.jarvis.domain.screen.screenDomainModule
import io.github.taetae98coding.jarvis.domain.terminal.TerminalRepository
import io.github.taetae98coding.jarvis.domain.terminal.TerminalSession
import io.github.taetae98coding.jarvis.domain.terminal.TerminalSize
import io.github.taetae98coding.jarvis.domain.terminal.terminalDomainModule
import io.github.taetae98coding.jarvis.ui.appUiModule
import io.github.taetae98coding.jarvis.ui.appinfo.appInfoUiModule
import io.github.taetae98coding.jarvis.ui.emulator.emulatorUiModule
import io.github.taetae98coding.jarvis.ui.rotation.rotationUiModule
import io.github.taetae98coding.jarvis.ui.screen.screenUiModule
import io.github.taetae98coding.jarvis.ui.terminal.terminalUiModule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.emptyFlow
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.mp.KoinPlatformTools

// 화면 테스트는 저장소도 플랫폼도 모른다. 도메인 인터페이스만 가짜로 끼우면 되는 것이 :ui 가
// :data 를 보지 않는다는 증거다.
internal val TestAppInfo = AppInfo(version = "1.2.3-test", platform = "Test Platform")

/**
 * 가짜 리포지토리만 담은 Koin 으로 앱 전체를 띄운다.
 *
 * 기능들의 진짜 domain·ui Koin 모듈을 그대로 쓰고 data 모듈만 가짜로 바꿔 끼운다. 손으로 조립하지
 * 않으므로 조립 자체도 이 테스트가 검증한다.
 *
 * 프로덕션과 같은 전역 Koin 을 쓴다. `KoinApplication` 컴포저블로 격리하려 했지만, 그 함수는 전역
 * Koin 이 이미 있으면 새 인스턴스를 만들지 않고 전역 것을 그대로 쓴다(`rememberKoinApplication`).
 * 그러면 먼저 실행된 테스트의 가짜 저장소가 남아 다음 테스트로 새어 든다. 대신 테스트마다 전역
 * Koin 을 세우고, 이전 테스트가 남긴 것이 있으면 먼저 치운다.
 */
@Composable
internal fun TestJarvisApp(
    settings: ScreenAwakeSettingsRepository = FakeScreenAwakeSettingsRepository(),
    systemScreenAwake: SystemScreenAwakeRepository = FakeSystemScreenAwakeRepository(),
    emulator: EmulatorRepository = FakeEmulatorRepository(),
    pairing: DevicePairingRepository = FakeDevicePairingRepository(),
    deviceRotation: DeviceRotationRepository = FakeDeviceRotationRepository(),
    screenAwake: ScreenAwakeRepository = ScreenAwakeRepository { },
    terminal: TerminalRepository = FakeTerminalRepository(),
    appInfo: AppInfo = TestAppInfo,
) {
    // 앱 수명 스코프. 프로덕션에서는 진입점이 만들고 platformModule 이 등록한다.
    val scope = rememberCoroutineScope()

    remember(scope) {
        val fakes = module {
            single<CoroutineScope> { scope }
            single<AppInfoRepository> { AppInfoRepository { appInfo } }
            single<EmulatorRepository> { emulator }
            single<DevicePairingRepository> { pairing }
            single<ScreenAwakeSettingsRepository> { settings }
            single<ScreenAwakeRepository> { screenAwake }
            single<SystemScreenAwakeRepository> { systemScreenAwake }
            single<DeviceRotationRepository> { deviceRotation }
            single<TerminalRepository> { terminal }
        }

        if (KoinPlatformTools.defaultContext().getOrNull() != null) {
            stopKoin()
        }

        startKoin {
            modules(
                fakes,
                appInfoDomainModule, appInfoUiModule,
                emulatorDomainModule, emulatorUiModule,
                screenDomainModule, screenUiModule,
                rotationDomainModule, rotationUiModule,
                terminalDomainModule, terminalUiModule,
                appUiModule,
            )
        }
    }

    JarvisApp()
}

internal class FakeScreenAwakeSettingsRepository(
    keepScreenAwake: Boolean = false,
    keepSystemScreenAwake: Boolean = false,
) : ScreenAwakeSettingsRepository {
    override val keepScreenAwake = MutableStateFlow(keepScreenAwake)
    override val keepSystemScreenAwake = MutableStateFlow(keepSystemScreenAwake)

    override fun setKeepScreenAwake(value: Boolean) {
        this.keepScreenAwake.value = value
    }

    override fun setKeepSystemScreenAwake(value: Boolean) {
        this.keepSystemScreenAwake.value = value
    }
}

// 기본값은 Skiko 로 렌더링하는 세 타깃의 실제 상태와 같다. 셋 다 시스템 전역 화면 유지를 지원하지 않는다.
internal class FakeSystemScreenAwakeRepository(
    initial: SystemScreenAwakeStatus = SystemScreenAwakeStatus(),
) : SystemScreenAwakeRepository {
    override val status = MutableStateFlow(initial)

    override fun setEnabled(enabled: Boolean) = Unit

    override fun requestPermission() = Unit
}

// 기본값은 "셀 수 없음" 과 빈 목록이다. 개수를 세지 못하는 타깃이 답하는 값과 같다.
internal class FakeEmulatorRepository(
    android: EmulatorSummary? = null,
    ios: EmulatorSummary? = null,
    devices: List<EmulatorDevice> = emptyList(),
    private val frames: Flow<ByteArray?> = emptyFlow(),
) : EmulatorRepository {
    val status = MutableStateFlow(EmulatorStatus(android = android, ios = ios))

    val devices = MutableStateFlow(devices)

    val gestures = mutableListOf<Pair<String, EmulatorGesture>>()

    val launched = mutableListOf<String>()

    val woken = mutableListOf<String>()

    override fun observeStatus() = status

    override fun observeDevices() = devices

    override fun observeScreen(deviceId: String) = frames

    override suspend fun sendGesture(deviceId: String, gesture: EmulatorGesture) {
        gestures += deviceId to gesture
    }

    override suspend fun launch(deviceId: String) {
        launched += deviceId
    }

    override suspend fun wake(deviceId: String) {
        woken += deviceId
    }
}

// 아직 아무 답도 하지 않은 저장소. 화면은 "확인 중…" 과 빈 목록을 보여줘야 한다.
internal object SilentEmulatorRepository : EmulatorRepository {
    override fun observeStatus() = emptyFlow<EmulatorStatus>()

    override fun observeDevices() = emptyFlow<List<EmulatorDevice>>()

    override fun observeScreen(deviceId: String) = emptyFlow<ByteArray?>()

    override suspend fun sendGesture(deviceId: String, gesture: EmulatorGesture) = Unit

    override suspend fun launch(deviceId: String) = Unit

    override suspend fun wake(deviceId: String) = Unit
}

// 기본값은 페어링을 기다리는 기기가 없는 개발자 머신이다.
internal class FakeDevicePairingRepository(
    services: List<PairingService>? = emptyList(),
    private val result: PairingResult = PairingResult.Paired(isConnected = true),
) : DevicePairingRepository {
    val services = MutableStateFlow(services)

    val paired = mutableListOf<Pair<PairingService, String>>()

    override fun observePairingServices() = services

    override suspend fun pair(service: PairingService, code: String): PairingResult {
        paired += service to code
        return result
    }
}

// 기본값은 Skiko 로 렌더링하는 세 타깃 중 JVM 의 실제 상태와 같다. 돌릴 화면이 없다.
internal class FakeDeviceRotationRepository(
    initial: DeviceRotationStatus = DeviceRotationStatus(),
) : DeviceRotationRepository {
    override val status = MutableStateFlow(initial)

    override fun setAngle(angle: RotationAngle) {
        status.value = status.value.copy(angle = angle)
    }

    override fun setLocked(locked: Boolean) {
        status.value = status.value.copy(locked = locked)
    }

    override fun requestPermission() = Unit
}

// 셸 대신 테스트가 출력을 흘려 넣고 종료를 정한다. 기본값은 JVM·Android 처럼 셸을 띄울 수 있는 타깃이다.
internal class FakeTerminalRepository(
    override val isSupported: Boolean = true,
) : TerminalRepository {
    val sessions = mutableListOf<FakeTerminalSession>()

    override suspend fun open(size: TerminalSize): TerminalSession =
        FakeTerminalSession(size).also { sessions += it }
}

internal class FakeTerminalSession(
    var size: TerminalSize,
) : TerminalSession {
    private val channel = Channel<ByteArray>(Channel.UNLIMITED)

    val written = mutableListOf<ByteArray>()

    var closed = false
        private set

    override val isPty: Boolean = true

    override val output: Flow<ByteArray> = channel.consumeAsFlow()

    fun emit(text: String) {
        channel.trySend(text.encodeToByteArray())
    }

    /** 셸이 스스로 끝난 것처럼 출력을 닫는다. */
    fun exit() {
        channel.close()
    }

    override suspend fun write(bytes: ByteArray) {
        written += bytes
    }

    override fun resize(size: TerminalSize) {
        this.size = size
    }

    override fun close() {
        closed = true
        channel.close()
    }
}
