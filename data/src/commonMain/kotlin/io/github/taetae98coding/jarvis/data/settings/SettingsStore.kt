package io.github.taetae98coding.jarvis.data.settings

import io.github.taetae98coding.jarvis.data.PlatformContext
import io.github.taetae98coding.jarvis.data.state.observeSystemState
import kotlinx.coroutines.flow.Flow
import kotlin.time.Duration.Companion.seconds

internal interface SettingsStore {
    fun getBoolean(key: String, defaultValue: Boolean): Boolean

    fun putBoolean(key: String, value: Boolean)

    /**
     * 저장소가 변경을 알려주는 신호. 알려주지 않는 저장소는 null 을 두고 폴링으로 대체한다.
     *
     * 어떤 키가 바뀌었는지는 담지 않는다. iOS 의 NSUserDefaultsDidChangeNotification 이 바뀐 키를
     * 알려주지 않아서, 키별 필터링 대신 값을 다시 읽는 쪽으로 맞췄다.
     */
    val changes: Flow<Unit>?

    fun observeBoolean(key: String, defaultValue: Boolean): Flow<Boolean> =
        observeSystemState(signals = changes, interval = SettingsPollInterval) {
            getBoolean(key, defaultValue)
        }
}

// 변경 신호가 없는 저장소를 위한 간격. 설정은 사용자가 손댈 때만 바뀌므로 촘촘히 볼 필요가 없다.
internal val SettingsPollInterval = 2.seconds

internal expect fun createSettingsStore(context: PlatformContext): SettingsStore
