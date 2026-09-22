package io.github.taetae98coding.jarvis.data.settings

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

internal class InMemorySettingsStore(
    private val values: MutableMap<String, Boolean> = mutableMapOf(),
    // 변경을 알려주지 않는 저장소를 흉내내려면 false 로 둔다. 그 경우 읽기는 폴링으로 대체된다.
    notifiesChanges: Boolean = true,
) : SettingsStore {
    private val changeSignals = MutableSharedFlow<Unit>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    override val changes: Flow<Unit>? = changeSignals.takeIf { notifiesChanges }

    override fun getBoolean(key: String, defaultValue: Boolean): Boolean =
        values[key] ?: defaultValue

    override fun putBoolean(key: String, value: Boolean) {
        values[key] = value
        changeSignals.tryEmit(Unit)
    }
}
