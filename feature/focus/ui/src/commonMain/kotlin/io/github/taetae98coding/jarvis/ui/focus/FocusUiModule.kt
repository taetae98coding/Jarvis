package io.github.taetae98coding.jarvis.ui.focus

import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val focusUiModule = module {
    viewModelOf(::FocusTimerViewModel)
}
