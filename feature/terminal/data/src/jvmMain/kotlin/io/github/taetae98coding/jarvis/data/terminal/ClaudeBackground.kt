package io.github.taetae98coding.jarvis.data.terminal

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.seconds

/**
 * Claude Code 의 백그라운드 세션(`claude --bg`)을 sessionId 로 찾고, 만들고, 멈춘다.
 *
 * 창을 닫아도 앱을 끝내도 Claude 가 일을 계속하려면 앱 밖의 무언가가 Claude 를 붙잡고 있어야 한다.
 * 그 일을 Claude Code 의 백그라운드 서비스에 맡기고, 창은 `claude attach` 로 붙기만 한다. 아래 동작은
 * 2.1.281 에서 직접 확인한 것이다(docs/platform/jvm.html#terminal-panels).
 */
internal class ClaudeBackground(
    private val shell: String,
    private val run: suspend (script: String, directory: String) -> ShellResult = { script, directory ->
        runShell(shell, script, directory)
    },
) {
    /** 창의 pty 에 띄울 명령. 세션이 없으면 [sessionId] 로 만든 뒤 붙는다. */
    suspend fun command(sessionId: String, directory: String): List<String> {
        findJob(sessionId)?.let { return interactiveCommand(shell, claudeAttachScript(it), thenShell = true) }

        // --bg 는 --session-id 를 무시하지만, 처음 보는 uuid 를 --resume 에 주면 그 uuid 로 새 세션을 만든다.
        val started = run(claudeStartScript(sessionId), directory)
        val job = if (started.exitCode == 0) findJob(sessionId) ?: parseBackgroundedId(started.output) else null

        return if (job != null) {
            interactiveCommand(shell, claudeAttachScript(job), thenShell = true)
        } else {
            interactiveCommand(shell, claudeForegroundScript(sessionId, started.output), thenShell = true)
        }
    }

    suspend fun stop(sessionId: String) {
        val job = findJob(sessionId) ?: return

        run("claude stop ${shellQuote(job)}", System.getProperty("user.home"))
    }

    private suspend fun findJob(sessionId: String): String? {
        val listed = run("claude agents --json --all", System.getProperty("user.home"))
        if (listed.exitCode != 0) return null

        return parseClaudeJobs(listed.output).firstOrNull { it.sessionId == sessionId }?.id
    }
}

internal class ShellResult(val exitCode: Int, val output: String)

internal data class ClaudeJob(val id: String, val sessionId: String)

internal fun claudeStartScript(sessionId: String): String =
    "claude --bg --resume ${shellQuote(sessionId)} --dangerously-skip-permissions"

// attach 는 짧은 id 만 받는다. 전체 sessionId 를 주면 "No job matching" 이다.
internal fun claudeAttachScript(job: String): String = "claude attach ${shellQuote(job)}"

/**
 * 백그라운드 세션을 만들지 못했을 때(신뢰하지 않은 디렉터리, 로그인 안 됨) 앱 안에서 직접 띄운다.
 * 왜 못 만들었는지 먼저 보여 준다. 대화가 이미 있으면 이어 가고, 없으면 같은 sessionId 로 시작한다 —
 * 다음에 창을 열 때 그 sessionId 로 백그라운드 세션을 만들 수 있게.
 */
internal fun claudeForegroundScript(sessionId: String, reason: String): String {
    val id = shellQuote(sessionId)
    val message = reason.trim().takeIf { it.isNotEmpty() }?.let { "printf '%s\\n\\n' ${shellQuote(it)}; " }.orEmpty()

    return message +
        "claude --dangerously-skip-permissions --resume $id || claude --dangerously-skip-permissions --session-id $id"
}

/**
 * `claude agents --json --all` 의 출력. 셸 설정이 앞에 글자를 찍을 수 있어서 `[` 로 시작하는 줄부터 읽는다.
 * 앱 안에서 직접 띄운 세션(`kind: interactive`)은 attach 할 수 없어 뺀다.
 */
internal fun parseClaudeJobs(output: String): List<ClaudeJob> {
    val start = Regex("""^\s*\[""", RegexOption.MULTILINE).find(output)?.range?.first ?: return emptyList()

    val array = runCatching { Json.parseToJsonElement(output.substring(start)) as? JsonArray }.getOrNull() ?: return emptyList()

    return array.mapNotNull { element ->
        val job = element as? JsonObject ?: return@mapNotNull null
        if (job["kind"]?.jsonPrimitive?.content != "background") return@mapNotNull null

        val id = job["id"]?.jsonPrimitive?.content ?: return@mapNotNull null
        val sessionId = job["sessionId"]?.jsonPrimitive?.content ?: return@mapNotNull null
        ClaudeJob(id, sessionId)
    }
}

/** `claude --bg` 가 찍는 `backgrounded · <id> (…)`. `agents --json` 으로 찾지 못했을 때만 쓴다. */
internal fun parseBackgroundedId(output: String): String? =
    Regex("""backgrounded · ([0-9a-f]+)""").find(output)?.groupValues?.get(1)

/**
 * 출력은 파이프가 아니라 파일로 받는다. `claude --bg` 가 처음 띄우는 백그라운드 서비스가 표준 출력을
 * 물려받아 붙잡으면, 파이프는 명령이 끝나도 EOF 가 오지 않는다.
 */
private suspend fun runShell(shell: String, script: String, directory: String): ShellResult =
    runInterruptible(Dispatchers.IO) {
        val output = File.createTempFile("jarvis-claude", ".out")
        try {
            val process = ProcessBuilder(interactiveCommand(shell, script, thenShell = false))
                .directory(File(directory))
                .redirectErrorStream(true)
                .redirectOutput(output)
                .redirectInput(ProcessBuilder.Redirect.from(File("/dev/null")))
                .apply {
                    environment().clear()
                    environment().putAll(terminalEnvironment(System.getenv()))
                }
                .start()

            if (!process.waitFor(ClaudeCommandTimeout.inWholeSeconds, TimeUnit.SECONDS)) {
                process.destroyForcibly()
                return@runInterruptible ShellResult(exitCode = -1, output = output.readText())
            }

            ShellResult(process.exitValue(), output.readText())
        } catch (e: java.io.IOException) {
            ShellResult(exitCode = -1, output = e.message.orEmpty())
        } finally {
            output.delete()
        }
    }

// 로그인·대화형 셸을 띄우고 claude 가 서비스를 깨우는 데 몇 초가 걸린다. 넘으면 멈춘 것으로 본다.
private val ClaudeCommandTimeout = 30.seconds
