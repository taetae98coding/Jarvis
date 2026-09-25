package io.github.taetae98coding.jarvis.ui.theme

import org.koin.compose.viewmodel.dsl.viewModelOf
import org.koin.dsl.module

val themeUiModule = module {
    viewModelOf(::ThemeModeViewModel)
    viewModelOf(::ThemeEffectViewModel)
}
