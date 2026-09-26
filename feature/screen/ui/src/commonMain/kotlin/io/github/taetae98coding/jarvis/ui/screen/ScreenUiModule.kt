package io.github.taetae98coding.jarvis.ui.screen

import io.github.taetae98coding.jarvis.ui.navigation.LocalNavigator
import io.github.taetae98coding.jarvis.ui.navigation.navKeySerializers
import kotlinx.serialization.modules.subclass
import org.koin.compose.viewmodel.dsl.viewModelOf
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.dsl.module
import org.koin.dsl.navigation3.navigation

@OptIn(KoinExperimentalAPI::class)
val screenUiModule = module {
    viewModelOf(::ScreenAwakeViewModel)
    viewModelOf(::ScreenAwakeEffectViewModel)

    navigation<ScreenAwakeRoute.App> {
        ScreenAwakeScreen(onBack = LocalNavigator.current::back)
    }

    navigation<ScreenAwakeRoute.System> {
        SystemScreenAwakeScreen(onBack = LocalNavigator.current::back)
    }

    navKeySerializers<ScreenAwakeRoute> {
        subclass(ScreenAwakeRoute.App::class, ScreenAwakeRoute.App.serializer())
        subclass(ScreenAwakeRoute.System::class, ScreenAwakeRoute.System.serializer())
    }
}
