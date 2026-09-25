package io.github.taetae98coding.jarvis.ui.app

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.setMain

// Main 은 이벤트 루프 하나뿐인데 waitUntil 이 그것을 막은 채 돈다. 그대로 두면 기다리는 동안 viewModelScope 의 일이
// 돌지 못한다(docs/platform/web.html#test).
@OptIn(ExperimentalCoroutinesApi::class)
internal actual fun installTestMainDispatcher() {
    Dispatchers.setMain(Dispatchers.Unconfined)
}
