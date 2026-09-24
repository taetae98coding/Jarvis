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

    /** 지금 변경 신호를 듣고 있는 수집자 수. 플랫폼 저장소의 등록된 리스너 수에 해당한다. */
    val listeners: Int get() = changeSignals.subscriptionCount.value

    override fun getBoolean(key: String, defaultValue: Boolean): Boolean =
        values[key] ?: defaultValue

    override fun putBoolean(key: String, value: Boolean) {
        values[key] = value
        changeSignals.tryEmit(Unit)
    }
}
