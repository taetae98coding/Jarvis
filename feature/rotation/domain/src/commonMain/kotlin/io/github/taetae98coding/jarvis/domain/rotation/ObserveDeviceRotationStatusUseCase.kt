package io.github.taetae98coding.jarvis.domain.rotation

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class ObserveDeviceRotationStatusUseCase(
    private val deviceRotation: DeviceRotationRepository,
) {
    operator fun invoke(scope: CoroutineScope): StateFlow<DeviceRotationStatus> =
        deviceRotation.observeStatus()
            .stateIn(scope, SharingStarted.WhileSubscribed(), deviceRotation.readStatus())
}
