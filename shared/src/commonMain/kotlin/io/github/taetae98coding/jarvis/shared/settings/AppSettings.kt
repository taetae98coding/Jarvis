package io.github.taetae98coding.jarvis.shared.settings

import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.staticCompositionLocalOf
import io.github.taetae98coding.jarvis.shared.platform.SettingsStore

/**
 * 앱 범위 설정. 모든 화면보다 위에 존재하므로 사용자가 화면을 옮겨도 토글 효과가 유지되고,
 * [SettingsStore] 가 값을 보관하므로 앱을 껐다 켜도 남는다.
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
    error("LocalAppSettings 가 제공되지 않았습니다. 화면을 App() 으로 감싸세요.")
}
