package io.github.taetae98coding.jarvis.data.theme

import io.github.taetae98coding.jarvis.data.settings.createSettingsStore
import io.github.taetae98coding.jarvis.domain.theme.ThemeAppearanceRepository
import io.github.taetae98coding.jarvis.domain.theme.ThemeSettingsRepository
import org.koin.dsl.module

val themeDataModule = module {
    single<ThemeSettingsRepository> { DefaultThemeSettingsRepository(createSettingsStore(get())) }

    single<ThemeAppearanceRepository> { DefaultThemeAppearanceRepository(createThemeAppearance(get())) }
}
