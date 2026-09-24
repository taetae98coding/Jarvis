package io.github.taetae98coding.jarvis.data.screen

import io.github.taetae98coding.jarvis.data.notification.NotificationPermission
import io.github.taetae98coding.jarvis.data.settings.SettingsStore
import io.github.taetae98coding.jarvis.domain.screen.SystemScreenAwakeNotificationRepository
import io.github.taetae98coding.jarvis.domain.screen.SystemScreenAwakeNotificationStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/** 플랫폼에 기대는 것은 권한만이라 expect 가 없다. 저장은 SettingsStore 가 플랫폼을 숨긴다. */
internal class DefaultSystemScreenAwakeNotificationRepository(
    private val store: SettingsStore,
    private val permission: NotificationPermission,
) : SystemScreenAwakeNotificationRepository {
    override fun observeStatus(): Flow<SystemScreenAwakeNotificationStatus> =
        combine(permission.observePermitted(), store.observeBoolean(PinnedKey, false), ::status)

    override fun readStatus(): SystemScreenAwakeNotificationStatus =
        status(permission.readPermitted(), store.getBoolean(PinnedKey, false))

    override fun setPinned(pinned: Boolean) {
        store.putBoolean(PinnedKey, pinned)
    }

    override fun requestPermission() {
        permission.requestPermission()
    }

    private fun status(permitted: Boolean, pinned: Boolean) =
        SystemScreenAwakeNotificationStatus(supported = permission.supported, permitted = permitted, pinned = pinned)

    internal companion object {
        const val PinnedKey = "system_screen_awake_notification_pinned"
    }
}
