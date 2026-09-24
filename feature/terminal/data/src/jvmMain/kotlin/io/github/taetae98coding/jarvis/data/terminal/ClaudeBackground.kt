package io.github.taetae98coding.jarvis.data.terminal

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible
import io.github.taetae98coding.jarvis.automation.AgentServer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.seconds

/**
 * 창 sessionId 에 묶인 Claude Code 백그라운드 세션(`claude --bg`)을 찾고, 만들고, 멈춘다.
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
    private val hasConversation: (sessionId: String) -> Boolean = ::hasClaudeTranscript,
) {
    /** 창의 pty 에 띄울 명령. [sessionId] 에 묶인 세션이 없으면 만든 뒤 붙는다. */
    suspend fun command(sessionId: String, directory: String): List<String> {
        val jobs = findJobs(sessionId)
        jobs.alive()?.let { return interactiveCommand(shell, claudeAttachScript(it.id), thenShell = true) }

        // 대화가 남아 있으면 잇는다. 예전 방식으로 만든 창은 창 sessionId 가, 죽은 세션은 그 sessionId 가 대화다.
        val resume = (listOf(sessionId) + jobs.map(ClaudeJob::sessionId)).distinct().firstOrNull(hasConversation)

        val started = run(claudeStartScript(sessionId, resume), directory)
        val job = if (started.exitCode == 0) findJobs(sessionId).alive()?.id ?: parseBackgroundedId(started.output) else null

        return if (job != null) {
            interactiveCommand(shell, claudeAttachScript(job), thenShell = true)
        } else {
            interactiveCommand(shell, claudeForegroundScript(sessionId, resume, started.output), thenShell = true)
        }
    }

    suspend fun stop(sessionId: String) {
        findJobs(sessionId).filterNot(ClaudeJob::isFailed).forEach { job ->
            run("claude stop ${shellQuote(job.id)}", System.getProperty("user.home"))
        }
    }

    private suspend fun findJobs(sessionId: String): List<ClaudeJob> {
        val listed = run("claude agents --json --all", System.getProperty("user.home"))
        if (listed.exitCode != 0) return emptyList()

        val name = claudeJobName(sessionId)
        return parseClaudeJobs(listed.output).filter { it.name == name || it.sessionId == sessionId }
    }

    private fun List<ClaudeJob>.alive(): ClaudeJob? = filterNot(ClaudeJob::isFailed).maxByOrNull(ClaudeJob::startedAt)
}

internal class ShellResult(val exitCode: Int, val output: String)

internal data class ClaudeJob(
    val id: String,
    val sessionId: String,
    val name: String? = null,
    val state: String? = null,
    val startedAt: Long = 0,
) {
    // 서비스가 세션을 띄우지 못했다. attach 해도 붙을 것이 없다. stopped 는 attach 가 다시 띄운다.
    val isFailed: Boolean get() = state == "failed"
}

/** 창 sessionId 와 Claude 가 정한 실제 세션을 잇는 이름. Claude 입력창에도 보인다. */
internal fun claudeJobName(sessionId: String): String = "jarvis-$sessionId"

/**
 * `--bg` 는 `--session-id` 를 무시하고, 처음 보는 uuid 를 `--resume` 에 주면 세션이 곧 죽는다
 * (2.1.281, docs/platform/jvm.html#terminal-panels). 그래서 새 대화는 Claude 가 sessionId 를 정하게 두고
 * 이름으로 찾는다. [resume] 은 대화 기록이 있는 sessionId 만 준다.
 */
internal fun claudeStartScript(sessionId: String, resume: String?): String =
    "claude --bg --name ${shellQuote(claudeJobName(sessionId))}" +
        resume?.let { " --resume ${shellQuote(it)}" }.orEmpty() +
        mcpConfigArgument(sessionId) +
        " --dangerously-skip-permissions"

// attach 는 짧은 id 만 받는다. 전체 sessionId 를 주면 "No job matching" 이다.
internal fun claudeAttachScript(job: String): String = "claude attach ${shellQuote(job)}"

/**
 * 백그라운드 세션을 만들지 못했을 때(신뢰하지 않은 디렉터리, 로그인 안 됨) 앱 안에서 직접 띄운다.
 * 왜 못 만들었는지 먼저 보여 준다. 이을 대화가 있으면 잇고, 없으면 창 sessionId 로 시작한다 — 그러면
 * 창 sessionId 로 대화 기록이 생겨 다음에 창을 열 때 백그라운드 세션이 그 대화를 잇는다.
 */
internal fun claudeForegroundScript(sessionId: String, resume: String?, reason: String): String {
    val message = reason.trim().takeIf { it.isNotEmpty() }?.let { "printf '%s\\n\\n' ${shellQuote(it)}; " }.orEmpty()
    val launch = if (resume != null) "--resume ${shellQuote(resume)}" else "--session-id ${shellQuote(sessionId)}"

    return "${message}claude${mcpConfigArgument(sessionId)} --dangerously-skip-permissions $launch"
}

/**
 * 세션이 앱의 MCP 서버에 붙게 한다. 헤더의 창 sessionId 로 서버가 어느 탭의 Claude 인지 안다
 * (docs/common/mcp-server.html R2·R3). `--mcp-config` 는 값을 여럿 받으므로 바로 뒤에 옵션이 와야 다음 낱말을 먹지 않는다.
 */
private fun mcpConfigArgument(sessionId: String): String = " --mcp-config ${shellQuote(jarvisMcpConfig(sessionId))}"

internal fun jarvisMcpConfig(sessionId: String): String =
    buildJsonObject {
        putJsonObject("mcpServers") {
            putJsonObject(AgentServer.Name) {
                put("type", "http")
                put("url", AgentServer.Url)
                putJsonObject("headers") { put(AgentServer.SessionHeader, sessionId) }
            }
        }
    }.toString()

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
        ClaudeJob(
            id = id,
            sessionId = sessionId,
            name = job["name"]?.jsonPrimitive?.contentOrNull,
            state = job["state"]?.jsonPrimitive?.contentOrNull,
            startedAt = job["startedAt"]?.jsonPrimitive?.longOrNull ?: 0,
        )
    }
}

/** `claude --bg` 가 찍는 `backgrounded · <id> (…)`. `agents --json` 으로 찾지 못했을 때만 쓴다. */
internal fun parseBackgroundedId(output: String): String? =
    Regex("""backgrounded · ([0-9a-f]+)""").find(output)?.groupValues?.get(1)

/**
 * Claude Code 는 대화를 `<설정 디렉터리>/projects/<cwd 를 바꾼 이름>/<sessionId>.jsonl` 에 쓴다. cwd 를
 * 이름으로 바꾸는 규칙을 따라 하지 않고 모든 프로젝트 디렉터리를 본다.
 */
private fun hasClaudeTranscript(sessionId: String): Boolean {
    val config = System.getenv("CLAUDE_CONFIG_DIR")?.let(::File) ?: File(System.getProperty("user.home"), ".claude")

    return File(config, "projects").listFiles().orEmpty().any { File(it, "$sessionId.jsonl").isFile }
}

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
