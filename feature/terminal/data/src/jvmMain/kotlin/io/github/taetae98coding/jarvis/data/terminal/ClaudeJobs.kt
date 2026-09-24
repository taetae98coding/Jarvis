package io.github.taetae98coding.jarvis.data.terminal

import io.github.taetae98coding.jarvis.data.state.observeByPolling
import io.github.taetae98coding.jarvis.domain.terminal.ClaudeActivity
import io.github.taetae98coding.jarvis.domain.terminal.ClaudeStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import java.io.File
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * 백그라운드 서비스는 `state.json` 을 고쳐 쓸 뿐 알려 주지 않고, macOS 의 WatchService 도 폴링 구현이다.
 * 버린 후보는 docs/platform/jvm.html#claude-notification 에 있다.
 */
internal val ClaudeStatusPollInterval: Duration = 1.seconds

/** Jarvis 탭의 백그라운드 세션마다 지금 상태. 수집마다 [ClaudeJobsReader] 를 새로 만들어 캐시가 수집과 함께 사라진다. */
internal fun observeClaudeStatuses(jobsDirectory: File): Flow<Map<String, ClaudeStatus>> =
    flow {
        val reader = ClaudeJobsReader(jobsDirectory)
        emitAll(observeByPolling(interval = ClaudeStatusPollInterval) { reader.read() })
    }.flowOn(Dispatchers.IO)

internal data class ClaudeJobState(
    val tabSessionId: String,
    val status: ClaudeStatus,
    val updatedAt: String,
)

/** 서비스가 세션마다 두는 `<jobsDirectory>/<짧은 id>/state.json` 을 읽는다. 키 이름은 Claude Code 2.1.282 기준이다. */
internal class ClaudeJobsReader(private val jobsDirectory: File) {
    private class Cached(val modified: Long, val job: ClaudeJobState?)

    private val cache = mutableMapOf<File, Cached>()

    fun read(): Map<String, ClaudeStatus> {
        val files = jobsDirectory.listFiles().orEmpty().map { File(it, "state.json") }.filter { it.isFile }
        cache.keys.retainAll(files.toSet())

        return files.mapNotNull(::readJob)
            .groupBy { it.tabSessionId }
            // 탭 하나에 세션이 여럿이면(죽은 세션을 새로 만든 경우) 최근에 쓴 것이 지금 붙은 세션이다.
            .mapValues { (_, jobs) -> jobs.maxBy { it.updatedAt }.status }
    }

    private fun readJob(file: File): ClaudeJobState? {
        val modified = file.lastModified()
        val cached = cache[file]
        if (cached != null && cached.modified == modified) return cached.job

        // 서비스가 쓰는 도중에 읽으면 JSON 이 깨져 있다. 앞 값을 두고, 시각을 남기지 않아 다음 번에 다시 읽는다.
        val parsed = runCatching { parseClaudeJobState(file.readText()) }.getOrElse { return cached?.job }
        cache[file] = Cached(modified, parsed)

        return parsed
    }
}

/** Jarvis 가 만든 세션이 아니면 null, JSON 이 깨져 있으면 예외다. */
internal fun parseClaudeJobState(text: String): ClaudeJobState? {
    val job = Json.parseToJsonElement(text) as? JsonObject ?: return null
    val tabSessionId = job.string("name")?.let(::claudeTabSessionId) ?: return null
    val state = job.string("state")
    // 턴 도중 선택을 기다리거나 첫 프롬프트를 기다릴 때는 state 가 working 그대로이고 tempo 만 blocked 다.
    val activity = when {
        state == "done" -> ClaudeActivity.Finished
        state == "blocked" || job.string("tempo") == "blocked" -> ClaudeActivity.WaitingForInput
        state == "working" -> ClaudeActivity.Working
        else -> ClaudeActivity.Idle
    }

    return ClaudeJobState(
        tabSessionId = tabSessionId,
        status = ClaudeStatus(activity, job.string("detail")?.takeIf { it.isNotBlank() }),
        updatedAt = job.string("updatedAt").orEmpty(),
    )
}

private fun JsonObject.string(key: String): String? = (get(key) as? JsonPrimitive)?.contentOrNull
