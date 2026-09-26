package io.github.taetae98coding.jarvis.ui.worldclock

import io.github.taetae98coding.jarvis.ui.navigation.LocalNavigator
import io.github.taetae98coding.jarvis.ui.navigation.navKeySerializers
import kotlinx.serialization.modules.subclass
import org.koin.compose.viewmodel.dsl.viewModelOf
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.dsl.module
import org.koin.dsl.navigation3.navigation

@OptIn(KoinExperimentalAPI::class)
val worldClockUiModule = module {
    viewModelOf(::WorldClockCardViewModel)
    viewModelOf(::WorldClockViewModel)

    navigation<WorldClockRoute> {
        WorldClockScreen(viewModel = koinViewModel(), onBack = LocalNavigator.current::back)
    }

    navKeySerializers<WorldClockRoute> {
        subclass(WorldClockRoute::class, WorldClockRoute.serializer())
    }
}
