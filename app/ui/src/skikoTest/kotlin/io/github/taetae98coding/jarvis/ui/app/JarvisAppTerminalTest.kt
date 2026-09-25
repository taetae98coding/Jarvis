package io.github.taetae98coding.jarvis.ui.app

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.MouseInjectionScope
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.test.withKeyDown
import io.github.taetae98coding.jarvis.domain.terminal.PaneNode
import io.github.taetae98coding.jarvis.domain.terminal.SplitDirection
import io.github.taetae98coding.jarvis.domain.terminal.TerminalProgram
import io.github.taetae98coding.jarvis.ui.terminal.TerminalDragGhostTestTag
import io.github.taetae98coding.jarvis.ui.terminal.TerminalEmptyPanelTestTag
import io.github.taetae98coding.jarvis.ui.terminal.TerminalLinkMenuJarvisTestTag
import io.github.taetae98coding.jarvis.ui.terminal.TerminalLinkMenuSystemTestTag
import io.github.taetae98coding.jarvis.ui.terminal.TerminalLinkMenuUrlTestTag
import io.github.taetae98coding.jarvis.ui.terminal.TerminalNewBrowserTabTestTag
import io.github.taetae98coding.jarvis.ui.terminal.TerminalNewClaudeTabTestTag
import io.github.taetae98coding.jarvis.ui.terminal.TerminalNewShellTabTestTag
import io.github.taetae98coding.jarvis.ui.terminal.TerminalScreenTestTag
import io.github.taetae98coding.jarvis.ui.terminal.TerminalTestTag
import io.github.taetae98coding.jarvis.ui.terminal.terminalDropPreviewTestTag
import io.github.taetae98coding.jarvis.ui.terminal.terminalNewTabTestTag
import io.github.taetae98coding.jarvis.ui.terminal.terminalTabCloseTestTag
import io.github.taetae98coding.jarvis.ui.terminal.terminalTabTestTag
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class JarvisAppTerminalTest {
    private fun tagPrefix(prefix: String) = SemanticsMatcher("testTag starts with $prefix") {
        it.config.getOrNull(SemanticsProperties.TestTag)?.startsWith(prefix) == true
    }

    private val pane = tagPrefix("terminal:pane:")
    private val tab = tagPrefix("terminal:tab:")
    private val group = tagPrefix("terminal:group:")
    private val dropPreview = tagPrefix("terminal:drop-preview:")

    private fun ComposeUiTest.paneCount() = onAllNodes(pane).fetchSemanticsNodes().size

    private fun ComposeUiTest.tabCount() = onAllNodes(tab).fetchSemanticsNodes().size

    private fun ComposeUiTest.groupCount() = onAllNodes(group).fetchSemanticsNodes().size

    private fun ComposeUiTest.openTerminal(
        terminal: FakeTerminalRepository,
        workspace: FakeTerminalWorkspaceRepository = FakeTerminalWorkspaceRepository(),
    ) {
        setContent { TestJarvisApp(terminal = terminal, terminalWorkspace = workspace) }
        onNodeWithTag(TerminalTestTag).performClick()
        waitUntil(timeoutMillis = FrameTimeoutMillis) { terminal.sessions.size == 1 }
    }

    /** 그룹이 하나뿐일 때. 그 그룹의 + 를 눌러 메뉴 항목을 고른다. */
    private fun ComposeUiTest.openNewTab(itemTag: String) {
        onNode(newTabButton).performClick()
        onNodeWithTag(itemTag).performClick()
    }

    /** 그룹 하나에 셸 탭 둘. 둘째 탭이 선택돼 있다. (첫 탭 id, 둘째 탭 id, 그룹 id) */
    private fun ComposeUiTest.openTwoTabs(
        terminal: FakeTerminalRepository,
        workspace: FakeTerminalWorkspaceRepository,
    ): Triple<Long, Long, Long> {
        openTerminal(terminal, workspace)
        openNewTab(TerminalNewShellTabTestTag)
        waitUntil(timeoutMillis = FrameTimeoutMillis) { terminal.sessions.size == 2 }

        val group = workspace.workspace.value.groups.single()
        return Triple(group.tabs[0].id, group.tabs[1].id, group.id)
    }

    @Test
    fun terminalCardOpensOneGroupWithOneShell() = runComposeUiTest {
        val terminal = FakeTerminalRepository()
        openTerminal(terminal)

        onNodeWithTag(TerminalScreenTestTag).assertIsDisplayed()
        assertEquals(1, groupCount())
        assertEquals(1, paneCount())
        assertEquals(1, tabCount())
        onNodeWithText("셸 1").assertIsDisplayed()
    }

    @Test
    fun terminalCardIsLockedWhereItIsNotSupported() = runComposeUiTest {
        val terminal = FakeTerminalRepository(isSupported = false)
        setContent { TestJarvisApp(terminal = terminal) }

        onNodeWithTag(TerminalTestTag).assertIsNotEnabled().performClick()

        onNodeWithText("이 플랫폼에서는 셸을 실행할 수 없습니다.").assertIsDisplayed()
        assertEquals(0, onAllNodesWithTag(TerminalScreenTestTag).fetchSemanticsNodes().size)
        assertTrue(terminal.sessions.isEmpty())
    }

    @Test
    fun topBarHasOnlyTheBackButton() = runComposeUiTest {
        val terminal = FakeTerminalRepository()
        openTerminal(terminal)

        onNodeWithContentDescription("뒤로").assertIsDisplayed()
        listOf("좌우 분할", "상하 분할", "닫기").forEach { description ->
            assertEquals(0, onAllNodes(hasContentDescription(description)).fetchSemanticsNodes().size, description)
        }
    }

    @Test
    fun everyGroupHasItsOwnTabRowAndNewTabButton() = runComposeUiTest {
        val terminal = FakeTerminalRepository()
        val workspace = FakeTerminalWorkspaceRepository()
        openTerminal(terminal, workspace)
        pressTerminalShortcut(Key.D)
        waitUntil(timeoutMillis = FrameTimeoutMillis) { terminal.sessions.size == 2 }
        val (left, right) = workspace.workspace.value.groups

        val tabs = onAllNodes(tab).fetchSemanticsNodes().map { it.boundsInRoot }
        val leftButton = onNodeWithTag(terminalNewTabTestTag(left.id)).fetchSemanticsNode().boundsInRoot
        val rightButton = onNodeWithTag(terminalNewTabTestTag(right.id)).fetchSemanticsNode().boundsInRoot

        assertEquals(2, tabs.size)
        assertTrue(leftButton.left >= tabs[0].right && leftButton.right <= tabs[1].left, "left + $leftButton between $tabs")
        assertTrue(rightButton.left >= tabs[1].right, "right + $rightButton after ${tabs[1]}")
        assertEquals(tabs[0].top, leftButton.top)
        assertEquals(tabs[0].bottom, leftButton.bottom)
    }

    @Test
    fun newTabButtonAddsATabToItsOwnGroupOnly() = runComposeUiTest {
        val terminal = FakeTerminalRepository()
        val workspace = FakeTerminalWorkspaceRepository()
        openTerminal(terminal, workspace)
        pressTerminalShortcut(Key.D)
        waitUntil(timeoutMillis = FrameTimeoutMillis) { terminal.sessions.size == 2 }
        val (left, right) = workspace.workspace.value.groups

        onNodeWithTag(terminalNewTabTestTag(left.id)).performClick()
        onNodeWithTag(TerminalNewShellTabTestTag).performClick()

        waitUntil(timeoutMillis = FrameTimeoutMillis) { terminal.sessions.size == 3 }
        val after = workspace.workspace.value
        assertEquals(2, after.groups.first { it.id == left.id }.tabs.size)
        assertEquals(1, after.groups.first { it.id == right.id }.tabs.size)
        assertEquals(left.id, after.focusedGroup!!.id)
        assertEquals(3, tabCount())
        assertEquals(2, paneCount())
    }

    @Test
    fun closeIconClosesThatTabAndEndsItsShell() = runComposeUiTest {
        val terminal = FakeTerminalRepository()
        val workspace = FakeTerminalWorkspaceRepository()
        val (first, second, groupId) = openTwoTabs(terminal, workspace)

        onNodeWithTag(terminalTabCloseTestTag(second)).performClick()

        waitUntil(timeoutMillis = FrameTimeoutMillis) { tabCount() == 1 }
        assertTrue(terminal.sessions[1].closed)
        assertFalse(terminal.sessions[0].closed)
        val group = workspace.workspace.value.groups.single()
        assertEquals(groupId, group.id)
        assertEquals(first, group.selectedTabId)
    }

    @Test
    fun closingTheLastTabOfAGroupRemovesTheGroup() = runComposeUiTest {
        val terminal = FakeTerminalRepository()
        openTerminal(terminal)
        pressTerminalShortcut(Key.D)
        waitUntil(timeoutMillis = FrameTimeoutMillis) { terminal.sessions.size == 2 }
        assertEquals(2, groupCount())

        pressTerminalShortcut(Key.W)

        waitUntil(timeoutMillis = FrameTimeoutMillis) { groupCount() == 1 }
        assertTrue(terminal.sessions[1].closed)
        assertFalse(terminal.sessions[0].closed)
        assertEquals(1, tabCount())
    }

    @Test
    fun droppingATabOnTheRightEdgeSplitsOffANewGroup() = runComposeUiTest {
        val terminal = FakeTerminalRepository()
        val workspace = FakeTerminalWorkspaceRepository()
        val (first, second, groupId) = openTwoTabs(terminal, workspace)

        dragTabOntoGroup(first, groupId, Offset(0.9f, 0.5f))

        waitUntil(timeoutMillis = FrameTimeoutMillis) { groupCount() == 2 }
        val root = assertIs<PaneNode.Split>(workspace.workspace.value.selectedPanel!!.root)
        assertEquals(SplitDirection.SideBySide, root.direction)
        assertEquals(PaneNode.Group(groupId, listOf(workspace.workspace.value.tabs.first { it.id == second }), second), root.first)
        val added = assertIs<PaneNode.Group>(root.second)
        assertEquals(listOf(first), added.tabs.map { it.id })
        assertEquals(added.id, workspace.workspace.value.focusedGroup!!.id)
        assertEquals(2, tabCount())
        assertEquals(2, paneCount())
        assertEquals(2, terminal.sessions.size)
        assertTrue(terminal.sessions.none { it.closed })
    }

    @Test
    fun droppingATabOnTheTopEdgeStacksTheNewGroupFirst() = runComposeUiTest {
        val terminal = FakeTerminalRepository()
        val workspace = FakeTerminalWorkspaceRepository()
        val (first, _, groupId) = openTwoTabs(terminal, workspace)

        dragTabOntoGroup(first, groupId, Offset(0.5f, 0.1f))

        waitUntil(timeoutMillis = FrameTimeoutMillis) { groupCount() == 2 }
        val root = assertIs<PaneNode.Split>(workspace.workspace.value.selectedPanel!!.root)
        assertEquals(SplitDirection.Stacked, root.direction)
        assertEquals(listOf(first), assertIs<PaneNode.Group>(root.first).tabs.map { it.id })
        assertEquals(groupId, assertIs<PaneNode.Group>(root.second).id)
    }

    @Test
    fun droppingInTheCenterOfAnotherGroupMovesTheTabAndRemovesTheEmptyGroup() = runComposeUiTest {
        val terminal = FakeTerminalRepository()
        val workspace = FakeTerminalWorkspaceRepository()
        openTerminal(terminal, workspace)
        pressTerminalShortcut(Key.D)
        waitUntil(timeoutMillis = FrameTimeoutMillis) { terminal.sessions.size == 2 }
        val (left, right) = workspace.workspace.value.groups

        dragTabOntoGroup(left.selectedTabId, right.id, Offset(0.5f, 0.5f))

        waitUntil(timeoutMillis = FrameTimeoutMillis) { groupCount() == 1 }
        val group = assertIs<PaneNode.Group>(workspace.workspace.value.selectedPanel!!.root)
        assertEquals(right.id, group.id)
        assertEquals(listOf(right.selectedTabId, left.selectedTabId), group.tabs.map { it.id })
        assertEquals(left.selectedTabId, group.selectedTabId)
        assertEquals(2, tabCount())
        assertEquals(1, paneCount())
        assertTrue(terminal.sessions.none { it.closed })
    }

    @Test
    fun previewAndGhostShowWhileDraggingAndVanishOnRelease() = runComposeUiTest {
        val terminal = FakeTerminalRepository()
        val workspace = FakeTerminalWorkspaceRepository()
        val (first, _, groupId) = openTwoTabs(terminal, workspace)

        dragTabOntoGroup(first, groupId, Offset(0.5f, 0.9f), release = false)

        onNodeWithTag(terminalDropPreviewTestTag(groupId)).assertIsDisplayed()
        onNode(hasTestTag(TerminalDragGhostTestTag) and hasText("셸 1")).assertIsDisplayed()

        onNodeWithTag(terminalTabTestTag(first)).performMouseInput { release() }

        waitUntil(timeoutMillis = FrameTimeoutMillis) { groupCount() == 2 }
        assertEquals(0, onAllNodes(dropPreview).fetchSemanticsNodes().size)
        assertEquals(0, onAllNodesWithTag(TerminalDragGhostTestTag).fetchSemanticsNodes().size)
    }

    @Test
    fun droppingOutsideAGroupChangesNothing() = runComposeUiTest {
        val terminal = FakeTerminalRepository()
        val workspace = FakeTerminalWorkspaceRepository()
        val (first, _, _) = openTwoTabs(terminal, workspace)
        val before = workspace.workspace.value

        // 상단 막대 한가운데. 그룹도 탭 줄도 아니다.
        dragTabTo(first, onNodeWithContentDescription("뒤로").fetchSemanticsNode().boundsInRoot.center + Offset(200f, 0f))

        assertEquals(before, workspace.workspace.value)
        assertEquals(2, tabCount())
        assertEquals(1, groupCount())
    }

    @Test
    fun draggingOverItsOwnGroupCenterShowsNoTargetAndChangesNothing() = runComposeUiTest {
        val terminal = FakeTerminalRepository()
        val workspace = FakeTerminalWorkspaceRepository()
        val (first, _, groupId) = openTwoTabs(terminal, workspace)
        val before = workspace.workspace.value

        dragTabOntoGroup(first, groupId, Offset(0.5f, 0.5f), release = false)
        assertEquals(0, onAllNodes(dropPreview).fetchSemanticsNodes().size)
        onNodeWithTag(TerminalDragGhostTestTag).assertIsDisplayed()

        onNodeWithTag(terminalTabTestTag(first)).performMouseInput { release() }

        assertEquals(before, workspace.workspace.value)
        assertEquals(1, groupCount())
    }

    @Test
    fun theOnlyTabOfAGroupCannotBeDroppedOnItsOwnEdge() = runComposeUiTest {
        val terminal = FakeTerminalRepository()
        val workspace = FakeTerminalWorkspaceRepository()
        openTerminal(terminal, workspace)
        val group = workspace.workspace.value.groups.single()
        val before = workspace.workspace.value

        dragTabOntoGroup(group.selectedTabId, group.id, Offset(0.9f, 0.5f), release = false)
        assertEquals(0, onAllNodes(dropPreview).fetchSemanticsNodes().size)
        onNodeWithTag(terminalTabTestTag(group.selectedTabId)).performMouseInput { release() }

        assertEquals(before, workspace.workspace.value)
    }

    @Test
    fun pressingATabWithoutDraggingSelectsIt() = runComposeUiTest {
        val terminal = FakeTerminalRepository()
        val workspace = FakeTerminalWorkspaceRepository()
        val (first, second, _) = openTwoTabs(terminal, workspace)
        assertEquals(second, workspace.workspace.value.focusedTab!!.id)

        onNodeWithTag(terminalTabTestTag(first)).performMouseInput {
            moveTo(center)
            press()
            moveBy(Offset(2f, 1f))
            release()
        }

        waitUntil(timeoutMillis = FrameTimeoutMillis) { workspace.workspace.value.focusedTab?.id == first }
        assertEquals(2, tabCount())
        assertEquals(1, paneCount())
    }

    @Test
    fun sideBySideShortcutOpensASecondGroup() = runComposeUiTest {
        val terminal = FakeTerminalRepository()
        openTerminal(terminal)

        pressTerminalShortcut(Key.D)

        waitUntil(timeoutMillis = FrameTimeoutMillis) { terminal.sessions.size == 2 }
        assertEquals(2, groupCount())
        assertEquals(2, paneCount())
    }

    @Test
    fun stackedShortcutOpensASecondGroup() = runComposeUiTest {
        val terminal = FakeTerminalRepository()
        openTerminal(terminal)

        pressTerminalShortcut(Key.D, shift = true)

        waitUntil(timeoutMillis = FrameTimeoutMillis) { terminal.sessions.size == 2 }
        assertEquals(2, groupCount())
    }

    @Test
    fun newTabHidesTheOtherTabsPaneInTheSameGroup() = runComposeUiTest {
        val terminal = FakeTerminalRepository()
        openTerminal(terminal)

        openNewTab(TerminalNewShellTabTestTag)

        waitUntil(timeoutMillis = FrameTimeoutMillis) { terminal.sessions.size == 2 }
        assertEquals(TerminalProgram.Shell, terminal.sessions[1].program)
        assertEquals(2, tabCount())
        onNodeWithText("셸 2").assertIsDisplayed()
        assertEquals(1, paneCount())
        assertTrue(terminal.sessions.none { it.closed })
    }

    @Test
    fun claudeMenuItemOpensClaudeInANewTab() = runComposeUiTest {
        val terminal = FakeTerminalRepository()
        openTerminal(terminal)

        assertEquals(0, onAllNodes(hasContentDescription("Claude (YOLO)")).fetchSemanticsNodes().size)
        onNode(newTabButton).performClick()
        onNodeWithText("Claude (YOLO)").performClick()

        waitUntil(timeoutMillis = FrameTimeoutMillis) { terminal.sessions.size == 2 }
        assertEquals(listOf(TerminalProgram.Shell, TerminalProgram.Claude), terminal.sessions.map { it.program })
        assertEquals(2, tabCount())
        assertEquals(1, paneCount())
    }

    @Test
    fun splittingAClaudeGroupOpensAShell() = runComposeUiTest {
        val terminal = FakeTerminalRepository()
        openTerminal(terminal)
        openNewTab(TerminalNewClaudeTabTestTag)
        waitUntil(timeoutMillis = FrameTimeoutMillis) { terminal.sessions.size == 2 }

        pressTerminalShortcut(Key.D)

        waitUntil(timeoutMillis = FrameTimeoutMillis) { terminal.sessions.size == 3 }
        assertEquals(TerminalProgram.Shell, terminal.sessions[2].program)
    }

    // 기기 구획이 있으면 Claude·브라우저가 없어도 메뉴가 뜬다(docs/common/terminal-device.html R14).
    @Test
    fun devicesKeepTheMenuWhereClaudeIsNotSupported() = runComposeUiTest {
        val terminal = FakeTerminalRepository(isClaudeSupported = false)
        openTerminal(terminal)

        openNewTab(TerminalNewShellTabTestTag)

        waitUntil(timeoutMillis = FrameTimeoutMillis) { terminal.sessions.size == 2 }
        assertEquals(TerminalProgram.Shell, terminal.sessions[1].program)
        assertEquals(0, onAllNodesWithTag(TerminalNewClaudeTabTestTag).fetchSemanticsNodes().size)
    }

    @Test
    fun browserMenuItemIsLastWhereItIsSupported() = runComposeUiTest {
        val terminal = FakeTerminalRepository(isBrowserSupported = true)
        openTerminal(terminal)

        onNode(newTabButton).performClick()

        val items = listOf(TerminalNewShellTabTestTag, TerminalNewClaudeTabTestTag, TerminalNewBrowserTabTestTag)
            .map { onNodeWithTag(it).fetchSemanticsNode().boundsInRoot.top }
        assertEquals(items.sorted(), items)
        onNodeWithText("웹 브라우저").assertIsDisplayed()
    }

    @Test
    fun browserAloneStillOpensTheMenu() = runComposeUiTest {
        val terminal = FakeTerminalRepository(isClaudeSupported = false, isBrowserSupported = true)
        openTerminal(terminal)

        onNode(newTabButton).performClick()

        onNodeWithTag(TerminalNewShellTabTestTag).assertIsDisplayed()
        onNodeWithTag(TerminalNewBrowserTabTestTag).assertIsDisplayed()
        assertEquals(0, onAllNodesWithTag(TerminalNewClaudeTabTestTag).fetchSemanticsNodes().size)
        assertEquals(1, terminal.sessions.size)
    }

    @Test
    fun browserMenuItemIsHiddenWhereItIsNotSupported() = runComposeUiTest {
        val terminal = FakeTerminalRepository()
        openTerminal(terminal)

        onNode(newTabButton).performClick()

        onNodeWithTag(TerminalNewClaudeTabTestTag).assertIsDisplayed()
        assertEquals(0, onAllNodesWithTag(TerminalNewBrowserTabTestTag).fetchSemanticsNodes().size)
    }

    @Test
    fun closeShortcutEndsTheSelectedTabOfTheFocusedGroup() = runComposeUiTest {
        val terminal = FakeTerminalRepository()
        openTerminal(terminal)
        openNewTab(TerminalNewShellTabTestTag)
        waitUntil(timeoutMillis = FrameTimeoutMillis) { terminal.sessions.size == 2 }

        pressTerminalShortcut(Key.W)

        waitUntil(timeoutMillis = FrameTimeoutMillis) { tabCount() == 1 }
        assertTrue(terminal.sessions[1].closed)
        assertFalse(terminal.sessions[0].closed)
        assertEquals(1, groupCount())
    }

    @Test
    fun closingTheLastTabLeavesAnEmptyPanelWithANewTabButton() = runComposeUiTest {
        val terminal = FakeTerminalRepository()
        val workspace = FakeTerminalWorkspaceRepository()
        openTerminal(terminal, workspace)
        val groupButton = onNodeWithTag(terminalNewTabTestTag(workspace.workspace.value.groups.single().id)).fetchSemanticsNode().boundsInRoot

        pressTerminalShortcut(Key.W)

        waitUntil(timeoutMillis = FrameTimeoutMillis) {
            onAllNodesWithTag(TerminalEmptyPanelTestTag).fetchSemanticsNodes().isNotEmpty()
        }
        onNodeWithTag(TerminalScreenTestTag).assertIsDisplayed()
        onNodeWithText("탭이 없습니다. 새 탭(+)으로 터미널이나 Claude 를 엽니다.").assertIsDisplayed()
        assertTrue(terminal.sessions.single().closed)
        val emptyButton = onNodeWithTag(terminalNewTabTestTag(null)).fetchSemanticsNode().boundsInRoot
        assertEquals(groupButton.size, emptyButton.size)
        assertEquals(groupButton.top, emptyButton.top)

        onNodeWithTag(terminalNewTabTestTag(null)).performClick()
        onNodeWithTag(TerminalNewShellTabTestTag).performClick()

        waitUntil(timeoutMillis = FrameTimeoutMillis) { terminal.sessions.size == 2 }
        assertEquals(1, groupCount())
        assertEquals(0, onAllNodesWithTag(TerminalEmptyPanelTestTag).fetchSemanticsNodes().size)
    }

    @Test
    fun exitedShellClosesItsTab() = runComposeUiTest {
        val terminal = FakeTerminalRepository()
        openTerminal(terminal)
        pressTerminalShortcut(Key.D, shift = true)
        waitUntil(timeoutMillis = FrameTimeoutMillis) { terminal.sessions.size == 2 }

        terminal.sessions[1].exit()

        waitUntil(timeoutMillis = FrameTimeoutMillis) { groupCount() == 1 }
    }

    @Test
    fun leavingTheScreenKeepsEveryShell() = runComposeUiTest {
        val terminal = FakeTerminalRepository()
        openTerminal(terminal)
        openNewTab(TerminalNewShellTabTestTag)
        waitUntil(timeoutMillis = FrameTimeoutMillis) { terminal.sessions.size == 2 }
        terminal.sessions[1].emit("still-here")

        onNodeWithContentDescription("뒤로").performClick()
        waitUntil(timeoutMillis = FrameTimeoutMillis) {
            onAllNodesWithTag(TerminalScreenTestTag).fetchSemanticsNodes().isEmpty()
        }
        assertTrue(terminal.sessions.none { it.closed })

        onNodeWithTag(TerminalTestTag).performClick()
        onNodeWithTag(TerminalScreenTestTag).assertIsDisplayed()

        assertEquals(2, terminal.sessions.size)
        assertTrue(terminal.sessions.none { it.closed })
    }

    /**
     * 셸이 첫 줄 첫 칸부터 [text] 를 찍고 제목을 정한다. 제목이 탭에 보이면 같은 덩어리의 글자도 해석된 것이다.
     * 첫 칸을 누르는 것이 곧 그 글자를 누르는 것이라, 칸 크기를 몰라도 된다.
     */
    private fun ComposeUiTest.emitAtFirstCell(session: FakeTerminalSession, text: String) {
        session.emit("$text\u001b]0;printed\u0007")
        waitUntil(timeoutMillis = FrameTimeoutMillis) {
            onAllNodesWithText("printed").fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun ComposeUiTest.clickFirstCell() {
        onNode(pane).performMouseInput { click(Offset(2f, 2f)) }
    }

    @Test
    fun clickingALinkOffersSystemAndJarvisBrowser() = runComposeUiTest {
        val terminal = FakeTerminalRepository(isBrowserSupported = true)
        val uriHandler = RecordingUriHandler()
        setContent { TestJarvisApp(terminal = terminal, uriHandler = uriHandler) }
        onNodeWithTag(TerminalTestTag).performClick()
        waitUntil(timeoutMillis = FrameTimeoutMillis) { terminal.sessions.size == 1 }
        emitAtFirstCell(terminal.sessions.single(), "https://example.com/docs. next")

        clickFirstCell()

        onNodeWithTag(TerminalLinkMenuUrlTestTag).assertIsDisplayed()
        onNode(hasText("https://example.com/docs") and hasTestTag(TerminalLinkMenuUrlTestTag)).assertIsDisplayed()
        onNodeWithTag(TerminalLinkMenuJarvisTestTag).assertIsDisplayed()
        val items = listOf(TerminalLinkMenuSystemTestTag, TerminalLinkMenuJarvisTestTag)
            .map { onNodeWithTag(it).fetchSemanticsNode().boundsInRoot.top }
        assertEquals(items.sorted(), items)

        onNodeWithTag(TerminalLinkMenuSystemTestTag).performClick()

        assertEquals(listOf("https://example.com/docs"), uriHandler.opened)
        assertEquals(0, onAllNodesWithTag(TerminalLinkMenuUrlTestTag).fetchSemanticsNodes().size)
    }

    @Test
    fun clickingPlainTextOnlyFocusesThePane() = runComposeUiTest {
        val terminal = FakeTerminalRepository(isBrowserSupported = true)
        val uriHandler = RecordingUriHandler()
        setContent { TestJarvisApp(terminal = terminal, uriHandler = uriHandler) }
        onNodeWithTag(TerminalTestTag).performClick()
        waitUntil(timeoutMillis = FrameTimeoutMillis) { terminal.sessions.size == 1 }
        emitAtFirstCell(terminal.sessions.single(), "hello https://example.com")

        clickFirstCell()

        assertEquals(0, onAllNodesWithTag(TerminalLinkMenuUrlTestTag).fetchSemanticsNodes().size)
        assertTrue(uriHandler.opened.isEmpty())
    }

    @Test
    fun linkOpensInTheSystemBrowserDirectlyWhereJarvisBrowserIsNotSupported() = runComposeUiTest {
        val terminal = FakeTerminalRepository(isBrowserSupported = false)
        val uriHandler = RecordingUriHandler()
        setContent { TestJarvisApp(terminal = terminal, uriHandler = uriHandler) }
        onNodeWithTag(TerminalTestTag).performClick()
        waitUntil(timeoutMillis = FrameTimeoutMillis) { terminal.sessions.size == 1 }
        emitAtFirstCell(terminal.sessions.single(), "http://localhost:8080/")

        clickFirstCell()

        assertEquals(listOf("http://localhost:8080/"), uriHandler.opened)
        assertEquals(0, onAllNodesWithTag(TerminalLinkMenuUrlTestTag).fetchSemanticsNodes().size)
    }

    /** 창의 첫 칸을 눌러 창 안 픽셀 [destination] 까지 마우스로 끌어 놓는다. */
    private fun ComposeUiTest.dragFromFirstCell(destination: MouseInjectionScope.() -> Offset) {
        onNode(pane).performMouseInput {
            moveTo(Offset(2f, 2f))
            press()
            // 슬롭을 넘기는 첫 이동과 목적지까지의 이동을 나눠서, 시작 판정과 이동이 서로 다른 이벤트로 온다.
            moveBy(Offset(DragSlop, 0f))
            moveTo(destination())
            release()
        }
    }

    @Test
    fun draggingAcrossARowCopiesItsTextWhenReleased() = runComposeUiTest {
        val terminal = FakeTerminalRepository()
        val clipboard = RecordingClipboardManager()
        setContent { TestJarvisApp(terminal = terminal, clipboard = clipboard) }
        onNodeWithTag(TerminalTestTag).performClick()
        waitUntil(timeoutMillis = FrameTimeoutMillis) { terminal.sessions.size == 1 }
        val session = terminal.sessions.single()
        emitAtFirstCell(session, "hello world")

        dragFromFirstCell { Offset(width - 2f, 2f) }

        assertEquals(listOf("hello world"), clipboard.copied)
        assertEquals("", session.writtenText())
    }

    @Test
    fun draggingDownJoinsRowsWithLineFeeds() = runComposeUiTest {
        val terminal = FakeTerminalRepository()
        val clipboard = RecordingClipboardManager()
        setContent { TestJarvisApp(terminal = terminal, clipboard = clipboard) }
        onNodeWithTag(TerminalTestTag).performClick()
        waitUntil(timeoutMillis = FrameTimeoutMillis) { terminal.sessions.size == 1 }
        emitAtFirstCell(terminal.sessions.single(), "first\r\nsecond")

        dragFromFirstCell { bottomRight - Offset(1f, 1f) }

        assertEquals(listOf("first\nsecond"), clipboard.copied)
    }

    @Test
    fun draggingWritesNothingToTheShellAndOpensNoLinkMenu() = runComposeUiTest {
        val terminal = FakeTerminalRepository(isBrowserSupported = true)
        val uriHandler = RecordingUriHandler()
        val clipboard = RecordingClipboardManager()
        setContent { TestJarvisApp(terminal = terminal, uriHandler = uriHandler, clipboard = clipboard) }
        onNodeWithTag(TerminalTestTag).performClick()
        waitUntil(timeoutMillis = FrameTimeoutMillis) { terminal.sessions.size == 1 }
        val session = terminal.sessions.single()
        emitAtFirstCell(session, "\u001b[?1000h\u001b[?1006hhttps://example.com")

        dragFromFirstCell { Offset(width - 2f, 2f) }

        assertEquals(listOf("https://example.com"), clipboard.copied)
        assertEquals("", session.writtenText())
        assertTrue(uriHandler.opened.isEmpty())
        assertEquals(0, onAllNodesWithTag(TerminalLinkMenuUrlTestTag).fetchSemanticsNodes().size)
    }

    @Test
    fun tabIsNamedAfterTheShellTitle() = runComposeUiTest {
        val terminal = FakeTerminalRepository()
        openTerminal(terminal)

        terminal.sessions.single().emit("\u001b]0;build-server\u0007")

        waitUntil(timeoutMillis = FrameTimeoutMillis) {
            onAllNodesWithText("build-server").fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun typedTextGoesToTheFocusedShell() = runComposeUiTest {
        val terminal = FakeTerminalRepository()
        openTerminal(terminal)

        onNode(hasSetTextAction() and hasAnyAncestor(pane)).performTextInput("ls")

        waitUntil(timeoutMillis = FrameTimeoutMillis) {
            terminal.sessions.single().written.joinToString("") { it.decodeToString() } == "ls"
        }
    }

    @Test
    fun commandArrowsAndBackspaceEditTheLine() = runComposeUiTest {
        val terminal = FakeTerminalRepository()
        openTerminal(terminal)

        onNode(hasSetTextAction() and hasAnyAncestor(pane)).performKeyInput {
            withKeyDown(Key.MetaLeft) {
                pressKey(Key.DirectionLeft)
                pressKey(Key.DirectionRight)
                pressKey(Key.Backspace)
            }
        }

        waitUntil(timeoutMillis = FrameTimeoutMillis) {
            terminal.sessions.single().written.joinToString("") { it.decodeToString() } == "\u0001\u0005\u0015"
        }
    }

    // 에뮬레이터가 셸의 커서 위치 질의에 답해야 zsh 같은 셸이 프롬프트를 제자리에 그린다.
    @Test
    fun cursorPositionQueriesAreAnswered() = runComposeUiTest {
        val terminal = FakeTerminalRepository()
        openTerminal(terminal)

        terminal.sessions.single().emit("ab\u001b[6n")

        waitUntil(timeoutMillis = FrameTimeoutMillis) {
            terminal.sessions.single().written.any { it.decodeToString() == "\u001b[1;3R" }
        }
    }

    private fun FakeTerminalSession.writtenText() = written.joinToString("") { it.decodeToString() }

    /** 마우스를 창 오른쪽 아래 칸에 두고 휠을 위로 굴린다. */
    private fun ComposeUiTest.scrollUpAtBottomRight() {
        onNode(pane).performMouseInput {
            moveTo(bottomRight - Offset(1f, 1f))
            scroll(-10f)
        }
    }

    @Test
    fun wheelIsReportedAsSgrWhereClaudeCodeFullscreenTurnedMouseTrackingOn() = runComposeUiTest {
        val terminal = FakeTerminalRepository()
        openTerminal(terminal)
        val session = terminal.sessions.single()
        emitAtFirstCell(session, "\u001b[?1049h\u001b[?1000h\u001b[?1002h\u001b[?1003h\u001b[?1006h")

        scrollUpAtBottomRight()

        val cell = "${session.size.columns};${session.size.rows}"
        waitUntil(timeoutMillis = FrameTimeoutMillis) { session.writtenText().startsWith("\u001b[<64;${cell}M") }
        assertFalse(session.writtenText().contains("\u001b[<65;"))
    }

    @Test
    fun wheelSendsArrowKeysOnTheAlternateScreenWithoutMouseTracking() = runComposeUiTest {
        val terminal = FakeTerminalRepository()
        openTerminal(terminal)
        val session = terminal.sessions.single()
        emitAtFirstCell(session, "\u001b[?1049h")

        scrollUpAtBottomRight()

        waitUntil(timeoutMillis = FrameTimeoutMillis) { session.writtenText().startsWith("\u001b[A") }
        assertEquals("", session.writtenText().replace("\u001b[A", ""))
    }

    @Test
    fun wheelOnTheMainScreenScrollsBackWithoutWritingAndClampsWhenScrollbackIsCleared() = runComposeUiTest {
        val terminal = FakeTerminalRepository(isBrowserSupported = false)
        val uriHandler = RecordingUriHandler()
        setContent { TestJarvisApp(terminal = terminal, uriHandler = uriHandler) }
        onNodeWithTag(TerminalTestTag).performClick()
        waitUntil(timeoutMillis = FrameTimeoutMillis) { terminal.sessions.size == 1 }
        val session = terminal.sessions.single()
        emitAtFirstCell(session, "line\r\n".repeat(200))

        scrollUpAtBottomRight()
        waitForIdle()
        assertEquals("", session.writtenText())

        // 스크롤백이 비면 보던 자리도 맨 아래로 줄어, 첫 칸을 누르면 지금 화면 첫 줄의 링크가 잡힌다.
        emitAtFirstCell(session, "\u001b[3J\u001b[H\u001b[2Jhttp://localhost:8080/")
        clickFirstCell()

        assertEquals(listOf("http://localhost:8080/"), uriHandler.opened)
    }

    private companion object {
        /** 마우스 끌기가 시작되도록 터치 슬롭을 넘기는 첫 이동 거리. */
        const val DragSlop = 40f

        const val FrameTimeoutMillis = 10_000L
    }
}
