package io.github.taetae98coding.jarvis.shared.platform

import androidx.compose.runtime.Composable

// Modifier.keepScreenOn() covers this platform; see the expect declaration.
@Composable
internal actual fun PlatformIdleInhibitor(enabled: Boolean) = Unit
