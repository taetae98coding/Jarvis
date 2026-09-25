package io.github.taetae98coding.jarvis.data.appinfo

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

// 창을 닫는 exitApplication 은 desktopApp 의 application { } 안에서만 부를 수 있다. 창이 나중에 구독해도
// 놓치지 않게 마지막 요청 하나를 남긴다(docs/common/app-update.html#implementation).
private val quitRequests = MutableSharedFlow<Unit>(replay = 1)

/** 업데이트 교체를 준비한 뒤 앱을 정상 종료해 달라는 요청. */
val appQuitRequests: Flow<Unit> = quitRequests

internal fun requestAppQuit() {
    quitRequests.tryEmit(Unit)
}
