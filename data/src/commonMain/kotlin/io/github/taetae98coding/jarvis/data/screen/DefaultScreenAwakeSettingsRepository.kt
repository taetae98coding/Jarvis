package io.github.taetae98coding.jarvis.data.screen

import io.github.taetae98coding.jarvis.data.settings.SettingsStore
import io.github.taetae98coding.jarvis.domain.screen.ScreenAwakeSettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

internal class DefaultScreenAwakeSettingsRepository(
    private val store: SettingsStore,
    private val scope: CoroutineScope,
) : ScreenAwakeSettingsRepository {
    override val keepScreenAwake: StateFlow<Boolean> = booleanSetting(KeepScreenAwakeKey)

    override val keepSystemScreenAwake: StateFlow<Boolean> = booleanSetting(KeepSystemScreenAwakeKey)

    override fun setKeepScreenAwake(value: Boolean) {
        store.putBoolean(KeepScreenAwakeKey, value)
    }

    override fun setKeepSystemScreenAwake(value: Boolean) {
        store.putBoolean(KeepSystemScreenAwakeKey, value)
    }

    // 초기값을 스토어에서 동기로 읽어 두어야 첫 프레임부터 저장된 값이 보인다. Flow 의 첫 방출은
    // 컴포지션이 한 번 끝난 뒤에야 도착한다.
    private fun booleanSetting(key: String): StateFlow<Boolean> =
        store.observeBoolean(key, false)
            .stateIn(scope, SharingStarted.Eagerly, store.getBoolean(key, false))

    internal companion object {
        const val KeepScreenAwakeKey = "keep_screen_awake"
        const val KeepSystemScreenAwakeKey = "keep_system_screen_awake"
    }
}
