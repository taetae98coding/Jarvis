package io.github.taetae98coding.jarvis.shared.platform

import androidx.compose.runtime.Composable
import platform.Foundation.NSUserDefaults

@Composable
internal actual fun rememberSettingsStore(): SettingsStore = UserDefaultsSettingsStore

private object UserDefaultsSettingsStore : SettingsStore {
    private val defaults = NSUserDefaults.standardUserDefaults

    override fun getBoolean(key: String, defaultValue: Boolean): Boolean =
        // boolForKey 는 키가 없을 때도 false 를 반환하므로, 기본값은 따로 확인해야 한다.
        if (defaults.objectForKey(key) == null) defaultValue else defaults.boolForKey(key)

    override fun putBoolean(key: String, value: Boolean) {
        defaults.setBool(value, key)
    }
}
