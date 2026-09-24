package io.github.taetae98coding.jarvis.data.screen

import io.github.taetae98coding.jarvis.data.settings.SettingsStore
import io.github.taetae98coding.jarvis.domain.screen.ScreenAwakeSettingsRepository
import kotlinx.coroutines.flow.Flow

internal class DefaultScreenAwakeSettingsRepository(
    private val store: SettingsStore,
) : ScreenAwakeSettingsRepository {
    override fun observeKeepScreenAwake(): Flow<Boolean> = store.observeBoolean(KeepScreenAwakeKey, false)

    override fun readKeepScreenAwake(): Boolean = store.getBoolean(KeepScreenAwakeKey, false)

    override fun observeKeepSystemScreenAwake(): Flow<Boolean> = store.observeBoolean(KeepSystemScreenAwakeKey, false)

    override fun readKeepSystemScreenAwake(): Boolean = store.getBoolean(KeepSystemScreenAwakeKey, false)

    override fun setKeepScreenAwake(value: Boolean) {
        store.putBoolean(KeepScreenAwakeKey, value)
    }

    override fun setKeepSystemScreenAwake(value: Boolean) {
        store.putBoolean(KeepSystemScreenAwakeKey, value)
    }

    internal companion object {
        const val KeepScreenAwakeKey = "keep_screen_awake"
        const val KeepSystemScreenAwakeKey = "keep_system_screen_awake"
    }
}
