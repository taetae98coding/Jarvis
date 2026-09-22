package io.github.taetae98coding.jarvis.domain.emulator

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class LaunchEmulatorUseCaseTest {
    @Test
    fun launchesADeviceThatCanBeLaunched() = runTest {
        val repository = RecordingEmulatorRepository()

        LaunchEmulatorUseCase(repository)(StoppedAvd)

        assertEquals(listOf(StoppedAvd.id), repository.launched)
    }

    // 목록이 5초마다 갱신되므로, 화면이 막기 전에 들어온 요청이 남아 있을 수 있다.
    @Test
    fun ignoresDevicesThatCannotBeLaunched() = runTest {
        val repository = RecordingEmulatorRepository()

        LaunchEmulatorUseCase(repository)(StoppedAvd.copy(canLaunch = false))

        assertEquals(emptyList(), repository.launched)
    }

    private class RecordingEmulatorRepository : EmulatorRepository {
        val launched = mutableListOf<String>()

        override fun observeStatus(): Flow<EmulatorStatus> = emptyFlow()

        override fun observeDevices(): Flow<List<EmulatorDevice>> = emptyFlow()

        override fun observeScreen(deviceId: String): Flow<ByteArray?> = emptyFlow()

        override suspend fun sendGesture(deviceId: String, gesture: EmulatorGesture) = Unit

        override suspend fun launch(deviceId: String) {
            launched += deviceId
        }

        override suspend fun wake(deviceId: String) = Unit
    }

    private companion object {
        val StoppedAvd = EmulatorDevice(
            id = "avd:Pixel_9_API_37",
            name = "Pixel_9_API_37",
            platform = EmulatorPlatform.ANDROID,
            canLaunch = true,
        )
    }
}
