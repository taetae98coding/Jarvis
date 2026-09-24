package io.github.taetae98coding.jarvis.ui.terminal

import io.github.taetae98coding.jarvis.ui.navigation.LocalNavigator
import io.github.taetae98coding.jarvis.ui.navigation.navKeySerializers
import kotlinx.serialization.modules.subclass
import org.koin.compose.viewmodel.dsl.viewModelOf
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.core.module.dsl.onClose
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.withOptions
import org.koin.dsl.module
import org.koin.dsl.navigation3.navigation

@OptIn(KoinExperimentalAPI::class)
val terminalUiModule = module {
    viewModelOf(::TerminalCardViewModel)
    viewModelOf(::TerminalViewModel)
    viewModelOf(::TerminalSideBarViewModel)
    viewModelOf(::ClaudeNotificationEffectViewModel)
    singleOf(::ClaudeAttention)
    singleOf(::TerminalPaneHost) withOptions { onClose { it?.close() } }

    navigation<TerminalRoute> {
        val navigator = LocalNavigator.current

        TerminalScreen(
            viewModel = koinViewModel(),
            sideBar = koinViewModel(),
            onBack = navigator::back,
        )
    }

    navKeySerializers<TerminalRoute> {
        subclass(TerminalRoute::class, TerminalRoute.serializer())
    }
}
