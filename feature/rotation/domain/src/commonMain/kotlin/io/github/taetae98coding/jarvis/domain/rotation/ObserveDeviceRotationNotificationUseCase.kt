package io.github.taetae98coding.jarvis.domain.rotation

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class ObserveDeviceRotationNotificationUseCase(
    private val notification: DeviceRotationNotificationRepository,
) {
    operator fun invoke(scope: CoroutineScope): StateFlow<DeviceRotationNotificationStatus> =
        notification.observeStatus()
            .stateIn(scope, SharingStarted.WhileSubscribed(), notification.readStatus())
}
