package io.github.taetae98coding.jarvis.data.emulator

import io.github.taetae98coding.jarvis.domain.emulator.EmulatorDevice
import io.github.taetae98coding.jarvis.domain.emulator.EmulatorGesture
import io.github.taetae98coding.jarvis.domain.emulator.EmulatorRepository
import io.github.taetae98coding.jarvis.domain.emulator.EmulatorStatus
import kotlinx.coroutines.flow.Flow

internal class DefaultEmulatorRepository(
    private val dataSource: EmulatorDataSource,
) : EmulatorRepository {
    override fun observeStatus(): Flow<EmulatorStatus> = dataSource.observeStatus()

    override fun observeDevices(): Flow<List<EmulatorDevice>> = dataSource.observeDevices()

    override fun observeScreen(deviceId: String): Flow<ByteArray?> = dataSource.observeScreen(deviceId)

    override suspend fun sendGesture(deviceId: String, gesture: EmulatorGesture) {
        dataSource.sendGesture(deviceId, gesture)
    }
}
