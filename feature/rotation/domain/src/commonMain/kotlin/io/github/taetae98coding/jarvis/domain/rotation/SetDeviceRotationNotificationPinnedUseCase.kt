package io.github.taetae98coding.jarvis.domain.rotation

import kotlinx.coroutines.flow.first

class SetDeviceRotationNotificationPinnedUseCase(
    private val notification: DeviceRotationNotificationRepository,
) {
    suspend operator fun invoke(pinned: Boolean) {
        // 권한이 없어도 값은 켜 둔다. 허용되는 순간 알림이 뜨고, 여기서 값을 되돌리면 왜 꺼졌는지 알 수 없다.
        notification.setPinned(pinned)

        if (pinned && !notification.observeStatus().first().permitted) {
            notification.requestPermission()
        }
    }
}
