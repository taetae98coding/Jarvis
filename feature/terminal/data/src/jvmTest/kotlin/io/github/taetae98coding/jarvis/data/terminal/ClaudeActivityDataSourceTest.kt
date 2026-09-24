package io.github.taetae98coding.jarvis.data.terminal

import io.github.taetae98coding.jarvis.domain.terminal.ClaudeActivity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** 임시 `jobs` 디렉터리에 Claude Code 2.1.282 가 쓰는 모양의 `state.json` 을 두고 읽는다. */
class ClaudeActivityDataSourceTest {
    private val jobs: File = Files.createTempDirectory("jarvis-jobs").toFile()

    private val source = JobStateClaudeActivityDataSource(jobs)

    private fun job(
        short: String,
        windowSessionId: String?,
        state: String,
        tempo: String? = null,
        inFlight: String? = null,
        detail: String? = null,
        createdAt: String = "2026-09-24T16:00:00.000Z",
        updatedAt: String = "2026-09-24T16:53:21.608Z",
    ) {
        val fields = listOfNotNull(
            "\"state\": \"$state\"",
            tempo?.let { "\"tempo\": \"$it\"" },
            inFlight?.let { "\"inFlight\": $it" },
            detail?.let { "\"detail\": \"$it\"" },
            windowSessionId?.let { "\"name\": \"${claudeJobName(it)}\"" },
            "\"sessionId\": \"$short-aa49-470a-aa43-2468b9373e0e\"",
            "\"createdAt\": \"$createdAt\"",
            "\"updatedAt\": \"$updatedAt\"",
        )
        File(jobs, short).apply { mkdirs() }.resolve("state.json").writeText(fields.joinToString(",\n", "{\n", "\n}"))
    }

    private suspend fun activity(windowSessionId: String): ClaudeActivity? =
        source.observeActivities(setOf(windowSessionId)).first()[windowSessionId]

    @Test
    fun anActiveTurnIsWorking() = runTest {
        job("81cd6cde", "w", state = "working", tempo = "active", inFlight = """{"tasks": 2, "queued": 0, "kinds": []}""")

        assertEquals(ClaudeActivity.Working, activity("w"))
    }

    @Test
    fun anIdleTurnWithBackgroundWorkIsMonitoring() = runTest {
        job("a", "tasks", state = "working", tempo = "idle", inFlight = """{"tasks": 1, "queued": 0, "kinds": ["monitor"]}""")
        job("b", "wake", state = "working", tempo = "idle", inFlight = """{"tasks": 0, "queued": 0, "kinds": [], "wake": {"at": 1, "fires": 0}}""")
        job("c", "cron", state = "working", tempo = "idle", inFlight = """{"tasks": 0, "queued": 0, "kinds": ["session_cron"]}""")

        val activities = source.observeActivities(setOf("tasks", "wake", "cron")).first()

        assertEquals(mapOf("tasks" to ClaudeActivity.Monitoring, "wake" to ClaudeActivity.Monitoring, "cron" to ClaudeActivity.Monitoring), activities)
    }

    @Test
    fun aBlockedTurnIsFinishedEvenWithBackgroundWork() = runTest {
        job("9b0bd7f8", "w", state = "blocked", tempo = "blocked", inFlight = """{"tasks": 1, "queued": 0, "kinds": []}""")

        assertEquals(ClaudeActivity.Finished(at = 1_790_268_801_608, needsInput = true), activity("w"))
    }

    @Test
    fun aFinishedTurnCarriesWhetherItNeedsInputAndItsSummary() = runTest {
        job("a", "done", state = "done", tempo = "idle", detail = "merged to local main; tests green")
        job("b", "asked", state = "blocked", tempo = "blocked", detail = "awaiting engine choice")
        // 턴 도중 선택을 기다리면 state 는 working 그대로이고 tempo 만 blocked 다.
        job("c", "choosing", state = "working", tempo = "blocked", detail = " ")

        val activities = source.observeActivities(setOf("done", "asked", "choosing")).first()

        assertEquals(ClaudeActivity.Finished(1_790_268_801_608, needsInput = false, summary = "merged to local main; tests green"), activities["done"])
        assertEquals(ClaudeActivity.Finished(1_790_268_801_608, needsInput = true, summary = "awaiting engine choice"), activities["asked"])
        assertEquals(ClaudeActivity.Finished(1_790_268_801_608, needsInput = true), activities["choosing"])
    }

    @Test
    fun doneStoppedAndFailedAreFinished() = runTest {
        job("a", "done", state = "done", tempo = "idle", inFlight = """{"tasks": 0, "queued": 0, "kinds": []}""")
        job("b", "stopped", state = "stopped", tempo = "idle", inFlight = """{"tasks": 3, "queued": 0, "kinds": []}""")
        job("c", "failed", state = "failed", tempo = "active")

        val activities = source.observeActivities(setOf("done", "stopped", "failed")).first()

        assertTrue(activities.values.all { it is ClaudeActivity.Finished }, "$activities")
        assertEquals(3, activities.size)
    }

    @Test
    fun aRecordWithoutTempoIsWorkingWhileItsStateIsWorking() = runTest {
        job("a", "w", state = "working")

        assertEquals(ClaudeActivity.Working, activity("w"))
    }

    @Test
    fun theLatestLiveRecordOfAWindowWins() = runTest {
        job("old", "w", state = "stopped", createdAt = "2026-09-24T10:00:00.000Z")
        job("new", "w", state = "working", tempo = "active", createdAt = "2026-09-24T12:00:00.000Z")
        job("dead", "w", state = "failed", createdAt = "2026-09-24T14:00:00.000Z")

        assertEquals(ClaudeActivity.Working, activity("w"))
    }

    @Test
    fun anUnnamedRecordIsFoundByItsSessionId() = runTest {
        job("d2605cd1", windowSessionId = null, state = "working", tempo = "active")

        assertEquals(ClaudeActivity.Working, activity("d2605cd1-aa49-470a-aa43-2468b9373e0e"))
    }

    @Test
    fun unknownWindowsBrokenFilesAndMissingDirectoriesAreLeftOut() = runTest {
        File(jobs, "broken").apply { mkdirs() }.resolve("state.json").writeText("{\"state\": \"work")
        job("a", "known", state = "working", tempo = "active")

        assertEquals(mapOf("known" to ClaudeActivity.Working), source.observeActivities(setOf("known", "unknown")).first())
        assertEquals(emptyMap(), JobStateClaudeActivityDataSource(File(jobs, "missing")).observeActivities(setOf("known")).first())
        assertEquals(emptyMap(), source.observeActivities(emptySet()).first())
    }

    @Test
    fun parsingKeepsOnlyTheFieldsItUnderstands() {
        assertNull(parseClaudeJobState("not json"))
        assertNull(parseClaudeJobState("[]"))

        val state = parseClaudeJobState("""{"state": "working", "tempo": "idle", "inFlight": {"tasks": "many"}, "updatedAt": "yesterday"}""")!!

        assertEquals(false, state.hasBackgroundWork)
        assertEquals(0, state.updatedAt)
    }
}
