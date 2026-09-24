package io.github.taetae98coding.jarvis.domain.screen

import kotlinx.coroutines.flow.MutableStateFlow

internal class FakeScreenAwakeSettingsRepository(
    keepScreenAwake: Boolean = false,
    keepSystemScreenAwake: Boolean = false,
) : ScreenAwakeSettingsRepository {
    val keepScreenAwake = MutableStateFlow(keepScreenAwake)
    val keepSystemScreenAwake = MutableStateFlow(keepSystemScreenAwake)

    override fun observeKeepScreenAwake() = keepScreenAwake

    override fun readKeepScreenAwake() = keepScreenAwake.value

    override fun observeKeepSystemScreenAwake() = keepSystemScreenAwake

    override fun readKeepSystemScreenAwake() = keepSystemScreenAwake.value

    override fun setKeepScreenAwake(value: Boolean) {
        keepScreenAwake.value = value
    }

    override fun setKeepSystemScreenAwake(value: Boolean) {
        keepSystemScreenAwake.value = value
    }
}
