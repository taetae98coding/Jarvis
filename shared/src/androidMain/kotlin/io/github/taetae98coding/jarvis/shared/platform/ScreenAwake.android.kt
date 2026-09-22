package io.github.taetae98coding.jarvis.shared.platform

import androidx.compose.runtime.Composable

// 이 플랫폼은 Modifier.keepScreenOn() 이 처리한다. expect 선언의 주석을 볼 것.
@Composable
internal actual fun PlatformIdleInhibitor(enabled: Boolean) = Unit
