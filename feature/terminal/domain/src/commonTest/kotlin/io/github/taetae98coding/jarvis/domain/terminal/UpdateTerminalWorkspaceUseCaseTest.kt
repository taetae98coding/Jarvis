package io.github.taetae98coding.jarvis.domain.terminal

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class UpdateTerminalWorkspaceUseCaseTest {
    private val terminal = RecordingTerminalRepository()

    private fun useCase(workspace: TerminalWorkspace) =
        UpdateTerminalWorkspaceUseCase(InMemoryTerminalWorkspaceRepository(workspace), terminal)

    private val withClaude = TerminalWorkspace.initial()
        .addTab(TerminalProgram.Claude, claudeSessionId = "a")
        .split(SplitDirection.SideBySide)

    @Test
    fun closingAClaudePaneStopsItsSession() = runTest {
        val claudePane = withClaude.leaves.first { it.claudeSessionId == "a" }.paneId

        useCase(withClaude).invoke { it.closePane(claudePane) }

        assertEquals(listOf("a"), terminal.stopped)
    }

    @Test
    fun closingAShellPaneStopsNothing() = runTest {
        useCase(withClaude).invoke { it.closePane(it.focusedPaneId!!) }

        assertEquals(emptyList(), terminal.stopped)
    }

    @Test
    fun closingAPanelStopsEveryClaudeSessionInIt() = runTest {
        val workspace = withClaude.addTab(TerminalProgram.Claude, claudeSessionId = "b").addPanel()
            .addTab(TerminalProgram.Claude, claudeSessionId = "other")
        val panel = workspace.panels.first().id

        useCase(workspace).invoke { it.closePanel(panel) }

        assertEquals(listOf("a", "b"), terminal.stopped)
    }

    @Test
    fun changesThatKeepThePaneStopNothing() = runTest {
        useCase(withClaude).invoke { it.addPanel().selectTabAt(0) }

        assertEquals(emptyList(), terminal.stopped)
    }
}
