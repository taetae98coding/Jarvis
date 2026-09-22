package io.github.taetae98coding.jarvis.domain.rotation

import kotlinx.coroutines.flow.StateFlow

class ObserveDeviceRotationStatusUseCase(
    private val deviceRotation: DeviceRotationRepository,
) {
    operator fun invoke(): StateFlow<DeviceRotationStatus> = deviceRotation.status
}
