package io.github.taetae98coding.jarvis.domain.terminal

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class UpdateTerminalWorkspaceUseCaseTest {
    private val terminal = RecordingTerminalRepository()

    private fun useCase(workspace: TerminalWorkspace) =
        UpdateTerminalWorkspaceUseCase(InMemoryTerminalWorkspaceRepository(workspace), terminal)

    // 그룹 하나에 [셸, Claude a] 탭, 그 오른쪽에 셸 탭 하나짜리 그룹.
    private val withClaude = TerminalWorkspace.initial()
        .addTab(program = TerminalProgram.Claude, claudeSessionId = "a")
        .split(SplitDirection.SideBySide)

    private val claudeTab = withClaude.tabs.first { it.claudeSessionId == "a" }

    @Test
    fun closingAClaudeTabStopsItsSession() = runTest {
        useCase(withClaude).invoke { it.closeTab(claudeTab.id) }

        assertEquals(listOf("a"), terminal.stopped)
    }

    @Test
    fun closingAShellTabStopsNothing() = runTest {
        useCase(withClaude).invoke { it.closeFocusedTab() }

        assertEquals(emptyList(), terminal.stopped)
    }

    @Test
    fun closingAPanelStopsEveryClaudeSessionInIt() = runTest {
        val workspace = withClaude.addTab(program = TerminalProgram.Claude, claudeSessionId = "b").addPanel()
            .addTab(program = TerminalProgram.Claude, claudeSessionId = "other")
        val panel = workspace.panels.first().id

        useCase(workspace).invoke { it.closePanel(panel) }

        assertEquals(listOf("a", "b"), terminal.stopped)
    }

    @Test
    fun movingAClaudeTabToAnotherGroupStopsNothing() = runTest {
        val target = withClaude.groups.last().id

        useCase(withClaude).invoke { it.dockTab(claudeTab.id, target, DockEdge.Center).addPanel().selectTabAt(0) }

        assertEquals(emptyList(), terminal.stopped)
    }
}
