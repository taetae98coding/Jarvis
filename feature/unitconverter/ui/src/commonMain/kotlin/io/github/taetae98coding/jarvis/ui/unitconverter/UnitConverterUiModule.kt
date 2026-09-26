package io.github.taetae98coding.jarvis.ui.unitconverter

import io.github.taetae98coding.jarvis.ui.navigation.LocalNavigator
import io.github.taetae98coding.jarvis.ui.navigation.navKeySerializers
import kotlinx.serialization.modules.subclass
import org.koin.compose.viewmodel.dsl.viewModelOf
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.dsl.module
import org.koin.dsl.navigation3.navigation

@OptIn(KoinExperimentalAPI::class)
val unitConverterUiModule = module {
    viewModelOf(::UnitConverterViewModel)
    viewModelOf(::UnitConverterCardViewModel)

    navigation<UnitConverterRoute> {
        UnitConverterScreen(viewModel = koinViewModel(), onBack = LocalNavigator.current::back)
    }

    navKeySerializers<UnitConverterRoute> {
        subclass(UnitConverterRoute::class, UnitConverterRoute.serializer())
    }
}
