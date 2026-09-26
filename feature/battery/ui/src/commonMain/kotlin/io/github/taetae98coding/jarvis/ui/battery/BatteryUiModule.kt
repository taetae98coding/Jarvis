package io.github.taetae98coding.jarvis.ui.battery

import org.koin.compose.viewmodel.dsl.viewModelOf
import org.koin.dsl.module

val batteryUiModule = module {
    viewModelOf(::BatteryViewModel)
}
