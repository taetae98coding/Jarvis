package io.github.taetae98coding.jarvis.domain.screen

import kotlinx.coroutines.flow.first

class SetSystemScreenAwakeNotificationPinnedUseCase(
    private val notification: SystemScreenAwakeNotificationRepository,
) {
    suspend operator fun invoke(pinned: Boolean) {
        // 권한이 없어도 값은 켜 둔다. SetKeepSystemScreenAwakeUseCase 와 같은 판단이다.
        notification.setPinned(pinned)

        if (pinned && !notification.observeStatus().first().permitted) {
            notification.requestPermission()
        }
    }
}
