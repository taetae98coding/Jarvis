package io.github.taetae98coding.jarvis.ui.screen

import androidx.compose.ui.Modifier
import androidx.compose.ui.keepScreenOn

// 플랫폼 API 호출은 :data 가 맡는다는 규칙의 예외다. Compose 의 Modifier.keepScreenOn() 이 Android 의
// View.keepScreenOn, iOS 의 UIApplication.idleTimerDisabled, Web 의 Screen Wake Lock API 를 대신
// 호출해 주고, Modifier 형태로만 존재해서 Compose 밖으로 꺼낼 수 없다. 레퍼런스 카운팅을 하므로
// 여러 곳에서 겹쳐 요청해도 서로의 요청을 지우지 않는다.
//
// 이 modifier 가 동작하지 않는 JVM 은 :data 의 IdleInhibitor 가 대신 맡는다.
internal fun Modifier.keepScreenAwake(enabled: Boolean): Modifier = if (enabled) keepScreenOn() else this
