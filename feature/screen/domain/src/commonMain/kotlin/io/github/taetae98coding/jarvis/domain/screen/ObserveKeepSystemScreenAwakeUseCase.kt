package io.github.taetae98coding.jarvis.domain.screen

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class ObserveKeepSystemScreenAwakeUseCase(
    private val settings: ScreenAwakeSettingsRepository,
) {
    operator fun invoke(scope: CoroutineScope): StateFlow<Boolean> =
        settings.observeKeepSystemScreenAwake()
            .stateIn(scope, SharingStarted.WhileSubscribed(), settings.readKeepSystemScreenAwake())
}
