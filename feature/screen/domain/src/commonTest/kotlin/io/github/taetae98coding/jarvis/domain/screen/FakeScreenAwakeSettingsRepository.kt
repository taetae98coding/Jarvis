package io.github.taetae98coding.jarvis.domain.screen

import kotlinx.coroutines.flow.MutableStateFlow

internal class FakeScreenAwakeSettingsRepository(
    keepScreenAwake: Boolean = false,
    keepSystemScreenAwake: Boolean = false,
) : ScreenAwakeSettingsRepository {
    override val keepScreenAwake = MutableStateFlow(keepScreenAwake)
    override val keepSystemScreenAwake = MutableStateFlow(keepSystemScreenAwake)

    override fun setKeepScreenAwake(value: Boolean) {
        this.keepScreenAwake.value = value
    }

    override fun setKeepSystemScreenAwake(value: Boolean) {
        this.keepSystemScreenAwake.value = value
    }
}
