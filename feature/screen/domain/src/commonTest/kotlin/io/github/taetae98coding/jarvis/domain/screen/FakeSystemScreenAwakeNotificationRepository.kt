package io.github.taetae98coding.jarvis.domain.screen

import kotlinx.coroutines.flow.MutableStateFlow

internal class FakeSystemScreenAwakeNotificationRepository(
    initial: SystemScreenAwakeNotificationStatus = SystemScreenAwakeNotificationStatus(supported = true, permitted = true),
) : SystemScreenAwakeNotificationRepository {
    val status = MutableStateFlow(initial)
    var permissionRequests = 0
        private set

    override fun observeStatus() = status

    override fun readStatus() = status.value

    override fun setPinned(pinned: Boolean) {
        status.value = status.value.copy(pinned = pinned)
    }

    override fun requestPermission() {
        permissionRequests++
    }
}
