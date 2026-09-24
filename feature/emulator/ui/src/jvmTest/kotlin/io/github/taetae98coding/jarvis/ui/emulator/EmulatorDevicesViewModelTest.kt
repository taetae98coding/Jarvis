package io.github.taetae98coding.jarvis.ui.emulator

import io.github.taetae98coding.jarvis.domain.emulator.LaunchEmulatorUseCase
import io.github.taetae98coding.jarvis.domain.emulator.ObserveEmulatorDevicesUseCase
import io.github.taetae98coding.jarvis.domain.emulator.WakeDeviceUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * 화면을 그리지 않고 상태만 보는 테스트.
 *
 * 이 기능 모듈의 ViewModel 테스트는 `jvmTest` 에만 있다. `viewModelScope` 가 `Dispatchers.Main` 을 쓰고,
 * 그것을 테스트 디스패처로 갈아끼우는 `Dispatchers.setMain` 을 Wasm 에서는 쓸 수 없다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class EmulatorDevicesViewModelTest {
    /**
     * `runTest` 안에서 되돌리면 안 된다. `viewModelScope` 에 남은 코루틴(구독이 끊긴 `stateIn`,
     * 실행 잠금 타임아웃)은 `runTest` 가 끝을 정리하는 단계에서 한 번 더 깨어나고, 그때까지
     * `Dispatchers.Main` 이 테스트 디스패처여야 한다.
     */
    @AfterTest
    fun resetMainDispatcher() {
        Dispatchers.resetMain()
    }

    @Test
    fun launchLocksTheDeviceUntilItShowsUp() {
        val emulator = FakeEmulatorRepository(devices = listOf(StoppedAndroidDevice))

        viewModelTest(emulator) { viewModel ->
            // WhileSubscribed 라 구독자가 있어야 값이 흐른다. 화면이 하는 일을 대신한다.
            backgroundScope.launch { viewModel.launchingDevices.collect() }
            runCurrent()

            viewModel.onLaunch(StoppedAndroidDevice)
            runCurrent()

            assertEquals(setOf(StoppedAndroidDevice.id), viewModel.launchingDevices.value)
            assertEquals(listOf(StoppedAndroidDevice.id), emulator.launched.toList())
        }
    }

    // 켜진 AVD 는 목록에서 시리얼로 바뀐다. 그 id 가 꺼진 채로 남아 있지 않으면 잠금이 풀려야 한다.
    @Test
    fun launchLockClearsWhenTheDeviceStarts() {
        val emulator = FakeEmulatorRepository(devices = listOf(StoppedAndroidDevice))

        viewModelTest(emulator) { viewModel ->
            backgroundScope.launch { viewModel.launchingDevices.collect() }
            runCurrent()
            viewModel.onLaunch(StoppedAndroidDevice)
            runCurrent()

            emulator.devices.value = listOf(RunningAndroidDevice)
            runCurrent()

            assertEquals(emptySet(), viewModel.launchingDevices.value)
        }
    }

    // 같은 기기에 두 번 보내면 두 번째 `emulator -avd` 는 "이미 실행 중" 으로 끝난다.
    @Test
    fun launchIsNotSentTwiceForTheSameDevice() {
        val emulator = FakeEmulatorRepository(devices = listOf(StoppedAndroidDevice))

        viewModelTest(emulator) { viewModel ->
            backgroundScope.launch { viewModel.launchingDevices.collect() }
            runCurrent()

            viewModel.onLaunch(StoppedAndroidDevice)
            runCurrent()
            viewModel.onLaunch(StoppedAndroidDevice)
            runCurrent()

            assertEquals(listOf(StoppedAndroidDevice.id), emulator.launched.toList())
        }
    }

    @Test
    fun wakingASleepingDeviceReachesTheRepository() {
        val emulator = FakeEmulatorRepository(devices = listOf(SleepingAndroidDevice))

        viewModelTest(emulator) { viewModel ->
            viewModel.onWake(SleepingAndroidDevice)
            runCurrent()

            assertEquals(listOf(SleepingAndroidDevice.id), emulator.woken.toList())
        }
    }

    /** `viewModelScope` 가 테스트 스케줄러를 쓰게 만든다. 같은 스케줄러라야 `runCurrent` 로 몰 수 있다. */
    private fun viewModelTest(
        emulator: FakeEmulatorRepository,
        body: suspend TestScope.(EmulatorDevicesViewModel) -> Unit,
    ) = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))

        val viewModel = EmulatorDevicesViewModel(
            observeEmulatorDevices = ObserveEmulatorDevicesUseCase(emulator),
            launchEmulator = LaunchEmulatorUseCase(emulator),
            wakeDevice = WakeDeviceUseCase(emulator),
        )

        body(viewModel)
    }
}
