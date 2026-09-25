package io.github.taetae98coding.jarvis.data.settings

import android.content.Context
import android.content.SharedPreferences
import io.github.taetae98coding.jarvis.data.PlatformContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

actual fun createSettingsStore(context: PlatformContext): SettingsStore =
    SharedPreferencesSettingsStore(context.context)

private class SharedPreferencesSettingsStore(context: Context) : SettingsStore {
    private val preferences: SharedPreferences =
        context.getSharedPreferences("jarvis.settings", Context.MODE_PRIVATE)

    override fun getBoolean(key: String, defaultValue: Boolean): Boolean =
        preferences.getBoolean(key, defaultValue)

    override fun putBoolean(key: String, value: Boolean) {
        preferences.edit().putBoolean(key, value).apply()
    }

    override fun getString(key: String, defaultValue: String): String =
        preferences.getString(key, null) ?: defaultValue

    override fun putString(key: String, value: String) {
        preferences.edit().putString(key, value).apply()
    }

    override val changes: Flow<Unit> = callbackFlow {
        // SharedPreferences 는 리스너를 약한 참조로만 잡는다. awaitClose 람다가 리스너를 붙들고 있어야
        // 수집 중에 GC 로 사라지지 않는다.
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> trySend(Unit) }
        preferences.registerOnSharedPreferenceChangeListener(listener)
        awaitClose { preferences.unregisterOnSharedPreferenceChangeListener(listener) }
    }
}
