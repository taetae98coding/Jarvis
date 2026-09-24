package io.github.taetae98coding.jarvis.domain.emulator

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class WakeDeviceUseCaseTest {
    @Test
    fun wakesADeviceThatTakesInput() = runTest {
        val repository = RecordingEmulatorRepository()

        WakeDeviceUseCase(repository)(SleepingDevice)

        assertEquals(listOf(SleepingDevice.id), repository.woken)
    }

    // 목록이 5초마다 갱신되므로, 화면이 막기 전에 들어온 요청이 남아 있을 수 있다.
    @Test
    fun ignoresDevicesThatTakeNoInput() = runTest {
        val repository = RecordingEmulatorRepository()

        WakeDeviceUseCase(repository)(SleepingDevice.copy(canControl = false))

        assertEquals(emptyList(), repository.woken)
    }

    private class RecordingEmulatorRepository : EmulatorRepository {
        val woken = mutableListOf<String>()

        override fun observeStatus(): Flow<EmulatorStatus> = emptyFlow()

        override fun observeDevices(): Flow<List<EmulatorDevice>> = emptyFlow()

        override fun observeScreen(deviceId: String): Flow<EmulatorFrame?> = emptyFlow()

        override suspend fun sendGesture(deviceId: String, gesture: EmulatorGesture) = Unit

        override suspend fun launch(deviceId: String) = Unit

        override suspend fun wake(deviceId: String) {
            woken += deviceId
        }
    }

    private companion object {
        val SleepingDevice = EmulatorDevice(
            id = "39061FDJH00CNS",
            name = "Pixel 9 Pro",
            platform = EmulatorPlatform.ANDROID,
            isPhysical = true,
            isRunning = true,
            isAsleep = true,
            canStream = true,
            canControl = true,
        )
    }
}
