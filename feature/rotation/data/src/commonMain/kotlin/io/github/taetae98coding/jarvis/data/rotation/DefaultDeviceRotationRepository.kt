package io.github.taetae98coding.jarvis.data.rotation

import io.github.taetae98coding.jarvis.domain.rotation.DeviceRotationRepository
import io.github.taetae98coding.jarvis.domain.rotation.DeviceRotationStatus
import io.github.taetae98coding.jarvis.domain.rotation.RotationAngle
import kotlinx.coroutines.flow.Flow

internal class DefaultDeviceRotationRepository(
    private val dataSource: DeviceRotationDataSource,
) : DeviceRotationRepository {
    override fun observeStatus(): Flow<DeviceRotationStatus> = dataSource.observeStatus()

    override fun readStatus(): DeviceRotationStatus = dataSource.readStatus()

    override fun setAngle(angle: RotationAngle) {
        dataSource.setAngle(angle)
    }

    override fun setLocked(locked: Boolean) {
        dataSource.setLocked(locked)
    }

    override fun requestPermission() {
        dataSource.requestPermission()
    }
}
