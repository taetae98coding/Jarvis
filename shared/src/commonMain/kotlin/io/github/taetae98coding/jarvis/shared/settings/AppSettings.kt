package io.github.taetae98coding.jarvis.shared.settings

import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.staticCompositionLocalOf
import io.github.taetae98coding.jarvis.shared.platform.SettingsStore

/**
 * App scoped settings. Lives above every screen so a toggle keeps its effect while the user moves
 * around, and is backed by [SettingsStore] so it survives a cold start.
 */
@Stable
internal class AppSettings(private val store: SettingsStore) {
    private val keepScreenAwakeState = mutableStateOf(store.getBoolean(KeepScreenAwakeKey, false))

    var keepScreenAwake: Boolean
        get() = keepScreenAwakeState.value
        set(value) {
            keepScreenAwakeState.value = value
            store.putBoolean(KeepScreenAwakeKey, value)
        }

    internal companion object {
        const val KeepScreenAwakeKey = "keep_screen_awake"
    }
}

internal val LocalAppSettings = staticCompositionLocalOf<AppSettings> {
    error("LocalAppSettings was not provided. Wrap the screen in App().")
}
