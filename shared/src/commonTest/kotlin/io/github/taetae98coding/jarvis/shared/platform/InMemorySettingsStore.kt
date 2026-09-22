package io.github.taetae98coding.jarvis.shared.platform

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

internal class InMemorySettingsStore(
    private val values: MutableMap<String, Boolean> = mutableMapOf(),
) : SettingsStore {
    private val changeSignals = MutableSharedFlow<Unit>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    override val changes: Flow<Unit> = changeSignals

    override fun getBoolean(key: String, defaultValue: Boolean): Boolean =
        values[key] ?: defaultValue

    override fun putBoolean(key: String, value: Boolean) {
        values[key] = value
        changeSignals.tryEmit(Unit)
    }
}
