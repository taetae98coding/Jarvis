package io.github.taetae98coding.jarvis.domain.terminal

import kotlin.test.Test
import kotlin.test.assertEquals

class ClaudeTurnEndsTest {
    private val working = ClaudeStatus(ClaudeActivity.Working, "running tests")
    private val finished = ClaudeStatus(ClaudeActivity.Finished, "tests green")
    private val waiting = ClaudeStatus(ClaudeActivity.WaitingForInput, "awaiting choice")
    private val idle = ClaudeStatus(ClaudeActivity.Idle)

    @Test
    fun workingToFinishedOrWaitingEndsATurn() {
        val ends = claudeTurnEnds(
            previous = mapOf("a" to working, "b" to working),
            current = mapOf("a" to finished, "b" to waiting),
        )

        assertEquals(
            listOf(
                ClaudeTurnEnd("a", ClaudeActivity.Finished, "tests green"),
                ClaudeTurnEnd("b", ClaudeActivity.WaitingForInput, "awaiting choice"),
            ),
            ends,
        )
    }

    @Test
    fun onlyAChangeFromWorkingEndsATurn() {
        val ends = claudeTurnEnds(
            previous = mapOf("same" to finished, "idle" to idle, "waiting" to waiting, "stopping" to working),
            current = mapOf("same" to finished, "idle" to waiting, "waiting" to finished, "stopping" to idle, "new" to finished),
        )

        assertEquals(emptyList(), ends)
    }

    @Test
    fun notificationShowsPanelTabAndSummary() {
        val panel = TerminalPanel(id = 1, name = "Jarvis", root = null, focusedGroupId = null)
        val end = ClaudeTurnEnd("a", ClaudeActivity.Finished, "  tests green \n")

        assertEquals(
            ClaudeNotification("Claude 작업 완료", "Jarvis · 리뷰 — tests green"),
            claudeNotification(end, panel, TerminalTab(2, TerminalProgram.Claude, name = "리뷰")),
        )
        assertEquals(
            ClaudeNotification("Claude 입력 대기", "Jarvis"),
            claudeNotification(end.copy(activity = ClaudeActivity.WaitingForInput, summary = " "), panel, TerminalTab(2)),
        )
    }

    @Test
    fun longSummaryIsCut() {
        val panel = TerminalPanel(id = 1, name = "P", root = null, focusedGroupId = null)
        val end = ClaudeTurnEnd("a", ClaudeActivity.Finished, "x".repeat(500))

        assertEquals("P — " + "x".repeat(200), claudeNotification(end, panel, TerminalTab(2)).message)
    }
}
