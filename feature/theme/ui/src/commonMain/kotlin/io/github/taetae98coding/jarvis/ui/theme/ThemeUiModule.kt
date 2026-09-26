package io.github.taetae98coding.jarvis.ui.theme

import io.github.taetae98coding.jarvis.ui.navigation.LocalNavigator
import io.github.taetae98coding.jarvis.ui.navigation.navKeySerializers
import kotlinx.serialization.modules.subclass
import org.koin.compose.viewmodel.dsl.viewModelOf
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.dsl.module
import org.koin.dsl.navigation3.navigation

@OptIn(KoinExperimentalAPI::class)
val themeUiModule = module {
    viewModelOf(::ThemeModeViewModel)
    viewModelOf(::ThemeEffectViewModel)

    navigation<ThemeModeRoute> {
        ThemeModeScreen(onBack = LocalNavigator.current::back)
    }

    navKeySerializers<ThemeModeRoute> {
        subclass(ThemeModeRoute::class, ThemeModeRoute.serializer())
    }
}
