package io.github.taetae98coding.jarvis.shared.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import kotlin.js.ExperimentalWasmJsInterop

@Composable
internal actual fun KeepScreenAwake(enabled: Boolean) {
    DisposableEffect(enabled) {
        setScreenWakeLock(enabled)
        onDispose { setScreenWakeLock(false) }
    }
}

// The sentinel lives on globalThis because releasing a wake lock needs the exact object the
// request resolved to, and awaiting that promise from Kotlin would make this a suspend API.
// The browser also drops the lock whenever the tab is hidden, hence the visibilitychange listener.
@OptIn(ExperimentalWasmJsInterop::class)
@JsFun(
    """
    (enabled) => {
        const wakeLock = globalThis.navigator?.wakeLock
        if (!wakeLock) return

        const acquire = () => {
            wakeLock.request('screen')
                .then((sentinel) => { globalThis.__jarvisWakeLock = sentinel })
                .catch(() => {})
        }

        if (!globalThis.__jarvisWakeLockListener) {
            globalThis.__jarvisWakeLockListener = () => {
                if (globalThis.__jarvisWakeLockEnabled && document.visibilityState === 'visible') acquire()
            }
            document.addEventListener('visibilitychange', globalThis.__jarvisWakeLockListener)
        }

        globalThis.__jarvisWakeLockEnabled = enabled
        if (enabled) {
            acquire()
        } else if (globalThis.__jarvisWakeLock) {
            globalThis.__jarvisWakeLock.release()
            globalThis.__jarvisWakeLock = null
        }
    }
    """,
)
private external fun setScreenWakeLock(enabled: Boolean)
