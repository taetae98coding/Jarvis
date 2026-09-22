package io.github.taetae98coding.jarvis.domain.screen

import kotlinx.coroutines.flow.StateFlow

class ObserveKeepScreenAwakeUseCase(
    private val settings: ScreenAwakeSettingsRepository,
) {
    operator fun invoke(): StateFlow<Boolean> = settings.keepScreenAwake
}
