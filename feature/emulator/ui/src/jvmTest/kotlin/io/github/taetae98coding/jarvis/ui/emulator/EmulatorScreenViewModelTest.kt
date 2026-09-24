package io.github.taetae98coding.jarvis.ui.emulator

import io.github.taetae98coding.jarvis.domain.emulator.ObserveEmulatorDevicesUseCase
import io.github.taetae98coding.jarvis.domain.emulator.ObserveEmulatorScreenUseCase
import io.github.taetae98coding.jarvis.domain.emulator.SendEmulatorGestureUseCase
import io.github.taetae98coding.jarvis.domain.emulator.WakeDeviceUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class EmulatorScreenViewModelTest {
    // 이유는 EmulatorDevicesViewModelTest.resetMainDispatcher 와 같다.
    @AfterTest
    fun resetMainDispatcher() {
        Dispatchers.resetMain()
    }

    @Test
    fun wakingStaysUntilTheListSaysTheScreenIsOn() {
        val emulator = FakeEmulatorRepository(devices = listOf(SleepingAndroidDevice))

        viewModelTest(emulator) { viewModel ->
            backgroundScope.launch { viewModel.isWaking.collect() }
            runCurrent()

            viewModel.onWake()
            runCurrent()

            assertTrue(viewModel.isWaking.value)
            assertEquals(listOf(SleepingAndroidDevice.id), emulator.woken.toList())

            emulator.devices.value = listOf(SleepingAndroidDevice.copy(isAsleep = false))
            runCurrent()

            assertFalse(viewModel.isWaking.value)
        }
    }

    // 켜지지 않는 기기에서 버튼이 영영 숨으면 다시 누를 방법이 없다.
    @Test
    fun wakingGivesUpWhenTheScreenStaysOff() {
        val emulator = FakeEmulatorRepository(devices = listOf(SleepingAndroidDevice))

        viewModelTest(emulator) { viewModel ->
            backgroundScope.launch { viewModel.isWaking.collect() }
            runCurrent()
            viewModel.onWake()
            runCurrent()

            advanceTimeBy(10.seconds + 1.seconds)
            runCurrent()

            assertFalse(viewModel.isWaking.value)
        }
    }

    @Test
    fun wakeIsNotSentAgainWhileWaking() {
        val emulator = FakeEmulatorRepository(devices = listOf(SleepingAndroidDevice))

        viewModelTest(emulator) { viewModel ->
            backgroundScope.launch { viewModel.device.collect() }
            runCurrent()

            viewModel.onWake()
            runCurrent()
            viewModel.onWake()
            runCurrent()

            assertEquals(listOf(SleepingAndroidDevice.id), emulator.woken.toList())
        }
    }

    private fun viewModelTest(
        emulator: FakeEmulatorRepository,
        body: suspend TestScope.(EmulatorScreenViewModel) -> Unit,
    ) = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))

        val viewModel = EmulatorScreenViewModel(
            deviceId = SleepingAndroidDevice.id,
            observeEmulatorDevices = ObserveEmulatorDevicesUseCase(emulator),
            observeEmulatorScreen = ObserveEmulatorScreenUseCase(emulator),
            sendEmulatorGesture = SendEmulatorGestureUseCase(emulator),
            wakeDevice = WakeDeviceUseCase(emulator),
        )

        body(viewModel)
    }
}
