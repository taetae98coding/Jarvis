package io.github.taetae98coding.jarvis.domain.rotation

import kotlinx.coroutines.flow.first

class SetDeviceRotationLockUseCase(
    private val deviceRotation: DeviceRotationRepository,
) {
    suspend operator fun invoke(locked: Boolean) {
        if (!deviceRotation.observeStatus().first().permitted) {
            deviceRotation.requestPermission()

            return
        }

        deviceRotation.setLocked(locked)
    }
}
