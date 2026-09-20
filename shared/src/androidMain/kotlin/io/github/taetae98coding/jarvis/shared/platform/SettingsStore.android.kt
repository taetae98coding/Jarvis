package io.github.taetae98coding.jarvis.shared.platform

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

@Composable
internal actual fun rememberSettingsStore(): SettingsStore {
    val context = LocalContext.current

    return remember(context) { SharedPreferencesSettingsStore(context.applicationContext) }
}

private class SharedPreferencesSettingsStore(context: Context) : SettingsStore {
    private val preferences: SharedPreferences =
        context.getSharedPreferences("jarvis.settings", Context.MODE_PRIVATE)

    override fun getBoolean(key: String, defaultValue: Boolean): Boolean =
        preferences.getBoolean(key, defaultValue)

    override fun putBoolean(key: String, value: Boolean) {
        preferences.edit().putBoolean(key, value).apply()
    }
}
