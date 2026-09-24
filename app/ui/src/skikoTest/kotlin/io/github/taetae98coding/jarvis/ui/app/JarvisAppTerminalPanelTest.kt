package io.github.taetae98coding.jarvis.ui.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.click
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.text.TextLayoutResult
import io.github.taetae98coding.jarvis.domain.terminal.TerminalProgram
import io.github.taetae98coding.jarvis.ui.terminal.TerminalEmptyPanelTestTag
import io.github.taetae98coding.jarvis.ui.terminal.TerminalNewClaudeTabTestTag
import io.github.taetae98coding.jarvis.ui.terminal.TerminalNewPanelCancelTestTag
import io.github.taetae98coding.jarvis.ui.terminal.TerminalNewPanelConfirmTestTag
import io.github.taetae98coding.jarvis.ui.terminal.TerminalNewPanelDialogTestTag
import io.github.taetae98coding.jarvis.ui.terminal.TerminalNewPanelDirectoryTestTag
import io.github.taetae98coding.jarvis.ui.terminal.TerminalNewPanelNameTestTag
import io.github.taetae98coding.jarvis.ui.terminal.TerminalNewPanelTestTag
import io.github.taetae98coding.jarvis.ui.terminal.TerminalNewShellTabTestTag
import io.github.taetae98coding.jarvis.ui.terminal.TerminalPanelNameFieldTestTag
import io.github.taetae98coding.jarvis.ui.terminal.TerminalTestTag
import io.github.taetae98coding.jarvis.ui.terminal.terminalPanelCloseTestTag
import io.github.taetae98coding.jarvis.ui.terminal.terminalPanelRenameTestTag
import io.github.taetae98coding.jarvis.ui.terminal.terminalPanelTestTag
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
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
        onNode(newTabButton).performClick()
        onNodeWithTag(itemTag).performClick()
    }

    /** "새 패널" 창에서 "확인" 을 눌러 패널을 만들고, 그 패널의 Claude 세션이 열릴 때까지 기다린다. */
    private fun ComposeUiTest.addPanel(terminal: FakeTerminalRepository, name: String = "", directory: String = "") {
        val before = terminal.sessions.size
        confirmNewPanel(name, directory)
        awaitSessions(terminal, before + 1)
    }

    private fun ComposeUiTest.confirmNewPanel(name: String = "", directory: String = "") {
        onNodeWithTag(TerminalNewPanelTestTag).performClick()
        if (name.isNotEmpty()) onNodeWithTag(TerminalNewPanelNameTestTag).performTextReplacement(name)
        if (directory.isNotEmpty()) onNodeWithTag(TerminalNewPanelDirectoryTestTag).performTextReplacement(directory)
        onNodeWithTag(TerminalNewPanelConfirmTestTag).performClick()
    }

    private fun ComposeUiTest.dialogCount() = onAllNodesWithTag(TerminalNewPanelDialogTestTag).fetchSemanticsNodes().size

    private fun ComposeUiTest.awaitEmptyPanel() {
        waitUntil(timeoutMillis = FrameTimeoutMillis) {
            onAllNodesWithTag(TerminalEmptyPanelTestTag).fetchSemanticsNodes().isNotEmpty()
        }
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
    fun panelButtonsSitBelowTheName() = runComposeUiTest {
        val terminal = FakeTerminalRepository()
        val workspace = FakeTerminalWorkspaceRepository()
        setContent { TestJarvisApp(terminal = terminal, terminalWorkspace = workspace) }
        openTerminal()
        awaitSessions(terminal, 1)
        val name = "아주 긴 패널 이름이라 목록 폭을 넘는다"
        addPanel(terminal, name = name)
        val id = workspace.workspace.value.selectedPanelId!!

        val row = onNodeWithTag(terminalPanelTestTag(id)).getBoundsInRoot()
        val title = onNodeWithText(name).getBoundsInRoot()
        val rename = onNodeWithTag(terminalPanelRenameTestTag(id)).getBoundsInRoot()
        val close = onNodeWithTag(terminalPanelCloseTestTag(id)).getBoundsInRoot()
        assertTrue(rename.top >= title.bottom, "✎ 가 이름 아래 줄에 있어야 한다: $title / $rename")
        assertTrue(close.top >= title.bottom, "✕ 가 이름 아래 줄에 있어야 한다: $title / $close")
        assertTrue(rename.left < close.left)
        assertTrue(close.right <= row.right)
        assertTrue(title.right > rename.left, "이름이 버튼 자리까지 폭을 써야 한다: $title / $rename")

        val first = workspace.workspace.value.panels.first().id
        // 버튼 줄의 왼쪽 빈 자리.
        onNodeWithTag(terminalPanelTestTag(first)).performTouchInput { click(Offset(10f, height - 10f)) }

        waitUntil(timeoutMillis = FrameTimeoutMillis) { workspace.workspace.value.selectedPanelId == first }
    }

    @Test
    fun longPanelTextsAreNotEllipsized() = runComposeUiTest {
        val terminal = FakeTerminalRepository()
        val workspace = FakeTerminalWorkspaceRepository()
        setContent { TestJarvisApp(terminal = terminal, terminalWorkspace = workspace) }
        openTerminal()
        awaitSessions(terminal, 1)
        val name = "아주 긴 패널 이름이라 목록 폭을 한참 넘는다"
        val directory = "/Users/someone/projects/very/deep/folder/that/overflows"
        addPanel(terminal, name = name, directory = directory)
        val row = onNodeWithTag(terminalPanelTestTag(workspace.workspace.value.selectedPanelId!!)).fetchSemanticsNode().size.width

        fun layoutOf(text: String): TextLayoutResult {
            val layout = mutableListOf<TextLayoutResult>()
            // 오른쪽 사이드 바 머리에도 같은 폴더가 보이므로 패널 줄 안에서 찾는다.
            onNode(hasText(text) and hasAnyAncestor(panel), useUnmergedTree = true)
                .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layout) }
            return layout.single()
        }

        // basicMarquee 는 글자를 너비 제한 없이 재서 Paragraph 폭이 무한대가 되므로 didOverflowWidth 는 늘 true 다.
        // 말줄임 여부는 줄 단위로 본다.
        listOf(name, directory).forEach { text ->
            val layout = layoutOf(text)
            assertEquals(1, layout.lineCount)
            assertFalse(layout.isLineEllipsized(0), "말줄임 없이 글자 전체가 배치되어야 한다: $text")
            assertTrue(layout.size.width > row, "글자가 줄 폭보다 넓어야 흐른다: $text")
        }
        assertTrue(layoutOf("패널 1").size.width < row)
    }

    @Test
    fun newPanelStartsWithClaudeAndHasItsOwnTabs() = runComposeUiTest {
        val terminal = FakeTerminalRepository()
        val workspace = FakeTerminalWorkspaceRepository()
        setContent { TestJarvisApp(terminal = terminal, terminalWorkspace = workspace) }
        openTerminal()
        awaitSessions(terminal, 1)
        openNewTab(TerminalNewShellTabTestTag)
        awaitSessions(terminal, 2)

        addPanel(terminal)

        assertEquals(0, dialogCount())
        onNodeWithText("패널 2").assertIsDisplayed()
        waitUntil(timeoutMillis = FrameTimeoutMillis) { tabCount() == 1 }
        val current = workspace.workspace.value
        assertEquals(current.panels.last().id, current.selectedPanelId)
        val claude = current.selectedPanel!!.tabs.single()
        assertEquals(TerminalProgram.Claude, claude.program)
        assertNotNull(claude.claudeSessionId)
        assertEquals(claude, terminal.sessions[2].tab)
        assertEquals(0, onAllNodesWithTag(TerminalEmptyPanelTestTag).fetchSemanticsNodes().size)

        openNewTab(TerminalNewShellTabTestTag)

        awaitSessions(terminal, 4)
        assertEquals(2, tabCount())
        assertEquals(listOf(2, 2), workspace.workspace.value.panels.map { it.tabs.size })
    }

    @Test
    fun newPanelStartsEmptyWhereClaudeCannotRun() = runComposeUiTest {
        val terminal = FakeTerminalRepository(isClaudeSupported = false)
        val workspace = FakeTerminalWorkspaceRepository()
        setContent { TestJarvisApp(terminal = terminal, terminalWorkspace = workspace) }
        openTerminal()
        awaitSessions(terminal, 1)

        confirmNewPanel(directory = "/work")

        awaitEmptyPanel()
        assertEquals(0, tabCount())
        assertEquals(1, terminal.sessions.size)
        assertEquals(emptyList(), workspace.workspace.value.selectedPanel!!.tabs)

        openNewTab(TerminalNewShellTabTestTag)

        awaitSessions(terminal, 2)
        assertEquals(TerminalProgram.Shell, terminal.sessions[1].program)
        assertEquals("/work", terminal.sessions[1].tab.directory)
    }

    @Test
    fun newPanelButtonAsksForATitleAndFolderFirst() = runComposeUiTest {
        val terminal = FakeTerminalRepository()
        setContent { TestJarvisApp(terminal = terminal) }
        openTerminal()
        awaitSessions(terminal, 1)

        onNodeWithTag(TerminalNewPanelTestTag).performClick()

        assertEquals(1, dialogCount())
        assertEquals(1, panelCount())
        onNodeWithText("패널 2").assertIsDisplayed()
        onNodeWithTag(TerminalNewPanelConfirmTestTag).assertIsDisplayed()
        onNodeWithTag(TerminalNewPanelCancelTestTag).assertIsDisplayed()
        assertEquals(0, onAllNodesWithText("Claude (YOLO)").fetchSemanticsNodes().size)
    }

    @Test
    fun namedPanelStartsClaudeInItsFolderAndNewShellsFollow() = runComposeUiTest {
        val terminal = FakeTerminalRepository()
        val workspace = FakeTerminalWorkspaceRepository()
        setContent { TestJarvisApp(terminal = terminal, terminalWorkspace = workspace) }
        openTerminal()
        awaitSessions(terminal, 1)

        addPanel(terminal, name = " API ", directory = " /work/api ")

        assertEquals(0, dialogCount())
        val panel = workspace.workspace.value.selectedPanel!!
        assertEquals("API", panel.name)
        assertEquals("/work/api", panel.directory)
        val claude = terminal.sessions[1].tab
        assertEquals(TerminalProgram.Claude, claude.program)
        assertEquals("/work/api", claude.directory)
        assertNotNull(claude.claudeSessionId)
        assertEquals(listOf(claude), panel.tabs)
        onNodeWithText("API").assertIsDisplayed()
        // 오른쪽 사이드 바 머리에도 같은 폴더가 보이므로 패널 줄 안에서 찾는다.
        onNode(hasText("/work/api") and hasAnyAncestor(hasTestTag(terminalPanelTestTag(panel.id))), useUnmergedTree = true).assertIsDisplayed()

        openNewTab(TerminalNewShellTabTestTag)

        awaitSessions(terminal, 3)
        assertEquals(TerminalProgram.Shell, terminal.sessions[2].program)
        assertEquals("/work/api", terminal.sessions[2].tab.directory)
    }

    @Test
    fun cancelCreatesNothingAndClearsTheFields() = runComposeUiTest {
        val workspace = FakeTerminalWorkspaceRepository()
        setContent { TestJarvisApp(terminalWorkspace = workspace) }
        openTerminal()
        onNodeWithTag(TerminalNewPanelTestTag).performClick()
        onNodeWithTag(TerminalNewPanelNameTestTag).performTextReplacement("버릴 제목")

        onNodeWithTag(TerminalNewPanelCancelTestTag).performClick()

        waitUntil(timeoutMillis = FrameTimeoutMillis) { dialogCount() == 0 }
        assertEquals(1, workspace.workspace.value.panels.size)
        onNodeWithTag(TerminalNewPanelTestTag).performClick()
        assertEquals(0, onAllNodesWithText("버릴 제목").fetchSemanticsNodes().size)
    }

    @Test
    fun enterInTheTitleConfirms() = runComposeUiTest {
        val terminal = FakeTerminalRepository()
        val workspace = FakeTerminalWorkspaceRepository()
        setContent { TestJarvisApp(terminal = terminal, terminalWorkspace = workspace) }
        openTerminal()
        awaitSessions(terminal, 1)
        onNodeWithTag(TerminalNewPanelTestTag).performClick()
        onNodeWithTag(TerminalNewPanelNameTestTag).performTextReplacement("프론트")

        onNodeWithTag(TerminalNewPanelNameTestTag).performKeyInput { pressKey(Key.Enter) }

        awaitSessions(terminal, 2)
        assertEquals(0, dialogCount())
        assertEquals("프론트", workspace.workspace.value.selectedPanel!!.name)
        assertEquals(listOf(terminal.sessions[1].tab), workspace.workspace.value.selectedPanel!!.tabs)
        assertEquals(1, workspace.workspace.value.panels.count { it.name == "프론트" })
    }

    // 첫 Claude 탭을 닫아 빈 패널이 돼도 + 는 패널 폴더에서 연다.
    @Test
    fun newTabInAPanelEmptiedAgainStartsInThePanelFolder() = runComposeUiTest {
        val terminal = FakeTerminalRepository()
        val workspace = FakeTerminalWorkspaceRepository()
        setContent { TestJarvisApp(terminal = terminal, terminalWorkspace = workspace) }
        openTerminal()
        awaitSessions(terminal, 1)
        addPanel(terminal, directory = "/work")
        terminal.sessions[1].exit()
        awaitEmptyPanel()

        openNewTab(TerminalNewClaudeTabTestTag)

        awaitSessions(terminal, 3)
        assertEquals(TerminalProgram.Claude, terminal.sessions[2].program)
        assertEquals("/work", terminal.sessions[2].tab.directory)
    }

    @Test
    fun newTabButtonAddsATabToTheSelectedPanelOnly() = runComposeUiTest {
        val terminal = FakeTerminalRepository()
        val workspace = FakeTerminalWorkspaceRepository()
        setContent { TestJarvisApp(terminal = terminal, terminalWorkspace = workspace) }
        openTerminal()
        awaitSessions(terminal, 1)
        addPanel(terminal)

        openNewTab(TerminalNewShellTabTestTag)

        awaitSessions(terminal, 3)
        val (first, second) = workspace.workspace.value.panels
        assertEquals(1, first.tabs.size)
        assertEquals(2, second.tabs.size)
        assertEquals(2, tabCount())
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
        addPanel(terminal)
        openNewTab(TerminalNewShellTabTestTag)
        awaitSessions(terminal, 4)

        onNodeWithText("패널 1").performClick()

        waitUntil(timeoutMillis = FrameTimeoutMillis) { tabCount() == 2 }
        assertEquals(4, terminal.sessions.size)
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
        addPanel(terminal)
        openNewTab(TerminalNewShellTabTestTag)
        awaitSessions(terminal, 3)
        val second = workspace.workspace.value.panels.last().id

        onNodeWithTag(terminalPanelCloseTestTag(second)).performClick()

        waitUntil(timeoutMillis = FrameTimeoutMillis) { panelCount() == 1 }
        assertTrue(terminal.sessions[1].closed)
        assertTrue(terminal.sessions[2].closed)
        assertEquals(TerminalProgram.Claude, terminal.sessions[1].program)
        assertEquals(listOf(terminal.sessions[1].tab.claudeSessionId), terminal.stoppedClaudeSessions)
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
        val claudeSessionId = assertNotNull(before.sessions[1].tab.claudeSessionId)

        generation = 1
        waitUntil(timeoutMillis = FrameTimeoutMillis) { before.sessions.all { it.closed } }
        openTerminal()

        awaitSessions(after, 1)
        onNodeWithText("백엔드").assertIsDisplayed()
        assertEquals(2, tabCount())
        assertEquals(TerminalProgram.Claude, after.sessions.single().program)
        assertEquals(claudeSessionId, after.sessions.single().tab.claudeSessionId)
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
        waitUntil(timeoutMillis = FrameTimeoutMillis) { workspace.workspace.value.focusedTab?.directory == "/work" }
        pressTerminalShortcut(Key.D)

        awaitSessions(terminal, 2)
        assertEquals("/work", terminal.sessions[1].tab.directory)
    }

    @Test
    fun shellExitingInAHiddenPanelClosesItsPane() = runComposeUiTest {
        val terminal = FakeTerminalRepository()
        val workspace = FakeTerminalWorkspaceRepository()
        setContent { TestJarvisApp(terminal = terminal, terminalWorkspace = workspace) }
        openTerminal()
        awaitSessions(terminal, 1)
        addPanel(terminal)

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
