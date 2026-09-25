package io.github.taetae98coding.jarvis.domain.theme

import kotlinx.coroutines.flow.MutableStateFlow

internal class FakeThemeSettingsRepository(
    mode: ThemeMode = ThemeMode.SYSTEM,
) : ThemeSettingsRepository {
    val themeMode = MutableStateFlow(mode)

    override fun observeThemeMode() = themeMode

    override fun readThemeMode() = themeMode.value

    override fun setThemeMode(mode: ThemeMode) {
        themeMode.value = mode
    }
}
