package io.github.taetae98coding.jarvis.ui.rotation

import io.github.taetae98coding.jarvis.ui.navigation.LocalNavigator
import io.github.taetae98coding.jarvis.ui.navigation.navKeySerializers
import kotlinx.serialization.modules.subclass
import org.koin.compose.viewmodel.dsl.viewModelOf
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.dsl.module
import org.koin.dsl.navigation3.navigation

@OptIn(KoinExperimentalAPI::class)
val rotationUiModule = module {
    viewModelOf(::DeviceRotationViewModel)

    navigation<DeviceRotationRoute> {
        DeviceRotationScreen(onBack = LocalNavigator.current::back)
    }

    navKeySerializers<DeviceRotationRoute> {
        subclass(DeviceRotationRoute::class, DeviceRotationRoute.serializer())
    }
}
