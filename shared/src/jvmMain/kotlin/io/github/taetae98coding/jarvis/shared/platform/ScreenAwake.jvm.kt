package io.github.taetae98coding.jarvis.shared.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect

@Composable
internal actual fun KeepScreenAwake(enabled: Boolean) {
    DisposableEffect(enabled) {
        val inhibitor = if (enabled) startIdleInhibitor() else null
        onDispose { inhibitor?.destroy() }
    }
}

// The JVM has no portable idle-inhibit API, so this delegates to the tool that owns the idle
// timer on each OS. Windows would need a SetThreadExecutionState call through JNA, which is not
// wired up, so the toggle is a no-op there.
private fun startIdleInhibitor(): Process? {
    val osName = System.getProperty("os.name").orEmpty().lowercase()
    val command = when {
        osName.contains("mac") -> listOf("caffeinate", "-di")
        osName.contains("nux") || osName.contains("nix") ->
            listOf("systemd-inhibit", "--what=idle", "--mode=block", "--why=Jarvis", "sleep", "infinity")
        else -> return null
    }

    return runCatching { ProcessBuilder(command).start() }.getOrNull()
}
