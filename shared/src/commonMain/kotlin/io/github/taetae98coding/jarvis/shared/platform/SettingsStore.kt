package io.github.taetae98coding.jarvis.shared.platform

import androidx.compose.runtime.Composable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart

internal interface SettingsStore {
    fun getBoolean(key: String, defaultValue: Boolean): Boolean

    fun putBoolean(key: String, value: Boolean)

    // 어떤 키가 바뀌었는지는 담지 않는다. iOS 의 NSUserDefaultsDidChangeNotification 이 바뀐 키를
    // 알려주지 않아서, 키별 필터링 대신 값을 다시 읽고 distinctUntilChanged 로 걸러낸다.
    val changes: Flow<Unit>

    fun observeBoolean(key: String, defaultValue: Boolean): Flow<Boolean> =
        changes
            .conflate()
            .onStart { emit(Unit) }
            .map { getBoolean(key, defaultValue) }
            .distinctUntilChanged()
}

@Composable
internal expect fun rememberSettingsStore(): SettingsStore
