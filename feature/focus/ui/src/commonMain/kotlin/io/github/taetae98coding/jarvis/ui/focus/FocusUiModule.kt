package io.github.taetae98coding.jarvis.ui.focus

import io.github.taetae98coding.jarvis.ui.navigation.LocalNavigator
import io.github.taetae98coding.jarvis.ui.navigation.navKeySerializers
import kotlinx.serialization.modules.subclass
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import org.koin.dsl.navigation3.navigation

@OptIn(KoinExperimentalAPI::class)
val focusUiModule = module {
    viewModelOf(::FocusTimerViewModel)

    navigation<FocusTimerRoute> {
        FocusTimerScreen(onBack = LocalNavigator.current::back)
    }

    navKeySerializers<FocusTimerRoute> {
        subclass(FocusTimerRoute::class, FocusTimerRoute.serializer())
    }
}
