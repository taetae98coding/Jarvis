package io.github.taetae98coding.jarvis.shared.platform

import androidx.compose.runtime.Composable

internal interface SettingsStore {
    fun getBoolean(key: String, defaultValue: Boolean): Boolean

    fun putBoolean(key: String, value: Boolean)
}

@Composable
internal expect fun rememberSettingsStore(): SettingsStore
