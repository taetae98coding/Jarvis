package io.github.taetae98coding.jarvis.shared.platform

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.keepScreenOn

// Compose's own Modifier.keepScreenOn() already drives View.keepScreenOn on Android,
// UIApplication.idleTimerDisabled on iOS and the Screen Wake Lock API on Web, and it ref-counts
// so overlapping callers cannot clear each other's request.
internal fun Modifier.keepScreenAwake(enabled: Boolean): Modifier = if (enabled) keepScreenOn() else this

// The desktop half of Modifier.keepScreenOn() is missing: PlatformContext.setKeepScreenOnEnabled
// is an empty default in Compose Multiplatform 1.12.0 and no Swing/AWT scene overrides it, so the
// modifier is a no-op on the JVM. This hook carries the JVM inhibitor and does nothing elsewhere.
// Drop it once CMP implements the desktop path.
@Composable
internal expect fun PlatformIdleInhibitor(enabled: Boolean)
