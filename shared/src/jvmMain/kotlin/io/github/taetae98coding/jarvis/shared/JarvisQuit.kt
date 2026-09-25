package io.github.taetae98coding.jarvis.shared

import io.github.taetae98coding.jarvis.data.appinfo.appQuitRequests
import kotlinx.coroutines.flow.Flow

/** 앱 업데이트가 교체를 준비한 뒤 창을 닫아 달라고 할 때 온다(docs/common/app-update.html#implementation). */
val jarvisQuitRequests: Flow<Unit> = appQuitRequests
