package io.github.taetae98coding.jarvis.domain.terminal

import kotlin.test.Test
import kotlin.test.assertEquals

class ClaudeTurnEndsTest {
    private val finished = ClaudeActivity.Finished(at = 20, summary = "tests green")
    private val asked = ClaudeActivity.Finished(at = 30, needsInput = true, summary = "awaiting choice")

    @Test
    fun workingOrMonitoringToFinishedEndsATurn() {
        val ends = claudeTurnEnds(
            previous = mapOf("a" to ClaudeActivity.Working, "b" to ClaudeActivity.Monitoring),
            current = mapOf("a" to finished, "b" to asked),
        )

        assertEquals(listOf(ClaudeTurnEnd("a", finished), ClaudeTurnEnd("b", asked)), ends)
    }

    @Test
    fun otherChangesDoNotEndATurn() {
        val ends = claudeTurnEnds(
            previous = mapOf("same" to finished, "again" to finished, "monitor" to ClaudeActivity.Working),
            current = mapOf("same" to finished, "again" to asked, "monitor" to ClaudeActivity.Monitoring, "new" to finished),
        )

        assertEquals(emptyList(), ends)
    }

    @Test
    fun notificationShowsPanelTabAndSummary() {
        val panel = TerminalPanel(id = 1, name = "Jarvis", root = null, focusedGroupId = null)

        assertEquals(
            ClaudeNotification("Claude 작업 완료", "Jarvis · 리뷰 — tests green"),
            claudeNotification(ClaudeTurnEnd("a", finished.copy(summary = "  tests green \n")), panel, TerminalTab(2, TerminalProgram.Claude, name = "리뷰")),
        )
        assertEquals(
            ClaudeNotification("Claude 입력 대기", "Jarvis"),
            claudeNotification(ClaudeTurnEnd("a", asked.copy(summary = " ")), panel, TerminalTab(2)),
        )
    }

    @Test
    fun longSummaryIsCut() {
        val panel = TerminalPanel(id = 1, name = "P", root = null, focusedGroupId = null)
        val end = ClaudeTurnEnd("a", finished.copy(summary = "x".repeat(500)))

        assertEquals("P — " + "x".repeat(200), claudeNotification(end, panel, TerminalTab(2)).message)
    }
}
