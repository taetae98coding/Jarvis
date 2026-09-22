package io.github.taetae98coding.jarvis.shared.settings

import androidx.compose.runtime.Stable
import androidx.compose.runtime.staticCompositionLocalOf
import io.github.taetae98coding.jarvis.shared.platform.SettingsStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/**
 * 앱 범위 설정. 모든 화면보다 위에 존재하므로 사용자가 화면을 옮겨도 토글 효과가 유지되고,
 * [SettingsStore] 가 값을 보관하므로 앱을 껐다 켜도 남는다.
 */
@Stable
internal class AppSettings(
    private val store: SettingsStore,
    scope: CoroutineScope,
) {
    // 초기값을 스토어에서 동기로 읽어 두어야 첫 프레임부터 저장된 값이 보인다. Flow 의 첫 방출은
    // 컴포지션이 한 번 끝난 뒤에야 도착한다.
    val keepScreenAwake: StateFlow<Boolean> =
        store.observeBoolean(KeepScreenAwakeKey, false)
            .stateIn(scope, SharingStarted.Eagerly, store.getBoolean(KeepScreenAwakeKey, false))

    fun setKeepScreenAwake(value: Boolean) {
        store.putBoolean(KeepScreenAwakeKey, value)
    }

    internal companion object {
        const val KeepScreenAwakeKey = "keep_screen_awake"
    }
}

internal val LocalAppSettings = staticCompositionLocalOf<AppSettings> {
    error("LocalAppSettings 가 제공되지 않았습니다. 화면을 App() 으로 감싸세요.")
}
