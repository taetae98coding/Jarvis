package io.github.taetae98coding.jarvis.ui.emulator

import io.github.taetae98coding.jarvis.domain.emulator.EmulatorDevice
import io.github.taetae98coding.jarvis.domain.emulator.EmulatorPlatform
import io.github.taetae98coding.jarvis.domain.emulator.ObserveEmulatorDevicesUseCase
import io.github.taetae98coding.jarvis.domain.emulator.ObserveEmulatorScreenUseCase
import io.github.taetae98coding.jarvis.domain.emulator.EmulatorGesture
import io.github.taetae98coding.jarvis.domain.emulator.SendEmulatorGestureUseCase
import io.github.taetae98coding.jarvis.domain.emulator.TouchAction
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

    // 뗀 뒤에도 끌린 것처럼 보이지 않으려면 DOWN·UP 이 순서를 지키고, 쌓인 MOVE 는 마지막만 가야 한다.
    @Test
    fun gesturesKeepOrderAndCoalesceMoves() {
        val emulator = FakeEmulatorRepository(devices = listOf(SleepingAndroidDevice))

        viewModelTest(emulator) { viewModel ->
            backgroundScope.launch { viewModel.device.collect() }
            runCurrent()

            viewModel.onGesture(touch(TouchAction.DOWN, 0, 0))
            viewModel.onGesture(touch(TouchAction.MOVE, 1, 1))
            viewModel.onGesture(touch(TouchAction.MOVE, 2, 2))
            viewModel.onGesture(touch(TouchAction.UP, 3, 3))
            runCurrent()

            assertEquals(
                listOf(
                    touch(TouchAction.DOWN, 0, 0),
                    touch(TouchAction.MOVE, 2, 2),
                    touch(TouchAction.UP, 3, 3),
                ),
                emulator.gestures.toList(),
            )
        }
    }

    private fun touch(action: TouchAction, x: Int, y: Int) =
        EmulatorGesture.Touch(action = action, x = x, y = y, frameWidth = 1080, frameHeight = 2400)

    @Test
    fun aStoppedAvdAliasFollowsTheEmulatorOnceItBoots() {
        val stopped = EmulatorDevice(id = "avd:Pixel_9", name = "Pixel_9", platform = EmulatorPlatform.ANDROID, canLaunch = true)
        val emulator = FakeEmulatorRepository(devices = listOf(stopped))

        viewModelTest(emulator, deviceId = "avd:Pixel_9") { viewModel ->
            backgroundScope.launch { viewModel.frames.collect() }
            runCurrent()

            emulator.devices.value = listOf(
                EmulatorDevice(id = "emulator-5556", name = "Pixel_9", platform = EmulatorPlatform.ANDROID, isRunning = true, canStream = true, canControl = true),
            )
            runCurrent()

            assertEquals(listOf("avd:Pixel_9", "emulator-5556"), emulator.screens.toList())
            assertEquals("emulator-5556", viewModel.device.value?.id)

            viewModel.onGesture(touch(TouchAction.DOWN, 1, 1))
            runCurrent()

            assertEquals(listOf("emulator-5556"), emulator.gestureTargets.toList())
        }
    }

    private fun viewModelTest(
        emulator: FakeEmulatorRepository,
        deviceId: String = SleepingAndroidDevice.id,
        body: suspend TestScope.(EmulatorScreenViewModel) -> Unit,
    ) = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))

        val viewModel = EmulatorScreenViewModel(
            deviceId = deviceId,
            observeEmulatorDevices = ObserveEmulatorDevicesUseCase(emulator),
            observeEmulatorScreen = ObserveEmulatorScreenUseCase(emulator),
            sendEmulatorGesture = SendEmulatorGestureUseCase(emulator),
            wakeDevice = WakeDeviceUseCase(emulator),
        )

        body(viewModel)
    }
}
