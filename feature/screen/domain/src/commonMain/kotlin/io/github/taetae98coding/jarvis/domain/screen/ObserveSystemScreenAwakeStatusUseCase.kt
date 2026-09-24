package io.github.taetae98coding.jarvis.domain.screen

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class ObserveSystemScreenAwakeStatusUseCase(
    private val systemScreenAwake: SystemScreenAwakeRepository,
) {
    operator fun invoke(scope: CoroutineScope): StateFlow<SystemScreenAwakeStatus> =
        systemScreenAwake.observeStatus()
            .stateIn(scope, SharingStarted.WhileSubscribed(), systemScreenAwake.readStatus())
}
