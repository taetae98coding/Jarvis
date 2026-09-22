package io.github.taetae98coding.jarvis.shared.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect

@Composable
internal actual fun PlatformIdleInhibitor(enabled: Boolean) {
    DisposableEffect(enabled) {
        val inhibitor = if (enabled) startIdleInhibitor() else null
        onDispose { inhibitor?.destroy() }
    }
}

// The JVM has no portable idle-inhibit API, so this delegates to the tool that owns the idle
// timer. Desktop support is macOS-only by decision; the Linux (systemd-inhibit) and Windows
// (SetThreadExecutionState via JNA) paths were dropped rather than left half-wired.
// On any other OS `caffeinate` is absent, so the launch fails and the toggle is a no-op.
private fun startIdleInhibitor(): Process? =
    runCatching { ProcessBuilder("caffeinate", "-di").start() }.getOrNull()
