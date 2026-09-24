package io.github.taetae98coding.jarvis.data.rotation

import io.github.taetae98coding.jarvis.data.notification.NotificationPermission
import io.github.taetae98coding.jarvis.data.settings.SettingsStore
import io.github.taetae98coding.jarvis.domain.rotation.DeviceRotationNotificationRepository
import io.github.taetae98coding.jarvis.domain.rotation.DeviceRotationNotificationStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/** 플랫폼에 기대는 것은 권한만이라 expect 가 없다. 저장은 SettingsStore 가 플랫폼을 숨긴다. */
internal class DefaultDeviceRotationNotificationRepository(
    private val store: SettingsStore,
    private val permission: NotificationPermission,
) : DeviceRotationNotificationRepository {
    override fun observeStatus(): Flow<DeviceRotationNotificationStatus> =
        combine(permission.observePermitted(), store.observeBoolean(PinnedKey, false), ::status)

    override fun readStatus(): DeviceRotationNotificationStatus =
        status(permission.readPermitted(), store.getBoolean(PinnedKey, false))

    override fun setPinned(pinned: Boolean) {
        store.putBoolean(PinnedKey, pinned)
    }

    override fun requestPermission() {
        permission.requestPermission()
    }

    private fun status(permitted: Boolean, pinned: Boolean) =
        DeviceRotationNotificationStatus(supported = permission.supported, permitted = permitted, pinned = pinned)

    internal companion object {
        const val PinnedKey = "device_rotation_notification_pinned"
    }
}
