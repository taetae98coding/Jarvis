package io.github.taetae98coding.jarvis.ui.app

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
internal actual fun installTestMainDispatcher() {
    Dispatchers.setMain(Dispatchers.Unconfined)
}
