package io.github.taetae98coding.jarvis.ui

import io.github.taetae98coding.jarvis.ui.app.HomeRoute
import io.github.taetae98coding.jarvis.ui.app.HomeScreen
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.dsl.module
import org.koin.dsl.navigation3.navigation

/** 앱 셸의 라우트 하나. 기능의 라우트는 각 기능의 Koin 모듈에 있다. */
@OptIn(KoinExperimentalAPI::class)
val appUiModule = module {
    navigation<HomeRoute> {
        HomeScreen()
    }
}
