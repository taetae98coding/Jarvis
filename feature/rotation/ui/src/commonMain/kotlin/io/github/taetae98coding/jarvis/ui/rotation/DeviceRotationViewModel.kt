package io.github.taetae98coding.jarvis.ui.rotation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.taetae98coding.jarvis.domain.rotation.DeviceRotationNotificationStatus
import io.github.taetae98coding.jarvis.domain.rotation.DeviceRotationStatus
import io.github.taetae98coding.jarvis.domain.rotation.ObserveDeviceRotationNotificationUseCase
import io.github.taetae98coding.jarvis.domain.rotation.ObserveDeviceRotationStatusUseCase
import io.github.taetae98coding.jarvis.domain.rotation.RotateDeviceUseCase
import io.github.taetae98coding.jarvis.domain.rotation.RotationAngle
import io.github.taetae98coding.jarvis.domain.rotation.SetDeviceRotationAngleUseCase
import io.github.taetae98coding.jarvis.domain.rotation.SetDeviceRotationLockUseCase
import io.github.taetae98coding.jarvis.domain.rotation.SetDeviceRotationNotificationPinnedUseCase
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

internal class DeviceRotationViewModel(
    observeDeviceRotationStatus: ObserveDeviceRotationStatusUseCase,
    private val setDeviceRotationAngle: SetDeviceRotationAngleUseCase,
    private val setDeviceRotationLock: SetDeviceRotationLockUseCase,
    private val rotateDevice: RotateDeviceUseCase,
    observeDeviceRotationNotification: ObserveDeviceRotationNotificationUseCase,
    private val setDeviceRotationNotificationPinned: SetDeviceRotationNotificationPinnedUseCase,
) : ViewModel() {
    val deviceRotation: StateFlow<DeviceRotationStatus> = observeDeviceRotationStatus(viewModelScope)

    val notification: StateFlow<DeviceRotationNotificationStatus> = observeDeviceRotationNotification(viewModelScope)

    fun onDeviceRotationAngleClick(angle: RotationAngle) {
        viewModelScope.launch { setDeviceRotationAngle(angle) }
    }

    fun onDeviceRotate(steps: Int) {
        viewModelScope.launch { rotateDevice(steps) }
    }

    fun onDeviceRotationLockChange(locked: Boolean) {
        viewModelScope.launch { setDeviceRotationLock(locked) }
    }

    fun onNotificationPinnedChange(pinned: Boolean) {
        viewModelScope.launch { setDeviceRotationNotificationPinned(pinned) }
    }
}
