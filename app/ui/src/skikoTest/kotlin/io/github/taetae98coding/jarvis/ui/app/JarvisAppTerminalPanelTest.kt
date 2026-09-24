package io.github.taetae98coding.jarvis.ui.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.v2.runComposeUiTest
import io.github.taetae98coding.jarvis.domain.terminal.TerminalProgram
import io.github.taetae98coding.jarvis.ui.terminal.TerminalEmptyPanelTestTag
import io.github.taetae98coding.jarvis.ui.terminal.TerminalNewClaudeTabTestTag
import io.github.taetae98coding.jarvis.ui.terminal.TerminalNewPanelTestTag
import io.github.taetae98coding.jarvis.ui.terminal.TerminalNewShellTabTestTag
import io.github.taetae98coding.jarvis.ui.terminal.TerminalNewTabTestTag
import io.github.taetae98coding.jarvis.ui.terminal.TerminalPanelNameFieldTestTag
import io.github.taetae98coding.jarvis.ui.terminal.TerminalSplitSideTestTag
import io.github.taetae98coding.jarvis.ui.terminal.TerminalTestTag
import io.github.taetae98coding.jarvis.ui.terminal.terminalPanelCloseTestTag
import io.github.taetae98coding.jarvis.ui.terminal.terminalPanelRenameTestTag
import io.github.taetae98coding.jarvis.ui.terminal.terminalPanelTestTag
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class JarvisAppTerminalPanelTest {
    private fun tagPrefix(prefix: String) = SemanticsMatcher("testTag starts with $prefix") {
        it.config.getOrNull(SemanticsProperties.TestTag)?.startsWith(prefix) == true
    }

    private val tab = tagPrefix("terminal:tab:")
    private val panel = tagPrefix("terminal:panel:")

    private fun ComposeUiTest.tabCount() = onAllNodes(tab).fetchSemanticsNodes().size

    private fun ComposeUiTest.panelCount() = onAllNodes(panel).fetchSemanticsNodes().size

    private fun ComposeUiTest.openTerminal() {
        onNodeWithTag(TerminalTestTag).performClick()
    }

    private fun ComposeUiTest.openNewTab(itemTag: String) {
        onNodeWithTag(TerminalNewTabTestTag).performClick()
        onNodeWithTag(itemTag).performClick()
    }

    private fun ComposeUiTest.awaitSessions(terminal: FakeTerminalRepository, count: Int) {
        waitUntil(timeoutMillis = FrameTimeoutMillis) { terminal.sessions.size == count }
    }

    @Test
    fun startsWithOneNamedPanel() = runComposeUiTest {
        val terminal = FakeTerminalRepository()
        setContent { TestJarvisApp(terminal = terminal) }
        openTerminal()
        awaitSessions(terminal, 1)

        assertEquals(1, panelCount())
        onNodeWithText("패널 1").assertIsDisplayed()
        assertEquals(0, onAllNodesWithTag(terminalPanelCloseTestTag(1)).fetchSemanticsNodes().size)
    }

    @Test
    fun newPanelHasItsOwnTabs() = runComposeUiTest {
        val terminal = FakeTerminalRepository()
        val workspace = FakeTerminalWorkspaceRepository()
        setContent { TestJarvisApp(terminal = terminal, terminalWorkspace = workspace) }
        openTerminal()
        awaitSessions(terminal, 1)
        openNewTab(TerminalNewShellTabTestTag)
        awaitSessions(terminal, 2)

        onNodeWithTag(TerminalNewPanelTestTag).performClick()

        awaitSessions(terminal, 3)
        onNodeWithText("패널 2").assertIsDisplayed()
        assertEquals(1, tabCount())
        assertEquals(workspace.workspace.value.panels.last().id, workspace.workspace.value.selectedPanelId)
    }

    @Test
    fun switchingBackToAPanelShowsItsTabsAndKeepsItsShells() = runComposeUiTest {
        val terminal = FakeTerminalRepository()
        val workspace = FakeTerminalWorkspaceRepository()
        setContent { TestJarvisApp(terminal = terminal, terminalWorkspace = workspace) }
        openTerminal()
        awaitSessions(terminal, 1)
        openNewTab(TerminalNewShellTabTestTag)
        awaitSessions(terminal, 2)
        onNodeWithTag(TerminalNewPanelTestTag).performClick()
        awaitSessions(terminal, 3)

        onNodeWithText("패널 1").performClick()

        waitUntil(timeoutMillis = FrameTimeoutMillis) { tabCount() == 2 }
        assertEquals(3, terminal.sessions.size)
        assertTrue(terminal.sessions.none { it.closed })
    }

    @Test
    fun renamingAPanelKeepsTheNewName() = runComposeUiTest {
        val workspace = FakeTerminalWorkspaceRepository()
        setContent { TestJarvisApp(terminalWorkspace = workspace) }
        openTerminal()
        val id = workspace.workspace.value.panels.single().id

        onNodeWithTag(terminalPanelRenameTestTag(id)).performClick()
        onNodeWithTag(TerminalPanelNameFieldTestTag).performTextReplacement("  백엔드 ")
        onNodeWithTag(TerminalPanelNameFieldTestTag).performImeAction()

        waitUntil(timeoutMillis = FrameTimeoutMillis) { onAllNodesWithText("백엔드").fetchSemanticsNodes().isNotEmpty() }
        assertEquals("백엔드", workspace.workspace.value.panels.single().name)
        assertEquals(0, onAllNodesWithTag(TerminalPanelNameFieldTestTag).fetchSemanticsNodes().size)
    }

    @Test
    fun escapeCancelsARename() = runComposeUiTest {
        val workspace = FakeTerminalWorkspaceRepository()
        setContent { TestJarvisApp(terminalWorkspace = workspace) }
        openTerminal()
        val id = workspace.workspace.value.panels.single().id

        onNodeWithTag(terminalPanelRenameTestTag(id)).performClick()
        onNodeWithTag(TerminalPanelNameFieldTestTag).performTextReplacement("버릴 이름")
        onNodeWithTag(TerminalPanelNameFieldTestTag).performKeyInput { pressKey(Key.Escape) }

        waitUntil(timeoutMillis = FrameTimeoutMillis) {
            onAllNodesWithTag(TerminalPanelNameFieldTestTag).fetchSemanticsNodes().isEmpty()
        }
        onNodeWithText("패널 1").assertIsDisplayed()
        assertEquals("패널 1", workspace.workspace.value.panels.single().name)
    }

    @Test
    fun closingAPanelEndsItsShellsAndStopsItsClaude() = runComposeUiTest {
        val terminal = FakeTerminalRepository()
        val workspace = FakeTerminalWorkspaceRepository()
        setContent { TestJarvisApp(terminal = terminal, terminalWorkspace = workspace) }
        openTerminal()
        awaitSessions(terminal, 1)
        onNodeWithTag(TerminalNewPanelTestTag).performClick()
        awaitSessions(terminal, 2)
        openNewTab(TerminalNewClaudeTabTestTag)
        awaitSessions(terminal, 3)
        val second = workspace.workspace.value.panels.last().id

        onNodeWithTag(terminalPanelCloseTestTag(second)).performClick()

        waitUntil(timeoutMillis = FrameTimeoutMillis) { panelCount() == 1 }
        assertTrue(terminal.sessions[1].closed)
        assertTrue(terminal.sessions[2].closed)
        assertEquals(listOf(terminal.sessions[2].pane.claudeSessionId), terminal.stoppedClaudeSessions)
        onNodeWithText("패널 1").assertIsDisplayed()
    }

    // 앱을 끝냈다 다시 켠 것처럼 Koin 과 화면을 새로 세운다. 배치는 저장소에 남고 세션은 새로 열린다.
    @Test
    fun restartRestoresTheLayoutAndReattachesClaude() = runComposeUiTest {
        val workspace = FakeTerminalWorkspaceRepository()
        val before = FakeTerminalRepository()
        val after = FakeTerminalRepository()
        var generation by mutableIntStateOf(0)
        setContent {
            key(generation) { TestJarvisApp(terminal = if (generation == 0) before else after, terminalWorkspace = workspace) }
        }
        openTerminal()
        awaitSessions(before, 1)
        onNodeWithTag(terminalPanelRenameTestTag(workspace.workspace.value.panels.single().id)).performClick()
        onNodeWithTag(TerminalPanelNameFieldTestTag).performTextReplacement("백엔드")
        onNodeWithTag(TerminalPanelNameFieldTestTag).performImeAction()
        openNewTab(TerminalNewClaudeTabTestTag)
        awaitSessions(before, 2)
        val claudeSessionId = assertNotNull(before.sessions[1].pane.claudeSessionId)

        generation = 1
        waitUntil(timeoutMillis = FrameTimeoutMillis) { before.sessions.all { it.closed } }
        openTerminal()

        awaitSessions(after, 1)
        onNodeWithText("백엔드").assertIsDisplayed()
        assertEquals(2, tabCount())
        assertEquals(TerminalProgram.Claude, after.sessions.single().program)
        assertEquals(claudeSessionId, after.sessions.single().pane.claudeSessionId)
        assertTrue(before.stoppedClaudeSessions.isEmpty())
    }

    @Test
    fun directoryIsSavedAndUsedForTheNextSplit() = runComposeUiTest {
        val terminal = FakeTerminalRepository()
        val workspace = FakeTerminalWorkspaceRepository()
        setContent { TestJarvisApp(terminal = terminal, terminalWorkspace = workspace) }
        openTerminal()
        awaitSessions(terminal, 1)

        terminal.sessions.single().changeDirectory("/work")
        waitUntil(timeoutMillis = FrameTimeoutMillis) { workspace.workspace.value.focusedLeaf?.directory == "/work" }
        onNodeWithTag(TerminalSplitSideTestTag).performClick()

        awaitSessions(terminal, 2)
        assertEquals("/work", terminal.sessions[1].pane.directory)
    }

    @Test
    fun shellExitingInAHiddenPanelClosesItsPane() = runComposeUiTest {
        val terminal = FakeTerminalRepository()
        val workspace = FakeTerminalWorkspaceRepository()
        setContent { TestJarvisApp(terminal = terminal, terminalWorkspace = workspace) }
        openTerminal()
        awaitSessions(terminal, 1)
        onNodeWithTag(TerminalNewPanelTestTag).performClick()
        awaitSessions(terminal, 2)

        terminal.sessions[0].exit()

        waitUntil(timeoutMillis = FrameTimeoutMillis) { workspace.workspace.value.panels.first().tabs.isEmpty() }
        onNodeWithTag(terminalPanelTestTag(workspace.workspace.value.panels.first().id)).assertIsDisplayed()
        onNodeWithText("패널 1").performClick()
        waitUntil(timeoutMillis = FrameTimeoutMillis) {
            onAllNodesWithTag(TerminalEmptyPanelTestTag).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private companion object {
        const val FrameTimeoutMillis = 10_000L
    }
}
