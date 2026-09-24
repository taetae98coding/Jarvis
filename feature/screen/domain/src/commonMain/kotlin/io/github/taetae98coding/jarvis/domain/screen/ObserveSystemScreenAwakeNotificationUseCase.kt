package io.github.taetae98coding.jarvis.domain.screen

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class ObserveSystemScreenAwakeNotificationUseCase(
    private val notification: SystemScreenAwakeNotificationRepository,
) {
    operator fun invoke(scope: CoroutineScope): StateFlow<SystemScreenAwakeNotificationStatus> =
        notification.observeStatus()
            .stateIn(scope, SharingStarted.WhileSubscribed(), notification.readStatus())
}
