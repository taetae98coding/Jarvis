package io.github.taetae98coding.jarvis.data.terminal

import io.github.taetae98coding.jarvis.data.state.observeByPolling
import io.github.taetae98coding.jarvis.domain.terminal.ClaudeActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import java.time.Instant
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

internal actual fun createClaudeActivityDataSource(): ClaudeActivityDataSource =
    JobStateClaudeActivityDataSource(File(claudeConfigDirectory(), "jobs"))

/** 파일 stat 몇십 번이라 싸다. Claude 가 차례를 끝낸 뒤 줄에 보이기까지 이만큼 늦다(docs/platform/jvm.html#terminal-claude-status). */
internal val ClaudeActivityPollInterval: Duration = 1.seconds

/**
 * Claude Code 백그라운드 서비스가 세션마다 쓰는 `<jobs>/<짧은 id>/state.json` 을 읽는다. 공개된 형식이 아니라
 * 2.1.282 에서 확인한 필드만 쓰고, 모르는 파일은 건너뛴다. `claude agents --json` 은 모니터링과 새 결과를
 * 가를 필드가 없어 버렸다(docs/platform/jvm.html#terminal-claude-status).
 */
internal class JobStateClaudeActivityDataSource(
    private val jobs: File,
) : ClaudeActivityDataSource {
    override fun observeActivities(sessionIds: Set<String>): Flow<Map<String, ClaudeActivity>> {
        if (sessionIds.isEmpty()) return flowOf(emptyMap())

        return flow {
            // 수집마다 따로 둔다. 수집이 끝나면 함께 버려진다.
            val cache = JobStateCache(jobs)
            emitAll(observeByPolling(ClaudeActivityPollInterval) { claudeActivities(cache.read(), sessionIds) })
        }.flowOn(Dispatchers.IO)
    }
}

internal data class ClaudeJobState(
    val name: String?,
    val sessionId: String?,
    val state: String?,
    val tempo: String?,
    val hasBackgroundWork: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
) {
    /**
     * `tempo` 는 서비스가 `active`·`idle`·`blocked` 만 받는다. `blocked` 는 Claude 가 사용자 입력을 기다리는 것이라
     * 백그라운드 작업이 있어도 끝난 것으로 본다(docs/common/terminal-claude-status.html#implementation).
     */
    val activity: ClaudeActivity
        get() = when {
            state == "stopped" || state == "failed" -> ClaudeActivity.Finished(updatedAt)
            tempo == "active" -> ClaudeActivity.Working
            tempo == "blocked" -> ClaudeActivity.Finished(updatedAt)
            tempo == null && state == "working" -> ClaudeActivity.Working
            hasBackgroundWork -> ClaudeActivity.Monitoring
            else -> ClaudeActivity.Finished(updatedAt)
        }
}

/**
 * 창 sessionId 마다 그 세션의 기록 하나. 창을 열 때 붙는 규칙(`ClaudeBackground`)과 같이 이름이 `jarvis-<id>` 이거나
 * sessionId 가 창 sessionId 인 기록 중, 서비스가 다시 띄워 여럿이면 `failed` 가 아닌 가장 늦은 것을 쓴다.
 */
internal fun claudeActivities(states: List<ClaudeJobState>, sessionIds: Set<String>): Map<String, ClaudeActivity> =
    sessionIds.mapNotNull { id ->
        val name = claudeJobName(id)
        val candidates = states.filter { it.name == name || it.sessionId == id }
        val job = candidates.filterNot { it.state == "failed" }.maxByOrNull(ClaudeJobState::createdAt)
            ?: candidates.maxByOrNull(ClaudeJobState::createdAt)

        job?.let { id to it.activity }
    }.toMap()

/** 쓰는 도중에 읽어 깨진 파일도 null 이다. 다음 폴링에 다시 읽는다. */
internal fun parseClaudeJobState(text: String): ClaudeJobState? {
    val job = runCatching { Json.parseToJsonElement(text) as? JsonObject }.getOrNull() ?: return null

    fun string(key: String): String? = (job[key] as? JsonPrimitive)?.contentOrNull
    fun time(key: String): Long? = string(key)?.let { runCatching { Instant.parse(it).toEpochMilli() }.getOrNull() }

    val inFlight = job["inFlight"] as? JsonObject
    fun count(key: String): Int = (inFlight?.get(key) as? JsonPrimitive)?.intOrNull ?: 0
    val kinds = (inFlight?.get("kinds") as? JsonArray).orEmpty().mapNotNull { runCatching { it.jsonPrimitive.content }.getOrNull() }

    val createdAt = time("createdAt") ?: 0

    return ClaudeJobState(
        name = string("name"),
        sessionId = string("sessionId"),
        state = string("state"),
        tempo = string("tempo"),
        hasBackgroundWork = count("tasks") > 0 || count("queued") > 0 || inFlight?.get("wake") is JsonObject || "session_cron" in kinds,
        createdAt = createdAt,
        updatedAt = time("updatedAt") ?: createdAt,
    )
}

/** 1초마다 모든 기록을 다시 파싱하지 않게, 수정 시각·크기가 그대로인 파일은 전에 읽은 값을 쓴다. */
private class JobStateCache(private val jobs: File) {
    private class Entry(val modified: Long, val length: Long, val state: ClaudeJobState?)

    private val entries = mutableMapOf<File, Entry>()

    fun read(): List<ClaudeJobState> {
        val files = jobs.listFiles().orEmpty().map { File(it, "state.json") }.filter(File::isFile)
        entries.keys.retainAll(files.toSet())

        return files.mapNotNull { file ->
            val modified = file.lastModified()
            val length = file.length()
            val cached = entries[file]?.takeIf { it.modified == modified && it.length == length }

            (cached ?: Entry(modified, length, runCatching { parseClaudeJobState(file.readText()) }.getOrNull()).also { entries[file] = it }).state
        }
    }
}
