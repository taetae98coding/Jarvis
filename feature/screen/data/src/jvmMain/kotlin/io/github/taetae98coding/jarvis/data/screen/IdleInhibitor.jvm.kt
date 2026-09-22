package io.github.taetae98coding.jarvis.data.screen

internal actual fun createIdleInhibitor(): IdleInhibitor = CaffeinateIdleInhibitor()

/**
 * JVM 에는 이식 가능한 idle-inhibit API 가 없어서, 유휴 타이머를 관리하는 OS 도구에 위임한다.
 *
 * 데스크탑은 macOS 만 지원하기로 했으므로 Linux(systemd-inhibit)와 Windows(JNA 를 통한
 * SetThreadExecutionState) 경로는 반쯤 걸쳐 두는 대신 걷어냈다. 다른 OS 에는 `caffeinate` 가 없어서
 * 실행이 실패하고 토글은 no-op 이 된다.
 */
private class CaffeinateIdleInhibitor : IdleInhibitor {
    private var process: Process? = null

    // 자식 프로세스는 JVM 이 죽어도 살아남는다. caffeinate 가 남으면 앱이 없는데도 화면이 계속
    // 켜져 있으므로, 정상 종료와 SIGTERM 까지는 훅으로 막는다. SIGKILL 은 훅도 돌지 않는다.
    private val hook = Thread { process?.destroy() }
        .also(Runtime.getRuntime()::addShutdownHook)

    override fun setEnabled(enabled: Boolean) {
        if (enabled) start() else stop()
    }

    private fun start() {
        if (process?.isAlive == true) return

        process = runCatching { ProcessBuilder("caffeinate", "-di").start() }.getOrNull()
    }

    private fun stop() {
        process?.destroy()
        process = null
    }
}
