package io.github.taetae98coding.jarvis.ui.theme

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.taetae98coding.jarvis.domain.theme.ObserveThemeModeUseCase
import io.github.taetae98coding.jarvis.domain.theme.SetThemeModeUseCase
import io.github.taetae98coding.jarvis.domain.theme.ThemeMode
import kotlinx.coroutines.flow.StateFlow

internal class ThemeModeViewModel(
    observeThemeMode: ObserveThemeModeUseCase,
    private val setThemeMode: SetThemeModeUseCase,
) : ViewModel() {
    val themeMode: StateFlow<ThemeMode> = observeThemeMode(viewModelScope)

    fun onThemeModeChange(mode: ThemeMode) {
        setThemeMode(mode)
    }
}
