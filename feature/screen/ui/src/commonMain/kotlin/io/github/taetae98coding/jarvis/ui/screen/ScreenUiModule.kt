package io.github.taetae98coding.jarvis.ui.screen

import org.koin.compose.viewmodel.dsl.viewModelOf
import org.koin.dsl.module

val screenUiModule = module {
    viewModelOf(::ScreenAwakeViewModel)
    viewModelOf(::ScreenAwakeEffectViewModel)
}
