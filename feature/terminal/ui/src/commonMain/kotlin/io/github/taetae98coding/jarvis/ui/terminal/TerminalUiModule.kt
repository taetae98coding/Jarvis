package io.github.taetae98coding.jarvis.ui.terminal

import io.github.taetae98coding.jarvis.ui.navigation.LocalNavigator
import io.github.taetae98coding.jarvis.ui.navigation.navKeySerializers
import kotlinx.serialization.modules.subclass
import org.koin.compose.viewmodel.dsl.viewModelOf
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.dsl.module
import org.koin.dsl.navigation3.navigation

@OptIn(KoinExperimentalAPI::class)
val terminalUiModule = module {
    viewModelOf(::TerminalCardViewModel)
    viewModelOf(::TerminalViewModel)

    navigation<TerminalRoute> {
        val navigator = LocalNavigator.current

        TerminalScreen(
            viewModel = koinViewModel(),
            onBack = navigator::back,
        )
    }

    navKeySerializers<TerminalRoute> {
        subclass(TerminalRoute::class, TerminalRoute.serializer())
    }
}
