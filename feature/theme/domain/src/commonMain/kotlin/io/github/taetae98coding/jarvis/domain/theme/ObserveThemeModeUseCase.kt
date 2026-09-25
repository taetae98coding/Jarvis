package io.github.taetae98coding.jarvis.domain.theme

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class ObserveThemeModeUseCase(
    private val settings: ThemeSettingsRepository,
) {
    operator fun invoke(scope: CoroutineScope): StateFlow<ThemeMode> =
        settings.observeThemeMode()
            .stateIn(scope, SharingStarted.WhileSubscribed(), settings.readThemeMode())
}
