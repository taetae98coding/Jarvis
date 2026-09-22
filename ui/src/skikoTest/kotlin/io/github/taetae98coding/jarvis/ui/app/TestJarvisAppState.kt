package io.github.taetae98coding.jarvis.ui.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import io.github.taetae98coding.jarvis.domain.appinfo.AppInfo
import io.github.taetae98coding.jarvis.domain.appinfo.GetAppInfoUseCase
import io.github.taetae98coding.jarvis.domain.emulator.EmulatorDevice
import io.github.taetae98coding.jarvis.domain.emulator.EmulatorGesture
import io.github.taetae98coding.jarvis.domain.emulator.EmulatorRepository
import io.github.taetae98coding.jarvis.domain.emulator.EmulatorStatus
import io.github.taetae98coding.jarvis.domain.emulator.EmulatorSummary
import io.github.taetae98coding.jarvis.domain.emulator.ObserveEmulatorDevicesUseCase
import io.github.taetae98coding.jarvis.domain.emulator.ObserveEmulatorScreenUseCase
import io.github.taetae98coding.jarvis.domain.emulator.ObserveEmulatorStatusUseCase
import io.github.taetae98coding.jarvis.domain.emulator.SendEmulatorGestureUseCase
import io.github.taetae98coding.jarvis.domain.screen.ApplyKeepScreenAwakeUseCase
import io.github.taetae98coding.jarvis.domain.screen.ApplySystemScreenAwakeUseCase
import io.github.taetae98coding.jarvis.domain.screen.ObserveKeepScreenAwakeUseCase
import io.github.taetae98coding.jarvis.domain.screen.ObserveKeepSystemScreenAwakeUseCase
import io.github.taetae98coding.jarvis.domain.screen.ObserveSystemScreenAwakeStatusUseCase
import io.github.taetae98coding.jarvis.domain.screen.ScreenAwakeRepository
import io.github.taetae98coding.jarvis.domain.screen.ScreenAwakeSettingsRepository
import io.github.taetae98coding.jarvis.domain.screen.SetKeepScreenAwakeUseCase
import io.github.taetae98coding.jarvis.domain.screen.SetKeepSystemScreenAwakeUseCase
import io.github.taetae98coding.jarvis.domain.screen.SystemScreenAwakeRepository
import io.github.taetae98coding.jarvis.domain.screen.SystemScreenAwakeStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow

// 화면 테스트는 저장소도 플랫폼도 모른다. 도메인 인터페이스만 가짜로 끼우면 되는 것이 :ui 가
// :data 를 보지 않는다는 증거다.
internal val TestAppInfo = AppInfo(version = "1.2.3-test", platform = "Test Platform")

@Composable
internal fun rememberTestJarvisAppState(
    settings: ScreenAwakeSettingsRepository = FakeScreenAwakeSettingsRepository(),
    systemScreenAwake: SystemScreenAwakeRepository = FakeSystemScreenAwakeRepository(),
    emulator: EmulatorRepository = FakeEmulatorRepository(),
    screenAwake: ScreenAwakeRepository = ScreenAwakeRepository { },
    appInfo: AppInfo = TestAppInfo,
): JarvisAppState {
    val scope = rememberCoroutineScope()

    return remember(scope) {
        JarvisAppState(
            scope = scope,
            getAppInfo = GetAppInfoUseCase { appInfo },
            observeEmulatorStatus = ObserveEmulatorStatusUseCase(emulator),
            observeEmulatorDevices = ObserveEmulatorDevicesUseCase(emulator),
            observeEmulatorScreen = ObserveEmulatorScreenUseCase(emulator),
            sendEmulatorGesture = SendEmulatorGestureUseCase(emulator),
            observeKeepScreenAwake = ObserveKeepScreenAwakeUseCase(settings),
            observeKeepSystemScreenAwake = ObserveKeepSystemScreenAwakeUseCase(settings),
            observeSystemScreenAwakeStatus = ObserveSystemScreenAwakeStatusUseCase(systemScreenAwake),
            setKeepScreenAwake = SetKeepScreenAwakeUseCase(settings),
            setKeepSystemScreenAwake = SetKeepSystemScreenAwakeUseCase(settings, systemScreenAwake),
            applyKeepScreenAwake = ApplyKeepScreenAwakeUseCase(settings, screenAwake),
            applySystemScreenAwake = ApplySystemScreenAwakeUseCase(settings, systemScreenAwake),
        )
    }
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

    override fun observeStatus() = status

    override fun observeDevices() = devices

    override fun observeScreen(deviceId: String) = frames

    override suspend fun sendGesture(deviceId: String, gesture: EmulatorGesture) {
        gestures += deviceId to gesture
    }
}

// 아직 아무 답도 하지 않은 저장소. 화면은 "확인 중…" 과 빈 목록을 보여줘야 한다.
internal object SilentEmulatorRepository : EmulatorRepository {
    override fun observeStatus() = emptyFlow<EmulatorStatus>()

    override fun observeDevices() = emptyFlow<List<EmulatorDevice>>()

    override fun observeScreen(deviceId: String) = emptyFlow<ByteArray?>()

    override suspend fun sendGesture(deviceId: String, gesture: EmulatorGesture) = Unit
}
