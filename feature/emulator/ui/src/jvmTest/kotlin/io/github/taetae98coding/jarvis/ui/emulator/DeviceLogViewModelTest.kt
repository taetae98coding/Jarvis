package io.github.taetae98coding.jarvis.ui.emulator

import io.github.taetae98coding.jarvis.domain.emulator.DeviceLogRepository
import io.github.taetae98coding.jarvis.domain.emulator.EmulatorDevice
import io.github.taetae98coding.jarvis.domain.emulator.EmulatorPlatform
import io.github.taetae98coding.jarvis.domain.emulator.ObserveDeviceLogUseCase
import io.github.taetae98coding.jarvis.domain.emulator.ObserveEmulatorDevicesUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onStart
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
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** docs/common/device-logcat.html */
@OptIn(ExperimentalCoroutinesApi::class)
class DeviceLogViewModelTest {
    private class FakeDeviceLogRepository : DeviceLogRepository {
        val logs = HashMap<String, MutableSharedFlow<List<String>>>()

        /** 로그 수집을 시작한 기기 id. 순서대로다. */
        val observed = mutableListOf<String>()

        fun of(deviceId: String) = logs.getOrPut(deviceId) { MutableSharedFlow() }

        override fun observeLog(deviceId: String): Flow<List<String>> = of(deviceId).onStart { observed += deviceId }
    }

    // 이유는 EmulatorDevicesViewModelTest.resetMainDispatcher 와 같다.
    @AfterTest
    fun resetMainDispatcher() {
        Dispatchers.resetMain()
    }

    @Test
    fun linesArriveWithTheirLevel() {
        val logs = FakeDeviceLogRepository()

        viewModelTest(logs) { viewModel ->
            assertFalse(viewModel.state.value.hasReceived)

            logs.of(RunningAndroidDevice.id).emit(listOf("09-25 17:18:51.832  1850  2652 E tag: boom", "--------- beginning of main"))
            runCurrent()

            val state = viewModel.state.value
            assertTrue(state.hasReceived)
            assertEquals(listOf("09-25 17:18:51.832  1850  2652 E tag: boom", "--------- beginning of main"), state.lines.map { it.line.text })
        }
    }

    // R7
    @Test
    fun filterKeepsOnlyMatchingLinesIgnoringCase() {
        val logs = FakeDeviceLogRepository()

        viewModelTest(logs) { viewModel ->
            logs.of(RunningAndroidDevice.id).emit(listOf("ActivityManager: Start", "Choreographer: skipped", "activitymanager: stop"))
            viewModel.setFilter("ACTIVITY")
            runCurrent()

            assertEquals(listOf("ActivityManager: Start", "activitymanager: stop"), viewModel.state.value.lines.map { it.line.text })

            viewModel.setFilter("nothing")
            runCurrent()
            assertTrue(viewModel.state.value.hasReceived)
            assertTrue(viewModel.state.value.lines.isEmpty())
        }
    }

    // R7
    @Test
    fun clearHidesWhatCameBeforeIt() {
        val logs = FakeDeviceLogRepository()

        viewModelTest(logs) { viewModel ->
            logs.of(RunningAndroidDevice.id).emit(listOf("a", "b"))
            runCurrent()

            viewModel.clear()
            runCurrent()
            assertFalse(viewModel.state.value.hasReceived)

            logs.of(RunningAndroidDevice.id).emit(listOf("c"))
            runCurrent()
            assertEquals(listOf("c"), viewModel.state.value.lines.map { it.line.text })
        }
    }

    // R8
    @Test
    fun keepsOnlyTheNewestFiveThousandLines() {
        val logs = FakeDeviceLogRepository()

        viewModelTest(logs) { viewModel ->
            logs.of(RunningAndroidDevice.id).emit((1..4000).map { "line $it" })
            logs.of(RunningAndroidDevice.id).emit((4001..6000).map { "line $it" })
            runCurrent()

            val lines = viewModel.state.value.lines
            assertEquals(5000, lines.size)
            assertEquals("line 1001", lines.first().line.text)
            assertEquals("line 6000", lines.last().line.text)
        }
    }

    // R10
    @Test
    fun physicalIosCannotShowLogs() {
        val iphone = EmulatorDevice(id = "ios:00008130", name = "iPhone", platform = EmulatorPlatform.IOS, isPhysical = true, isRunning = true)

        viewModelTest(FakeDeviceLogRepository(), emulator = FakeEmulatorRepository(devices = listOf(iphone)), deviceId = iphone.id) { viewModel ->
            assertFalse(viewModel.state.value.isSupported)
        }
    }

    // R12
    @Test
    fun aStoppedAvdAliasFollowsTheEmulatorOnceItBoots() {
        val stopped = EmulatorDevice(id = "avd:Pixel_9", name = "Pixel_9", platform = EmulatorPlatform.ANDROID, canLaunch = true)
        val emulator = FakeEmulatorRepository(devices = listOf(stopped))
        val logs = FakeDeviceLogRepository()

        viewModelTest(logs, emulator = emulator, deviceId = stopped.id) { viewModel ->
            emulator.devices.value = listOf(RunningAndroidDevice.copy(id = "emulator-5556", name = "Pixel_9"))
            runCurrent()

            logs.of("emulator-5556").emit(listOf("booted"))
            runCurrent()

            assertEquals(listOf("avd:Pixel_9", "emulator-5556"), logs.observed.toList())
            assertEquals(listOf("booted"), viewModel.state.value.lines.map { it.line.text })
        }
    }

    private fun viewModelTest(
        logs: FakeDeviceLogRepository,
        emulator: FakeEmulatorRepository = FakeEmulatorRepository(devices = listOf(RunningAndroidDevice)),
        deviceId: String = RunningAndroidDevice.id,
        body: suspend TestScope.(DeviceLogViewModel) -> Unit,
    ) = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))

        val viewModel = DeviceLogViewModel(
            deviceId = deviceId,
            observeEmulatorDevices = ObserveEmulatorDevicesUseCase(emulator),
            observeDeviceLog = ObserveDeviceLogUseCase(logs),
        )
        backgroundScope.launch { viewModel.state.collect() }
        runCurrent()

        body(viewModel)
    }
}
