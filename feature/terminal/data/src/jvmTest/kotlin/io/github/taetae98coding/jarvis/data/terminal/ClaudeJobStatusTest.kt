package io.github.taetae98coding.jarvis.data.terminal

import io.github.taetae98coding.jarvis.domain.terminal.ClaudeActivity
import io.github.taetae98coding.jarvis.domain.terminal.ClaudeStatus
import java.io.File
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class ClaudeJobStatusTest {
    private val jobs = Files.createTempDirectory("jarvis-claude-jobs").toFile()

    @AfterTest
    fun cleanUp() {
        jobs.deleteRecursively()
    }

    private fun state(
        name: String? = "jarvis-tab",
        state: String = "working",
        tempo: String = "active",
        detail: String? = null,
        updatedAt: String = "2026-09-24T17:00:00.000Z",
    ): String =
        buildString {
            append("""{"state":"$state","tempo":"$tempo","updatedAt":"$updatedAt"""")
            name?.let { append(""","name":"$it"""") }
            detail?.let { append(""","detail":"$it"""") }
            append("}")
        }

    private fun write(job: String, text: String, modified: Long? = null) {
        val file = File(jobs, "$job/state.json")
        file.parentFile.mkdirs()
        file.writeText(text)
        modified?.let(file::setLastModified)
    }

    @Test
    fun stateWordsBecomeActivities() {
        fun activity(state: String, tempo: String = "active") = parseClaudeJobState(state(state = state, tempo = tempo))?.status?.activity

        assertEquals(ClaudeActivity.Working, activity("working"))
        assertEquals(ClaudeActivity.Finished, activity("done", tempo = "idle"))
        assertEquals(ClaudeActivity.WaitingForInput, activity("blocked"))
        // 첫 프롬프트나 턴 도중의 선택을 기다릴 때는 state 가 working 그대로다.
        assertEquals(ClaudeActivity.WaitingForInput, activity("working", tempo = "blocked"))
        assertEquals(ClaudeActivity.Idle, activity("stopped", tempo = "idle"))
        assertEquals(ClaudeActivity.Idle, activity("failed", tempo = "idle"))
    }

    @Test
    fun onlyJarvisSessionsAreRead() {
        assertEquals("abc", parseClaudeJobState(state(name = "jarvis-abc"))?.tabSessionId)
        assertNull(parseClaudeJobState(state(name = "diarykmp-91")))
        assertNull(parseClaudeJobState(state(name = null)))
        assertFailsWith<Exception> { parseClaudeJobState("""{"state":"wor""") }
    }

    @Test
    fun readerMapsTabsToTheirLatestSession() {
        write("old", state(state = "stopped", updatedAt = "2026-09-24T10:00:00.000Z"))
        write("new", state(state = "done", detail = "tests green", updatedAt = "2026-09-24T11:00:00.000Z"))
        write("other", state(name = "jarvis-other", state = "working"))
        write("outside", state(name = "macro-d9", state = "done"))
        File(jobs, "empty").mkdirs()

        assertEquals(
            mapOf(
                "tab" to ClaudeStatus(ClaudeActivity.Finished, "tests green"),
                "other" to ClaudeStatus(ClaudeActivity.Working),
            ),
            ClaudeJobsReader(jobs).read(),
        )
    }

    @Test
    fun aHalfWrittenFileKeepsThePreviousStatus() {
        val reader = ClaudeJobsReader(jobs)
        write("job", state(state = "working"), modified = 1_000_000)
        assertEquals(mapOf("tab" to ClaudeStatus(ClaudeActivity.Working)), reader.read())

        write("job", """{"state":"do""", modified = 2_000_000)
        assertEquals(mapOf("tab" to ClaudeStatus(ClaudeActivity.Working)), reader.read())

        write("job", state(state = "done"), modified = 3_000_000)
        assertEquals(mapOf("tab" to ClaudeStatus(ClaudeActivity.Finished)), reader.read())
    }

    @Test
    fun missingDirectoryIsEmpty() {
        assertEquals(emptyMap(), ClaudeJobsReader(File(jobs, "missing")).read())
    }
}
