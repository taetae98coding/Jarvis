package io.github.taetae98coding.jarvis.domain.rotation

class SetDeviceRotationLockUseCase(
    private val deviceRotation: DeviceRotationRepository,
) {
    operator fun invoke(locked: Boolean) {
        if (!deviceRotation.status.value.permitted) {
            deviceRotation.requestPermission()

            return
        }

        deviceRotation.setLocked(locked)
    }
}
