package io.github.taetae98coding.jarvis.data.theme

import io.github.taetae98coding.jarvis.data.settings.SettingsStore
import io.github.taetae98coding.jarvis.domain.theme.ThemeMode
import io.github.taetae98coding.jarvis.domain.theme.ThemeSettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal class DefaultThemeSettingsRepository(
    private val store: SettingsStore,
) : ThemeSettingsRepository {
    override fun observeThemeMode(): Flow<ThemeMode> =
        store.observeString(ThemeModeKey, ThemeMode.SYSTEM.storedValue).map(ThemeMode::fromStored)

    override fun readThemeMode(): ThemeMode =
        ThemeMode.fromStored(store.getString(ThemeModeKey, ThemeMode.SYSTEM.storedValue))

    override fun setThemeMode(mode: ThemeMode) {
        store.putString(ThemeModeKey, mode.storedValue)
    }

    internal companion object {
        const val ThemeModeKey = "theme_mode"
    }
}
