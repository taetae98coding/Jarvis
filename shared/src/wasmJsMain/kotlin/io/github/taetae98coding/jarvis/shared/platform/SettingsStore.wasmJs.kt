package io.github.taetae98coding.jarvis.shared.platform

import androidx.compose.runtime.Composable
import kotlinx.browser.localStorage

@Composable
internal actual fun rememberSettingsStore(): SettingsStore = LocalStorageSettingsStore

private object LocalStorageSettingsStore : SettingsStore {
    override fun getBoolean(key: String, defaultValue: Boolean): Boolean =
        localStorage.getItem(key.namespaced())?.toBooleanStrictOrNull() ?: defaultValue

    override fun putBoolean(key: String, value: Boolean) {
        localStorage.setItem(key.namespaced(), value.toString())
    }

    // localStorage is shared by every page on the origin, unlike the per-app stores the other
    // targets get, so keys carry the same namespace those stores have built in.
    private fun String.namespaced(): String = "jarvis.settings.$this"
}
