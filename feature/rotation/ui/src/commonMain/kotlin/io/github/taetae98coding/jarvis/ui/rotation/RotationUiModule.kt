package io.github.taetae98coding.jarvis.ui.rotation

import org.koin.compose.viewmodel.dsl.viewModelOf
import org.koin.dsl.module

val rotationUiModule = module {
    viewModelOf(::DeviceRotationViewModel)
}
