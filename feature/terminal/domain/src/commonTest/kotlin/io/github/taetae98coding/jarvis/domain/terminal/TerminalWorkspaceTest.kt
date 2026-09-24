package io.github.taetae98coding.jarvis.domain.terminal

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TerminalWorkspaceTest {
    @Test
    fun startsWithOneTabAndOnePane() {
        val workspace = TerminalWorkspace.initial()

        assertEquals(1, workspace.tabs.size)
        assertEquals(1, workspace.paneIds.size)
        assertEquals(workspace.paneIds.single(), workspace.focusedPaneId)
    }

    @Test
    fun sideBySideSplitPutsNewPaneOnTheRightAndFocusesIt() {
        val initial = TerminalWorkspace.initial()
        val original = initial.focusedPaneId!!

        val split = initial.split(SplitDirection.SideBySide)
        val root = assertIs<PaneNode.Split>(split.selectedTab!!.root)

        assertEquals(SplitDirection.SideBySide, root.direction)
        assertEquals(PaneNode.Leaf(original), root.first)
        assertEquals(root.second, PaneNode.Leaf(split.focusedPaneId!!))
        assertEquals(0.5f, root.ratio)
    }

    @Test
    fun stackedSplitPutsNewPaneBelow() {
        val split = TerminalWorkspace.initial().split(SplitDirection.Stacked)
        val root = assertIs<PaneNode.Split>(split.selectedTab!!.root)

        assertEquals(SplitDirection.Stacked, root.direction)
        assertEquals(PaneNode.Leaf(split.focusedPaneId!!), root.second)
    }

    @Test
    fun splitsNestInsideTheFocusedPane() {
        val workspace = TerminalWorkspace.initial()
            .split(SplitDirection.SideBySide)
            .split(SplitDirection.Stacked)

        val root = assertIs<PaneNode.Split>(workspace.selectedTab!!.root)
        val right = assertIs<PaneNode.Split>(root.second)

        assertIs<PaneNode.Leaf>(root.first)
        assertEquals(SplitDirection.Stacked, right.direction)
        assertEquals(3, workspace.paneIds.size)
        assertEquals(workspace.paneIds.toSet().size, workspace.paneIds.size)
    }

    @Test
    fun closingAPanePromotesItsSiblingAndFocusesTheAdjacentPane() {
        val split = TerminalWorkspace.initial().split(SplitDirection.SideBySide)
        val (left, right) = split.paneIds

        val closed = split.closePane(right)

        assertEquals(PaneNode.Leaf(left), closed.selectedTab!!.root)
        assertEquals(left, closed.focusedPaneId)
    }

    @Test
    fun closingTheFirstChildFocusesTheSecondChildsFirstPane() {
        val workspace = TerminalWorkspace.initial()
            .split(SplitDirection.SideBySide)
            .split(SplitDirection.Stacked)
        val (left, topRight, _) = workspace.paneIds

        val closed = workspace.focusPane(left).closePane(left)

        assertEquals(topRight, closed.focusedPaneId)
        assertEquals(2, closed.paneIds.size)
    }

    @Test
    fun closingAnUnfocusedPaneKeepsFocus() {
        val split = TerminalWorkspace.initial().split(SplitDirection.SideBySide)
        val (left, right) = split.paneIds

        val closed = split.closePane(left)

        assertEquals(right, closed.focusedPaneId)
    }

    @Test
    fun closingTheLastPaneOfATabClosesTheTab() {
        val workspace = TerminalWorkspace.initial().addTab()
        val second = workspace.selectedTab!!

        val closed = workspace.closePane(second.focusedPaneId)

        assertEquals(1, closed.tabs.size)
        assertEquals(closed.tabs.single().id, closed.selectedTabId)
    }

    @Test
    fun closingTheLastTabLeavesAnEmptyWorkspace() {
        val workspace = TerminalWorkspace.initial()

        val closed = workspace.closePane(workspace.focusedPaneId!!)

        assertTrue(closed.tabs.isEmpty())
        assertNull(closed.selectedTabId)
        assertNull(closed.focusedPaneId)
    }

    @Test
    fun newTabIsAppendedAndSelected() {
        val workspace = TerminalWorkspace.initial()

        val added = workspace.addTab()

        assertEquals(2, added.tabs.size)
        assertEquals(added.tabs.last().id, added.selectedTabId)
    }

    @Test
    fun closingTheSelectedMiddleTabSelectsTheNextOne() {
        val workspace = TerminalWorkspace.initial().addTab().addTab()
        val (first, middle, last) = workspace.tabs

        val closed = workspace.selectTab(middle.id).closeTab(middle.id)

        assertEquals(listOf(first.id, last.id), closed.tabs.map { it.id })
        assertEquals(last.id, closed.selectedTabId)
    }

    @Test
    fun adjacentTabWrapsAround() {
        val workspace = TerminalWorkspace.initial().addTab()

        assertEquals(workspace.tabs.first().id, workspace.selectAdjacentTab(1).selectedTabId)
        assertEquals(workspace.tabs.first().id, workspace.selectAdjacentTab(-1).selectedTabId)
    }

    @Test
    fun adjacentPaneFollowsScreenOrder() {
        val workspace = TerminalWorkspace.initial()
            .split(SplitDirection.SideBySide)
            .split(SplitDirection.Stacked)
        val ids = workspace.paneIds

        assertEquals(ids[0], workspace.focusAdjacentPane(1).focusedPaneId)
        assertEquals(ids[1], workspace.focusAdjacentPane(-1).focusedPaneId)
    }

    @Test
    fun focusingAPaneInAnotherTabSelectsThatTab() {
        val workspace = TerminalWorkspace.initial()
        val firstPane = workspace.focusedPaneId!!

        val focused = workspace.addTab().focusPane(firstPane)

        assertEquals(workspace.tabs.single().id, focused.selectedTabId)
        assertEquals(firstPane, focused.focusedPaneId)
    }

    @Test
    fun ratioIsClamped() {
        val split = TerminalWorkspace.initial().split(SplitDirection.SideBySide)
        val id = assertIs<PaneNode.Split>(split.selectedTab!!.root).id

        assertEquals(0.9f, (split.setRatio(id, 2f).selectedTab!!.root as PaneNode.Split).ratio)
        assertEquals(0.1f, (split.setRatio(id, -1f).selectedTab!!.root as PaneNode.Split).ratio)
        assertEquals(0.3f, (split.setRatio(id, 0.3f).selectedTab!!.root as PaneNode.Split).ratio)
    }

    @Test
    fun resizeSplitAddsToTheCurrentRatio() {
        val split = TerminalWorkspace.initial().split(SplitDirection.SideBySide)
        val id = assertIs<PaneNode.Split>(split.selectedTab!!.root).id

        val resized = split.resizeSplit(id, 0.1f).resizeSplit(id, 0.1f)

        assertEquals(0.7f, (resized.selectedTab!!.root as PaneNode.Split).ratio, 0.0001f)
    }

    @Test
    fun idsAreNeverReused() {
        val workspace = TerminalWorkspace.initial().split(SplitDirection.SideBySide)
        val closed = workspace.closePane(workspace.focusedPaneId!!)

        val again = closed.split(SplitDirection.SideBySide)

        assertTrue(again.focusedPaneId!! !in workspace.paneIds)
    }
}
