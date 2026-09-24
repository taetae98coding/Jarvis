package io.github.taetae98coding.jarvis.domain.terminal

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class NotifyClaudeTurnEndsUseCaseTest {
    // 패널 "Jarvis" 에 셸 탭 하나와 Claude 탭 둘("session-a" 는 이름 "리뷰").
    private val workspace = TerminalWorkspace.initial()
        .renamePanel(1, "Jarvis")
        .addTab(program = TerminalProgram.Claude, claudeSessionId = "session-a")
        .let { it.renameTab(it.tabs.last().id, "리뷰") }
        .addTab(program = TerminalProgram.Claude, claudeSessionId = "session-b")

    private val working = ClaudeActivity.Working

    private val activities = RecordingClaudeActivityRepository()

    private fun TestScope.start(
        repository: RecordingTerminalRepository,
        isWatching: (String) -> Boolean = { false },
    ) {
        backgroundScope.launch {
            NotifyClaudeTurnEndsUseCase(activities, InMemoryTerminalWorkspaceRepository(workspace), repository)(isWatching)
        }
        runCurrent()
    }

    @Test
    fun theFirstStatusesAreNotNotified() = runTest {
        val repository = RecordingTerminalRepository()
        activities.activities.value = mapOf("session-a" to ClaudeActivity.Finished(10, summary = "done before"))

        start(repository)

        assertEquals(emptyList(), repository.notifications)
    }

    @Test
    fun aFinishedTurnIsNotified() = runTest {
        val repository = RecordingTerminalRepository()
        activities.activities.value = mapOf("session-a" to working, "session-b" to working)
        start(repository)

        activities.activities.value = mapOf(
            "session-a" to ClaudeActivity.Finished(20, summary = "tests green"),
            "session-b" to ClaudeActivity.Finished(20, needsInput = true, summary = "pick one"),
        )
        runCurrent()

        assertEquals(
            listOf(
                ClaudeNotification("Claude 작업 완료", "Jarvis · 리뷰 — tests green"),
                ClaudeNotification("Claude 입력 대기", "Jarvis — pick one"),
            ),
            repository.notifications,
        )
    }

    @Test
    fun aWatchedTabIsNotNotified() = runTest {
        val repository = RecordingTerminalRepository()
        activities.activities.value = mapOf("session-a" to working, "session-b" to working)
        start(repository, isWatching = { it == "session-a" })

        activities.activities.value = mapOf(
            "session-a" to ClaudeActivity.Finished(20),
            "session-b" to ClaudeActivity.Finished(20),
        )
        runCurrent()

        assertEquals(listOf(ClaudeNotification("Claude 작업 완료", "Jarvis")), repository.notifications)
    }

    @Test
    fun nothingIsObservedWhereClaudeIsNotSupported() = runTest {
        val repository = RecordingTerminalRepository(isClaudeSupported = false)
        activities.activities.value = mapOf("session-a" to working)
        start(repository)

        activities.activities.value = mapOf("session-a" to ClaudeActivity.Finished(20))
        runCurrent()

        assertEquals(emptyList(), repository.notifications)
    }

    @Test
    fun aSessionOutsideTheWorkspaceIsNotNotified() = runTest {
        val repository = RecordingTerminalRepository()
        activities.activities.value = mapOf("elsewhere" to working)
        start(repository)

        activities.activities.value = mapOf("elsewhere" to ClaudeActivity.Finished(20))
        runCurrent()

        assertEquals(emptyList(), repository.notifications)
    }
}
