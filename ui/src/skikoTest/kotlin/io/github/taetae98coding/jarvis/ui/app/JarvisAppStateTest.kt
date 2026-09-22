package io.github.taetae98coding.jarvis.ui.app

import io.github.taetae98coding.jarvis.domain.appinfo.GetAppInfoUseCase
import io.github.taetae98coding.jarvis.domain.emulator.EmulatorRepository
import io.github.taetae98coding.jarvis.domain.emulator.LaunchEmulatorUseCase
import io.github.taetae98coding.jarvis.domain.emulator.ObserveEmulatorDevicesUseCase
import io.github.taetae98coding.jarvis.domain.emulator.ObserveEmulatorScreenUseCase
import io.github.taetae98coding.jarvis.domain.emulator.ObserveEmulatorStatusUseCase
import io.github.taetae98coding.jarvis.domain.emulator.SendEmulatorGestureUseCase
import io.github.taetae98coding.jarvis.domain.emulator.WakeDeviceUseCase
import io.github.taetae98coding.jarvis.domain.rotation.ObserveDeviceRotationStatusUseCase
import io.github.taetae98coding.jarvis.domain.rotation.RotateDeviceUseCase
import io.github.taetae98coding.jarvis.domain.rotation.SetDeviceRotationAngleUseCase
import io.github.taetae98coding.jarvis.domain.rotation.SetDeviceRotationLockUseCase
import io.github.taetae98coding.jarvis.domain.screen.ApplyKeepScreenAwakeUseCase
import io.github.taetae98coding.jarvis.domain.screen.ApplySystemScreenAwakeUseCase
import io.github.taetae98coding.jarvis.domain.screen.ObserveKeepScreenAwakeUseCase
import io.github.taetae98coding.jarvis.domain.screen.ObserveKeepSystemScreenAwakeUseCase
import io.github.taetae98coding.jarvis.domain.screen.ObserveSystemScreenAwakeStatusUseCase
import io.github.taetae98coding.jarvis.domain.screen.ScreenAwakeRepository
import io.github.taetae98coding.jarvis.domain.screen.SetKeepScreenAwakeUseCase
import io.github.taetae98coding.jarvis.domain.screen.SetKeepSystemScreenAwakeUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * 화면을 그리지 않고 상태만 보는 테스트. 실행 잠금이 풀리는 규칙은 렌더링과 무관한 상태 규칙이고,
 * Compose UI 테스트로 확인하면 Wasm 에서 Flow 가 화면까지 전파되기를 기다리는 일이 섞여 들어온다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class JarvisAppStateTest {
    @Test
    fun launchLocksTheDeviceUntilItShowsUp() = runTest {
        val emulator = FakeEmulatorRepository(devices = listOf(StoppedAndroidDevice))
        val state = appState(backgroundScope, emulator)
        // WhileSubscribed 라 구독자가 있어야 값이 흐른다. 화면이 하는 일을 대신한다.
        backgroundScope.launch { state.launchingDevices.collect() }
        runCurrent()

        state.onEmulatorDeviceLaunch(StoppedAndroidDevice)
        runCurrent()

        assertEquals(setOf(StoppedAndroidDevice.id), state.launchingDevices.value)
        assertEquals(listOf(StoppedAndroidDevice.id), emulator.launched.toList())
    }

    // 켜진 AVD 는 목록에서 시리얼로 바뀐다. 그 id 가 꺼진 채로 남아 있지 않으면 잠금이 풀려야 한다.
    @Test
    fun launchLockClearsWhenTheDeviceStarts() = runTest {
        val emulator = FakeEmulatorRepository(devices = listOf(StoppedAndroidDevice))
        val state = appState(backgroundScope, emulator)
        backgroundScope.launch { state.launchingDevices.collect() }
        runCurrent()
        state.onEmulatorDeviceLaunch(StoppedAndroidDevice)
        runCurrent()

        emulator.devices.value = listOf(RunningAndroidDevice)
        runCurrent()

        assertEquals(emptySet(), state.launchingDevices.value)
    }

    // 같은 기기에 두 번 보내면 두 번째 `emulator -avd` 는 "이미 실행 중" 으로 끝난다.
    @Test
    fun launchIsNotSentTwiceForTheSameDevice() = runTest {
        val emulator = FakeEmulatorRepository(devices = listOf(StoppedAndroidDevice))
        val state = appState(backgroundScope, emulator)
        backgroundScope.launch { state.launchingDevices.collect() }
        runCurrent()

        state.onEmulatorDeviceLaunch(StoppedAndroidDevice)
        runCurrent()
        state.onEmulatorDeviceLaunch(StoppedAndroidDevice)
        runCurrent()

        assertEquals(listOf(StoppedAndroidDevice.id), emulator.launched.toList())
    }

    @Test
    fun wakingASleepingDeviceReachesTheRepository() = runTest {
        val emulator = FakeEmulatorRepository(devices = listOf(SleepingAndroidDevice))
        val state = appState(backgroundScope, emulator)

        state.onEmulatorDeviceWake(SleepingAndroidDevice)
        runCurrent()

        assertEquals(listOf(SleepingAndroidDevice.id), emulator.woken.toList())
    }

    private fun TestScope.appState(scope: CoroutineScope, emulator: EmulatorRepository): JarvisAppState {
        val settings = FakeScreenAwakeSettingsRepository()
        val systemScreenAwake = FakeSystemScreenAwakeRepository()
        val rotation = FakeDeviceRotationRepository()
        val setAngle = SetDeviceRotationAngleUseCase(rotation)

        return JarvisAppState(
            scope = scope,
            getAppInfo = GetAppInfoUseCase { TestAppInfo },
            observeEmulatorStatus = ObserveEmulatorStatusUseCase(emulator),
            observeEmulatorDevices = ObserveEmulatorDevicesUseCase(emulator),
            observeEmulatorScreen = ObserveEmulatorScreenUseCase(emulator),
            sendEmulatorGesture = SendEmulatorGestureUseCase(emulator),
            launchEmulator = LaunchEmulatorUseCase(emulator),
            wakeDevice = WakeDeviceUseCase(emulator),
            observeKeepScreenAwake = ObserveKeepScreenAwakeUseCase(settings),
            observeKeepSystemScreenAwake = ObserveKeepSystemScreenAwakeUseCase(settings),
            observeSystemScreenAwakeStatus = ObserveSystemScreenAwakeStatusUseCase(systemScreenAwake),
            observeDeviceRotationStatus = ObserveDeviceRotationStatusUseCase(rotation),
            setKeepScreenAwake = SetKeepScreenAwakeUseCase(settings),
            setKeepSystemScreenAwake = SetKeepSystemScreenAwakeUseCase(settings, systemScreenAwake),
            applyKeepScreenAwake = ApplyKeepScreenAwakeUseCase(settings, ScreenAwakeRepository { }),
            applySystemScreenAwake = ApplySystemScreenAwakeUseCase(settings, systemScreenAwake),
            setDeviceRotationAngle = setAngle,
            setDeviceRotationLock = SetDeviceRotationLockUseCase(rotation),
            rotateDevice = RotateDeviceUseCase(rotation, setAngle),
        )
    }
}
