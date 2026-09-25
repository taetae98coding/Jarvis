package io.github.taetae98coding.jarvis.data.settings

import io.github.taetae98coding.jarvis.data.PlatformContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.util.prefs.PreferenceChangeListener
import java.util.prefs.Preferences

actual fun createSettingsStore(context: PlatformContext): SettingsStore =
    PreferencesSettingsStore

private object PreferencesSettingsStore : SettingsStore {
    private val preferences: Preferences =
        Preferences.userRoot().node("io/github/taetae98coding/jarvis")

    override fun getBoolean(key: String, defaultValue: Boolean): Boolean =
        preferences.getBoolean(key, defaultValue)

    override fun putBoolean(key: String, value: Boolean) {
        preferences.putBoolean(key, value)
    }

    override fun getString(key: String, defaultValue: String): String =
        preferences.get(key, defaultValue)

    override fun putString(key: String, value: String) {
        preferences.put(key, value)
    }

    override val changes: Flow<Unit> = callbackFlow {
        val listener = PreferenceChangeListener { trySend(Unit) }
        preferences.addPreferenceChangeListener(listener)
        awaitClose { preferences.removePreferenceChangeListener(listener) }
    }
}
