package io.github.taetae98coding.jarvis.shared.platform

import androidx.compose.runtime.Composable
import platform.Foundation.NSUserDefaults

@Composable
internal actual fun rememberSettingsStore(): SettingsStore = UserDefaultsSettingsStore

private object UserDefaultsSettingsStore : SettingsStore {
    private val defaults = NSUserDefaults.standardUserDefaults

    override fun getBoolean(key: String, defaultValue: Boolean): Boolean =
        // boolForKey returns false for a missing key, so the default needs an explicit check.
        if (defaults.objectForKey(key) == null) defaultValue else defaults.boolForKey(key)

    override fun putBoolean(key: String, value: Boolean) {
        defaults.setBool(value, key)
    }
}
