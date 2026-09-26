package io.github.taetae98coding.jarvis.data.settings

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

internal class InMemorySettingsStore(
    private val values: MutableMap<String, Any> = mutableMapOf(),
) : SettingsStore {
    private val changeSignals = MutableSharedFlow<Unit>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    override val changes: Flow<Unit> = changeSignals

    /** 지금 변경 신호를 듣고 있는 수집자 수. 플랫폼 저장소의 등록된 리스너 수에 해당한다. */
    val listeners: Int get() = changeSignals.subscriptionCount.value

    override fun getBoolean(key: String, defaultValue: Boolean): Boolean =
        values[key] as? Boolean ?: defaultValue

    override fun putBoolean(key: String, value: Boolean) {
        values[key] = value
        changeSignals.tryEmit(Unit)
    }

    override fun getString(key: String, defaultValue: String): String =
        values[key] as? String ?: defaultValue

    override fun putString(key: String, value: String) {
        values[key] = value
        changeSignals.tryEmit(Unit)
    }
}
