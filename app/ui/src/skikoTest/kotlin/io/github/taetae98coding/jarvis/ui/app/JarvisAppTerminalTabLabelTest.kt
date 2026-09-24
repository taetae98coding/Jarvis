package io.github.taetae98coding.jarvis.ui.app

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.click
import androidx.compose.ui.test.doubleClick
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.v2.runComposeUiTest
import io.github.taetae98coding.jarvis.ui.terminal.TerminalNewClaudeTabTestTag
import io.github.taetae98coding.jarvis.ui.terminal.TerminalNewShellTabTestTag
import io.github.taetae98coding.jarvis.ui.terminal.TerminalTabNameFieldTestTag
import io.github.taetae98coding.jarvis.ui.terminal.TerminalTestTag
import io.github.taetae98coding.jarvis.ui.terminal.terminalTabKindTestTag
import io.github.taetae98coding.jarvis.ui.terminal.terminalTabTestTag
import io.github.taetae98coding.jarvis.ui.terminal.terminalTabTitleTestTag
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** docs/common/terminal-tab-label.html */
@OptIn(ExperimentalTestApi::class)
class JarvisAppTerminalTabLabelTest {
    private fun ComposeUiTest.openTerminal(
        terminal: FakeTerminalRepository,
        workspace: FakeTerminalWorkspaceRepository,
    ): Long {
        setContent { TestJarvisApp(terminal = terminal, terminalWorkspace = workspace) }
        onNodeWithTag(TerminalTestTag).performClick()
        waitUntil(timeoutMillis = FrameTimeoutMillis) { terminal.sessions.size == 1 }

        return workspace.workspace.value.focusedTab!!.id
    }

    private fun ComposeUiTest.openNewTab(workspace: FakeTerminalWorkspaceRepository, itemTag: String): Long {
        val before = workspace.workspace.value.tabIds.size
        onNode(newTabButton).performClick()
        onNodeWithTag(itemTag).performClick()
        waitUntil(timeoutMillis = FrameTimeoutMillis) { workspace.workspace.value.tabIds.size == before + 1 }

        return workspace.workspace.value.focusedTab!!.id
    }

    private fun ComposeUiTest.startRename(tabId: Long) {
        onNodeWithTag(terminalTabTitleTestTag(tabId)).performMouseInput { doubleClick(center) }
        waitUntil(timeoutMillis = FrameTimeoutMillis) {
            onAllNodesWithTag(TerminalTabNameFieldTestTag).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun ComposeUiTest.nameFieldCount() = onAllNodesWithTag(TerminalTabNameFieldTestTag).fetchSemanticsNodes().size

    // 브라우저 탭은 웹뷰가 skiko 테스트에서 뜨지 않아 도메인 테스트(TerminalWorkspaceTest)가 종류를 확인한다.
    @Test
    fun everyTabShowsItsKind() = runComposeUiTest {
        val terminal = FakeTerminalRepository()
        val workspace = FakeTerminalWorkspaceRepository()
        val shell = openTerminal(terminal, workspace)
        val claude = openNewTab(workspace, TerminalNewClaudeTabTestTag)

        onNodeWithTag(terminalTabKindTestTag(shell)).assertContentDescriptionEquals("터미널")
        onNodeWithTag(terminalTabKindTestTag(claude)).assertContentDescriptionEquals("Claude")
    }

    @Test
    fun doubleClickingTheTitleRenamesTheTab() = runComposeUiTest {
        val terminal = FakeTerminalRepository()
        val workspace = FakeTerminalWorkspaceRepository()
        val tabId = openTerminal(terminal, workspace)

        startRename(tabId)
        onNodeWithTag(TerminalTabNameFieldTestTag).assertTextEquals("셸 1")
        onNodeWithTag(TerminalTabNameFieldTestTag).performTextReplacement("  서버 로그 ")
        onNodeWithTag(TerminalTabNameFieldTestTag).performKeyInput { pressKey(Key.Enter) }

        waitUntil(timeoutMillis = FrameTimeoutMillis) { workspace.workspace.value.focusedTab?.name == "서버 로그" }
        onNodeWithTag(terminalTabTitleTestTag(tabId)).assertTextEquals("서버 로그")
        assertEquals(0, nameFieldCount())
    }

    @Test
    fun theNameWinsOverTheShellTitle() = runComposeUiTest {
        val terminal = FakeTerminalRepository()
        val workspace = FakeTerminalWorkspaceRepository()
        val tabId = openTerminal(terminal, workspace)

        startRename(tabId)
        onNodeWithTag(TerminalTabNameFieldTestTag).performTextReplacement("서버")
        onNodeWithTag(TerminalTabNameFieldTestTag).performImeAction()
        waitUntil(timeoutMillis = FrameTimeoutMillis) { workspace.workspace.value.focusedTab?.name == "서버" }

        terminal.sessions.single().emit("\u001b]0;build-server\u0007")
        waitForIdle()

        onNodeWithTag(terminalTabTitleTestTag(tabId)).assertTextEquals("서버")
        assertEquals(0, onAllNodesWithText("build-server").fetchSemanticsNodes().size)
    }

    @Test
    fun escapeCancelsTheRename() = runComposeUiTest {
        val terminal = FakeTerminalRepository()
        val workspace = FakeTerminalWorkspaceRepository()
        val tabId = openTerminal(terminal, workspace)

        startRename(tabId)
        onNodeWithTag(TerminalTabNameFieldTestTag).performTextReplacement("버릴 이름")
        onNodeWithTag(TerminalTabNameFieldTestTag).performKeyInput { pressKey(Key.Escape) }

        waitUntil(timeoutMillis = FrameTimeoutMillis) { nameFieldCount() == 0 }
        assertNull(workspace.workspace.value.focusedTab!!.name)
        onNodeWithTag(terminalTabTitleTestTag(tabId)).assertTextEquals("셸 1")
    }

    @Test
    fun confirmingWithoutChangesKeepsTheAutomaticTitle() = runComposeUiTest {
        val terminal = FakeTerminalRepository()
        val workspace = FakeTerminalWorkspaceRepository()
        val tabId = openTerminal(terminal, workspace)

        startRename(tabId)
        onNodeWithTag(TerminalTabNameFieldTestTag).performImeAction()

        waitUntil(timeoutMillis = FrameTimeoutMillis) { nameFieldCount() == 0 }
        assertNull(workspace.workspace.value.focusedTab!!.name)
    }

    @Test
    fun blankNameGoesBackToTheAutomaticTitle() = runComposeUiTest {
        val terminal = FakeTerminalRepository()
        val workspace = FakeTerminalWorkspaceRepository()
        val tabId = openTerminal(terminal, workspace)
        workspace.workspace.value = workspace.workspace.value.renameTab(tabId, "서버")
        waitUntil(timeoutMillis = FrameTimeoutMillis) {
            onAllNodesWithText("서버").fetchSemanticsNodes().isNotEmpty()
        }

        startRename(tabId)
        onNodeWithTag(TerminalTabNameFieldTestTag).performTextReplacement("  ")
        onNodeWithTag(TerminalTabNameFieldTestTag).performImeAction()

        waitUntil(timeoutMillis = FrameTimeoutMillis) { workspace.workspace.value.focusedTab?.name == null }
        onNodeWithTag(terminalTabTitleTestTag(tabId)).assertTextEquals("셸 1")
    }

    @Test
    fun aSingleClickSelectsWithoutRenaming() = runComposeUiTest {
        val terminal = FakeTerminalRepository()
        val workspace = FakeTerminalWorkspaceRepository()
        val first = openTerminal(terminal, workspace)
        openNewTab(workspace, TerminalNewShellTabTestTag)

        onNodeWithTag(terminalTabTitleTestTag(first)).performMouseInput { click(center) }

        waitUntil(timeoutMillis = FrameTimeoutMillis) { workspace.workspace.value.focusedTab?.id == first }
        assertEquals(0, nameFieldCount())
    }

    @Test
    fun aTabBeingRenamedCannotBeDragged() = runComposeUiTest {
        val terminal = FakeTerminalRepository()
        val workspace = FakeTerminalWorkspaceRepository()
        val tabId = openTerminal(terminal, workspace)
        openNewTab(workspace, TerminalNewShellTabTestTag)
        val groupId = workspace.workspace.value.groups.single().id

        startRename(tabId)
        dragTabOntoGroup(tabId, groupId, Offset(0.95f, 0.5f))
        waitForIdle()

        assertEquals(listOf(groupId), workspace.workspace.value.groups.map { it.id })
        assertEquals(2, workspace.workspace.value.groups.single().tabs.size)
        onNodeWithTag(terminalTabTestTag(tabId)).assertIsDisplayed()
    }

    private companion object {
        const val FrameTimeoutMillis = 10_000L
    }
}
