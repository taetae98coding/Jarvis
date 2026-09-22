package io.github.taetae98coding.jarvis.domain.screen

class SetKeepScreenAwakeUseCase(
    private val settings: ScreenAwakeSettingsRepository,
) {
    operator fun invoke(value: Boolean) {
        settings.setKeepScreenAwake(value)
    }
}
