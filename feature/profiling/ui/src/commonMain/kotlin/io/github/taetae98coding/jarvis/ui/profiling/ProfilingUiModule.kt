package io.github.taetae98coding.jarvis.ui.profiling

import org.koin.compose.viewmodel.dsl.viewModelOf
import org.koin.dsl.module

val profilingUiModule = module {
    viewModelOf(::ProfilingViewModel)
}
