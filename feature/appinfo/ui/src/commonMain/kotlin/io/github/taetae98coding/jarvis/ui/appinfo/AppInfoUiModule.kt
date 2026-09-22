package io.github.taetae98coding.jarvis.ui.appinfo

import org.koin.compose.viewmodel.dsl.viewModelOf
import org.koin.dsl.module

val appInfoUiModule = module {
    viewModelOf(::AppInfoViewModel)
}
