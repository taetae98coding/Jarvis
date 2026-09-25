package io.github.taetae98coding.jarvis.data.terminal

import java.io.File
import java.util.concurrent.TimeUnit

/**
 * 로그인·대화형 셸의 환경 변수. Finder 로 띄운 앱에는 `PATH`·`JAVA_HOME` 이 없어서 언어 서버가 `java`·`gradle` 을 못 찾는다.
 * 셸을 한 번 띄워 `env -0` 를 받아 둔다. 대화형 셸이 먼저 찍는 글과 섞이지 않게 표시 뒤만 읽는다.
 */
internal object LoginShellEnvironment {
    val value: Map<String, String> by lazy { capture()?.let(::terminalEnvironment) ?: terminalEnvironment(System.getenv()) }

    private fun capture(): Map<String, String>? {
        val shell = System.getenv("SHELL")?.takeIf { File(it).canExecute() } ?: "/bin/zsh"
        val output = runQuietly(listOf(shell, "-l", "-i", "-c", "printf '$Marker'; /usr/bin/env -0"), terminalEnvironment(System.getenv()), ShellTimeoutSeconds)
            ?: return null
        if (Marker !in output) return null

        return output.substringAfter(Marker)
            .split('\u0000')
            .filter { '=' in it }
            .associate { it.substringBefore('=') to it.substringAfter('=') }
            .takeIf { "PATH" in it }
    }

    private const val Marker = "__JARVIS_ENV__"

    private const val ShellTimeoutSeconds = 10L
}

/** 표준 출력. 0 이 아닌 코드로 끝났거나 시간 안에 끝나지 않았으면 null 이다. */
internal fun runQuietly(command: List<String>, env: Map<String, String>, timeoutSeconds: Long = 30): String? =
    runCatching {
        // 출력은 파일로 받는다. 셸이 띄운 백그라운드 프로세스가 stdout 을 물려받으면 파이프 읽기가 끝나지 않는다.
        val output = File.createTempFile("jarvis-lsp", ".out")
        try {
            val process = ProcessBuilder(command)
                .redirectOutput(output)
                .redirectError(ProcessBuilder.Redirect.DISCARD)
                .apply {
                    environment().clear()
                    environment().putAll(env)
                }
                .start()
            process.outputStream.close()
            if (!process.waitFor(timeoutSeconds, TimeUnit.SECONDS)) {
                process.destroyForcibly()
                null
            } else if (process.exitValue() != 0) {
                null
            } else {
                output.readText()
            }
        } finally {
            output.delete()
        }
    }.getOrNull()
