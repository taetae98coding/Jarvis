package io.github.taetae98coding.jarvis.shared.platform

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.keepScreenOn

// Compose 의 Modifier.keepScreenOn() 이 Android 의 View.keepScreenOn, iOS 의
// UIApplication.idleTimerDisabled, Web 의 Screen Wake Lock API 를 대신 호출한다. 레퍼런스 카운팅을
// 하므로 여러 곳에서 겹쳐 요청해도 서로의 요청을 지우지 않는다.
internal fun Modifier.keepScreenAwake(enabled: Boolean): Modifier = if (enabled) keepScreenOn() else this

// Modifier.keepScreenOn() 의 데스크탑 경로만 비어 있다. Compose Multiplatform 1.12.0 의
// PlatformContext.setKeepScreenOnEnabled 는 본문이 빈 기본 구현이고 이를 오버라이드하는 Swing/AWT
// 구현이 없어서, JVM 에서는 modifier 가 아무 일도 하지 않는다. 이 훅이 JVM 에서만 실제 동작을 갖고
// 나머지 타깃에서는 비어 있다. CMP 가 데스크탑 경로를 구현하면 통째로 걷어낸다.
@Composable
internal expect fun PlatformIdleInhibitor(enabled: Boolean)
