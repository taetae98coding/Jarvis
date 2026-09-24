package io.github.taetae98coding.jarvis.domain.terminal

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame

class ClaudeStatusTest {
    private fun TerminalWorkspace.addClaude(sessionId: String): TerminalWorkspace =
        addTab(program = TerminalProgram.Claude, claudeSessionId = sessionId)

    private fun TerminalWorkspace.tab(sessionId: String): TerminalTab = tabs.single { it.claudeSessionId == sessionId }

    @Test
    fun aFinishedResultIsAwaitingUntilCheckedAtOrAfterIt() {
        val finished = ClaudeActivity.Finished(at = 100)

        assertEquals(ClaudeStatus.AwaitingReply, finished.status(checkedAt = null))
        assertEquals(ClaudeStatus.AwaitingReply, finished.status(checkedAt = 99))
        assertEquals(ClaudeStatus.Checked, finished.status(checkedAt = 100))
        assertEquals(ClaudeStatus.Working, ClaudeActivity.Working.status(checkedAt = 100))
        assertEquals(ClaudeStatus.Monitoring, ClaudeActivity.Monitoring.status(checkedAt = 100))
    }

    @Test
    fun aPanelShowsItsMostUrgentClaudeTab() {
        val workspace = TerminalWorkspace.initial().addClaude("a").addClaude("b").addClaude("c")
        val panel = workspace.panels.single()

        assertNull(panel.claudeStatus(emptyMap()))
        assertEquals(ClaudeStatus.Monitoring, panel.claudeStatus(mapOf("a" to ClaudeActivity.Monitoring)))
        assertEquals(
            ClaudeStatus.Working,
            panel.claudeStatus(mapOf("a" to ClaudeActivity.Monitoring, "b" to ClaudeActivity.Working)),
        )
        assertEquals(
            ClaudeStatus.AwaitingReply,
            panel.claudeStatus(
                mapOf("a" to ClaudeActivity.Monitoring, "b" to ClaudeActivity.Working, "c" to ClaudeActivity.Finished(1)),
            ),
        )
    }

    @Test
    fun aWorktreePanelIsNotCountedInItsParent() {
        val parent = TerminalWorkspace.initial().addClaude("parent")
        val workspace = parent.addWorktreePanel(parent.panels.single().id, directory = "/w").addClaude("child")
        val activities = mapOf("parent" to ClaudeActivity.Working, "child" to ClaudeActivity.Finished(1))

        assertEquals(ClaudeStatus.Working, workspace.panels[0].claudeStatus(activities))
        assertEquals(ClaudeStatus.AwaitingReply, workspace.panels[1].claudeStatus(activities))
        assertEquals(setOf("parent", "child"), workspace.claudeSessionIds)
    }

    @Test
    fun onlyVisibleFinishedTabsAreChecked() {
        val first = TerminalWorkspace.initial().addClaude("hidden").addClaude("shown")
        val workspace = first.addPanel().addClaude("other panel").selectPanel(first.panels.single().id)
        val activities = mapOf(
            "hidden" to ClaudeActivity.Finished(10),
            "shown" to ClaudeActivity.Finished(20),
            "other panel" to ClaudeActivity.Finished(30),
        )

        val checked = workspace.checkVisibleClaudeTabs(activities)

        assertNull(checked.tab("hidden").claudeCheckedAt)
        assertEquals(20, checked.tab("shown").claudeCheckedAt)
        assertNull(checked.tab("other panel").claudeCheckedAt)
    }

    @Test
    fun checkingTheSameResultAgainChangesNothing() {
        val workspace = TerminalWorkspace.initial().addClaude("a")

        val checked = workspace.checkVisibleClaudeTabs(mapOf("a" to ClaudeActivity.Finished(20)))

        assertSame(checked, checked.checkVisibleClaudeTabs(mapOf("a" to ClaudeActivity.Finished(20))))
        assertSame(checked, checked.checkVisibleClaudeTabs(mapOf("a" to ClaudeActivity.Working)))
        assertSame(workspace, workspace.checkVisibleClaudeTabs(emptyMap()))
    }

    @Test
    fun aNewResultAfterCheckingIsAwaitingAgain() {
        val workspace = TerminalWorkspace.initial().addClaude("a")
            .checkVisibleClaudeTabs(mapOf("a" to ClaudeActivity.Finished(20)))
            .let { it.selectTab(it.addTab().tabs.last().id) }
        val panel = workspace.panels.single()

        assertEquals(ClaudeStatus.Checked, panel.claudeStatus(mapOf("a" to ClaudeActivity.Finished(20))))
        assertEquals(ClaudeStatus.AwaitingReply, panel.claudeStatus(mapOf("a" to ClaudeActivity.Finished(40))))
    }
}
