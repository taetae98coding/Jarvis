package io.github.taetae98coding.jarvis.data.rotation

import io.github.taetae98coding.jarvis.domain.rotation.DeviceRotationRepository
import io.github.taetae98coding.jarvis.domain.rotation.DeviceRotationStatus
import io.github.taetae98coding.jarvis.domain.rotation.RotationAngle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

internal class DefaultDeviceRotationRepository(
    private val dataSource: DeviceRotationDataSource,
    scope: CoroutineScope,
) : DeviceRotationRepository {
    // 구독자가 여럿이어도 플랫폼 리스너는 한 벌만 돈다.
    override val status: StateFlow<DeviceRotationStatus> =
        dataSource.observeStatus()
            .stateIn(scope, SharingStarted.WhileSubscribed(), dataSource.readStatus())

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
