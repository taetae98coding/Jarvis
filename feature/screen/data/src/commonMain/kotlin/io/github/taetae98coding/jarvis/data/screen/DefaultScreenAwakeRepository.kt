package io.github.taetae98coding.jarvis.data.screen

import io.github.taetae98coding.jarvis.domain.screen.ScreenAwakeRepository

internal class DefaultScreenAwakeRepository(
    private val inhibitor: IdleInhibitor,
) : ScreenAwakeRepository {
    override fun setKeepScreenAwake(enabled: Boolean) {
        inhibitor.setEnabled(enabled)
    }
}
