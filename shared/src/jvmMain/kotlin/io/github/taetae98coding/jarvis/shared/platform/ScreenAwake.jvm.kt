package io.github.taetae98coding.jarvis.shared.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect

@Composable
internal actual fun PlatformIdleInhibitor(enabled: Boolean) {
    DisposableEffect(enabled) {
        val inhibitor = if (enabled) startIdleInhibitor() else null
        onDispose { inhibitor?.stop() }
    }
}

// JVM 에는 이식 가능한 idle-inhibit API 가 없어서, 유휴 타이머를 관리하는 OS 도구에 위임한다.
// 데스크탑은 macOS 만 지원하기로 했으므로 Linux(systemd-inhibit)와 Windows(JNA 를 통한
// SetThreadExecutionState) 경로는 반쯤 걸쳐 두는 대신 걷어냈다.
// 다른 OS 에는 `caffeinate` 가 없어서 실행이 실패하고 토글은 no-op 이 된다.
private fun startIdleInhibitor(): IdleInhibitor? =
    runCatching { IdleInhibitor(ProcessBuilder("caffeinate", "-di").start()) }.getOrNull()

private class IdleInhibitor(private val process: Process) {
    // 자식 프로세스는 JVM 이 죽어도 살아남는다. caffeinate 가 남으면 앱이 없는데도 화면이 계속
    // 켜져 있으므로, 정상 종료와 SIGTERM 까지는 훅으로 막는다. SIGKILL 은 훅도 돌지 않는다.
    private val hook = Thread { process.destroy() }
        .also(Runtime.getRuntime()::addShutdownHook)

    fun stop() {
        // 종료가 이미 시작됐으면 훅을 뗄 수 없다. 그 경우 훅이 대신 프로세스를 죽인다.
        runCatching { Runtime.getRuntime().removeShutdownHook(hook) }
        process.destroy()
    }
}
