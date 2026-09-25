package io.github.taetae98coding.jarvis.domain.appinfo

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class ObserveAppUpdateUseCase(
    private val repository: AppUpdateRepository,
) {
    operator fun invoke(scope: CoroutineScope): StateFlow<AppRelease?> =
        repository.observeAvailableUpdate()
            .stateIn(scope, SharingStarted.WhileSubscribed(), null)
}
