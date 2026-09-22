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

// JVM 에는 이식 가능한 idle-inhibit API 가 없어서, 유휴 타이머를 관리하는 OS 도구에 위임한다.
// 데스크탑은 macOS 만 지원하기로 했으므로 Linux(systemd-inhibit)와 Windows(JNA 를 통한
// SetThreadExecutionState) 경로는 반쯤 걸쳐 두는 대신 걷어냈다.
// 다른 OS 에는 `caffeinate` 가 없어서 실행이 실패하고 토글은 no-op 이 된다.
private fun startIdleInhibitor(): Process? =
    runCatching { ProcessBuilder("caffeinate", "-di").start() }.getOrNull()
