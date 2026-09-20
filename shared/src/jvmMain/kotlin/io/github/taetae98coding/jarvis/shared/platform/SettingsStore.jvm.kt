package io.github.taetae98coding.jarvis.shared.platform

import androidx.compose.runtime.Composable
import java.util.prefs.Preferences

@Composable
internal actual fun rememberSettingsStore(): SettingsStore = PreferencesSettingsStore

private object PreferencesSettingsStore : SettingsStore {
    private val preferences: Preferences =
        Preferences.userRoot().node("io/github/taetae98coding/jarvis")

    override fun getBoolean(key: String, defaultValue: Boolean): Boolean =
        preferences.getBoolean(key, defaultValue)

    override fun putBoolean(key: String, value: Boolean) {
        preferences.putBoolean(key, value)
    }
}
