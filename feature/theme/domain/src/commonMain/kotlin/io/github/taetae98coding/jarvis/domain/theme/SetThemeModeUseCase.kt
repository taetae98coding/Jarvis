package io.github.taetae98coding.jarvis.domain.theme

class SetThemeModeUseCase(
    private val settings: ThemeSettingsRepository,
) {
    operator fun invoke(mode: ThemeMode) {
        settings.setThemeMode(mode)
    }
}
