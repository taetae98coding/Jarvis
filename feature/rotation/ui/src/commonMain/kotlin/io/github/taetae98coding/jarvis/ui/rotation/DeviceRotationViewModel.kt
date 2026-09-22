package io.github.taetae98coding.jarvis.ui.rotation

import androidx.lifecycle.ViewModel
import io.github.taetae98coding.jarvis.domain.rotation.DeviceRotationStatus
import io.github.taetae98coding.jarvis.domain.rotation.ObserveDeviceRotationStatusUseCase
import io.github.taetae98coding.jarvis.domain.rotation.RotateDeviceUseCase
import io.github.taetae98coding.jarvis.domain.rotation.RotationAngle
import io.github.taetae98coding.jarvis.domain.rotation.SetDeviceRotationAngleUseCase
import io.github.taetae98coding.jarvis.domain.rotation.SetDeviceRotationLockUseCase
import kotlinx.coroutines.flow.StateFlow

internal class DeviceRotationViewModel(
    observeDeviceRotationStatus: ObserveDeviceRotationStatusUseCase,
    private val setDeviceRotationAngle: SetDeviceRotationAngleUseCase,
    private val setDeviceRotationLock: SetDeviceRotationLockUseCase,
    private val rotateDevice: RotateDeviceUseCase,
) : ViewModel() {
    val deviceRotation: StateFlow<DeviceRotationStatus> = observeDeviceRotationStatus()

    fun onDeviceRotationAngleClick(angle: RotationAngle) {
        setDeviceRotationAngle(angle)
    }

    fun onDeviceRotate(steps: Int) {
        rotateDevice(steps)
    }

    fun onDeviceRotationLockChange(locked: Boolean) {
        setDeviceRotationLock(locked)
    }
}
