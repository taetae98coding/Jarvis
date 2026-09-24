package io.github.taetae98coding.jarvis.domain.terminal

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class TerminalWorkspaceTest {
    private val TerminalWorkspace.root: PaneNode?
        get() = selectedPanel!!.root

    private val TerminalWorkspace.focusedGroupId: Long?
        get() = focusedGroup?.id

    @Test
    fun startsWithOnePanelOneGroupAndOneTab() {
        val workspace = TerminalWorkspace.initial()

        assertEquals(listOf("패널 1"), workspace.panels.map { it.name })
        assertEquals(workspace.panels.single().id, workspace.selectedPanelId)
        assertEquals(1, workspace.groups.size)
        assertEquals(1, workspace.tabIds.size)
        assertEquals(workspace.tabIds.single(), workspace.focusedTab!!.id)
        assertEquals(workspace.groups.single().id, workspace.focusedGroupId)
    }

    @Test
    fun newTabIsAppendedToTheFocusedGroupAndSelected() {
        val workspace = TerminalWorkspace.initial()

        val added = workspace.addTab()
        val group = added.groups.single()

        assertEquals(2, group.tabs.size)
        assertEquals(group.tabs.last().id, group.selectedTabId)
        assertEquals(group.tabs.last(), added.focusedTab)
    }

    @Test
    fun newTabGoesToTheGivenGroupAndFocusesIt() {
        val workspace = TerminalWorkspace.initial().split(SplitDirection.SideBySide)
        val (left, right) = workspace.groups
        assertEquals(right.id, workspace.focusedGroupId)

        val added = workspace.addTab(groupId = left.id)

        assertEquals(2, added.groups.first { it.id == left.id }.tabs.size)
        assertEquals(1, added.groups.first { it.id == right.id }.tabs.size)
        assertEquals(left.id, added.focusedGroupId)
    }

    @Test
    fun newTabInAnEmptyPanelCreatesAGroup() {
        val workspace = TerminalWorkspace.initial()
        val emptied = workspace.closeTab(workspace.focusedTab!!.id)
        assertNull(emptied.root)

        val added = emptied.addTab()

        assertIs<PaneNode.Group>(added.root)
        assertEquals(1, added.groups.single().tabs.size)
        assertEquals(added.groups.single().id, added.focusedGroupId)
    }

    @Test
    fun sideBySideSplitPutsANewGroupOnTheRightAndFocusesIt() {
        val initial = TerminalWorkspace.initial()
        val original = initial.groups.single()

        val split = initial.split(SplitDirection.SideBySide)
        val root = assertIs<PaneNode.Split>(split.root)

        assertEquals(SplitDirection.SideBySide, root.direction)
        assertEquals(original, root.first)
        assertEquals<PaneNode?>(split.focusedGroup, root.second)
        assertEquals(TerminalProgram.Shell, split.focusedTab!!.program)
        assertEquals(0.5f, root.ratio)
    }

    @Test
    fun stackedSplitPutsTheNewGroupBelow() {
        val split = TerminalWorkspace.initial().split(SplitDirection.Stacked)
        val root = assertIs<PaneNode.Split>(split.root)

        assertEquals(SplitDirection.Stacked, root.direction)
        assertEquals<PaneNode?>(split.focusedGroup, root.second)
    }

    @Test
    fun splitsNestInsideTheFocusedGroup() {
        val workspace = TerminalWorkspace.initial()
            .split(SplitDirection.SideBySide)
            .split(SplitDirection.Stacked)

        val root = assertIs<PaneNode.Split>(workspace.root)
        val right = assertIs<PaneNode.Split>(root.second)

        assertIs<PaneNode.Group>(root.first)
        assertEquals(SplitDirection.Stacked, right.direction)
        assertEquals(3, workspace.groups.size)
        assertEquals(workspace.tabIds.toSet().size, workspace.tabIds.size)
    }

    @Test
    fun closingTheSelectedMiddleTabSelectsTheNextOne() {
        val workspace = TerminalWorkspace.initial().addTab().addTab()
        val (first, middle, last) = workspace.groups.single().tabs

        val closed = workspace.selectTab(middle.id).closeTab(middle.id)
        val group = closed.groups.single()

        assertEquals(listOf(first.id, last.id), group.tabs.map { it.id })
        assertEquals(last.id, group.selectedTabId)
    }

    @Test
    fun closingAnUnselectedTabKeepsTheSelection() {
        val workspace = TerminalWorkspace.initial().addTab()
        val (first, second) = workspace.groups.single().tabs

        val closed = workspace.closeTab(first.id)

        assertEquals(second.id, closed.groups.single().selectedTabId)
    }

    @Test
    fun closingTheLastTabRemovesTheGroupAndFocusesTheAdjacentOne() {
        val split = TerminalWorkspace.initial().split(SplitDirection.SideBySide)
        val (left, right) = split.groups

        val closed = split.closeTab(right.selectedTabId)

        assertEquals(left, closed.root)
        assertEquals(left.id, closed.focusedGroupId)
    }

    @Test
    fun closingTheFirstChildFocusesTheSecondChildsFirstGroup() {
        val workspace = TerminalWorkspace.initial()
            .split(SplitDirection.SideBySide)
            .split(SplitDirection.Stacked)
        val (left, topRight, _) = workspace.groups

        val closed = workspace.focusGroup(left.id).closeTab(left.selectedTabId)

        assertEquals(topRight.id, closed.focusedGroupId)
        assertEquals(2, closed.groups.size)
    }

    @Test
    fun closingAnUnfocusedGroupKeepsFocus() {
        val split = TerminalWorkspace.initial().split(SplitDirection.SideBySide)
        val (left, right) = split.groups

        val closed = split.closeTab(left.selectedTabId)

        assertEquals(right.id, closed.focusedGroupId)
    }

    @Test
    fun closingTheLastGroupLeavesAnEmptyPanel() {
        val workspace = TerminalWorkspace.initial()

        val closed = workspace.closeFocusedTab()

        assertEquals(1, closed.panels.size)
        assertEquals(workspace.selectedPanelId, closed.selectedPanelId)
        assertNull(closed.root)
        assertTrue(closed.groups.isEmpty())
        assertNull(closed.focusedTab)
        assertEquals(closed, closed.closeFocusedTab())
    }

    @Test
    fun selectingATabFocusesItsGroup() {
        val workspace = TerminalWorkspace.initial().split(SplitDirection.SideBySide)
        val left = workspace.groups.first()

        val selected = workspace.selectTab(left.selectedTabId)

        assertEquals(left.id, selected.focusedGroupId)
    }

    @Test
    fun adjacentTabWrapsAroundInsideTheFocusedGroup() {
        val workspace = TerminalWorkspace.initial().addTab()
        val (first, second) = workspace.groups.single().tabs

        assertEquals(first.id, workspace.selectAdjacentTab(1).focusedTab!!.id)
        assertEquals(first.id, workspace.selectAdjacentTab(-1).focusedTab!!.id)
        assertEquals(second.id, workspace.selectTabAt(1).focusedTab!!.id)
        assertEquals(workspace, workspace.selectTabAt(5))
    }

    @Test
    fun adjacentGroupFollowsScreenOrder() {
        val workspace = TerminalWorkspace.initial()
            .split(SplitDirection.SideBySide)
            .split(SplitDirection.Stacked)
        val ids = workspace.groups.map { it.id }

        assertEquals(ids[0], workspace.focusAdjacentGroup(1).focusedGroupId)
        assertEquals(ids[1], workspace.focusAdjacentGroup(-1).focusedGroupId)
    }

    @Test
    fun focusingAGroupInAnotherPanelSelectsThatPanel() {
        val workspace = TerminalWorkspace.initial()
        val firstGroup = workspace.groups.single()

        val focused = workspace.addPanel().focusGroup(firstGroup.id)

        assertEquals(workspace.selectedPanelId, focused.selectedPanelId)
        assertEquals(firstGroup.id, focused.focusedGroupId)
    }

    @Test
    fun ratioIsClamped() {
        val split = TerminalWorkspace.initial().split(SplitDirection.SideBySide)
        val id = assertIs<PaneNode.Split>(split.root).id

        assertEquals(0.9f, (split.setRatio(id, 2f).root as PaneNode.Split).ratio)
        assertEquals(0.1f, (split.setRatio(id, -1f).root as PaneNode.Split).ratio)
        assertEquals(0.3f, (split.setRatio(id, 0.3f).root as PaneNode.Split).ratio)
    }

    @Test
    fun resizeSplitAddsToTheCurrentRatio() {
        val split = TerminalWorkspace.initial().split(SplitDirection.SideBySide)
        val id = assertIs<PaneNode.Split>(split.root).id

        val resized = split.resizeSplit(id, 0.1f).resizeSplit(id, 0.1f)

        assertEquals(0.7f, (resized.root as PaneNode.Split).ratio, 0.0001f)
    }

    @Test
    fun idsAreNeverReused() {
        val workspace = TerminalWorkspace.initial().split(SplitDirection.SideBySide)
        val closed = workspace.closeFocusedTab()

        val again = closed.split(SplitDirection.SideBySide)

        assertTrue(again.focusedTab!!.id !in workspace.tabIds)
        assertTrue(again.focusedGroupId !in workspace.groups.map { it.id })
    }

    @Test
    fun newPanelIsNamedByCountSelectedAndEmpty() {
        val workspace = TerminalWorkspace.initial().addPanel()

        assertEquals(listOf("패널 1", "패널 2"), workspace.panels.map { it.name })
        assertEquals(workspace.panels.last().id, workspace.selectedPanelId)
        assertNull(workspace.selectedPanel!!.root)
        assertNull(workspace.selectedPanel!!.focusedGroupId)
        assertEquals(emptyList(), workspace.groups)
        assertNull(workspace.focusedTab)
        assertEquals(1, workspace.tabIds.size)
    }

    @Test
    fun newPanelTrimsNameAndDirectoryAndFallsBackWhenBlank() {
        val named = TerminalWorkspace.initial().addPanel(name = "  API ", directory = " /work/api ")
        val blank = TerminalWorkspace.initial().addPanel(name = "  ", directory = "  ")

        assertEquals("API", named.selectedPanel!!.name)
        assertEquals("/work/api", named.selectedPanel!!.directory)
        assertEquals("패널 2", blank.selectedPanel!!.name)
        assertEquals(null, blank.selectedPanel!!.directory)
    }

    @Test
    fun newPanelWithAClaudeSessionStartsWithThatClaudeTabInThePanelDirectory() {
        val workspace = TerminalWorkspace.initial().addPanel(directory = "/work", claudeSessionId = "session")

        val panel = workspace.selectedPanel!!
        assertEquals(workspace.panels.last().id, panel.id)
        assertEquals(1, panel.groups.size)
        assertEquals(TerminalTab(panel.tabs.single().id, TerminalProgram.Claude, "/work", "session"), workspace.focusedTab)
        assertEquals(panel.groups.single().id, workspace.focusedGroupId)
        assertEquals(workspace.tabIds.toSet().size, workspace.tabIds.size)
    }

    @Test
    fun firstTabOfANewPanelIsOpenedByAddTabInThePanelDirectory() {
        val empty = TerminalWorkspace.initial().addPanel(directory = "/work")

        val opened = empty.addTab(directory = empty.startDirectory())

        assertEquals(1, opened.selectedPanel!!.groups.size)
        assertEquals(TerminalTab(opened.focusedTab!!.id, TerminalProgram.Shell, "/work"), opened.focusedTab)
        assertEquals(opened.groups.single().id, opened.focusedGroupId)
    }

    @Test
    fun worktreePanelWithAClaudeSessionStartsWithThatClaudeTabInTheWorktreeFolder() {
        val workspace = TerminalWorkspace.initial().addPanel(name = "Jarvis", directory = "/work/jarvis")
        val parent = workspace.selectedPanel!!

        val added = workspace.addWorktreePanel(parent.id, name = "a", directory = "/work/a", claudeSessionId = "session")

        val child = added.selectedPanel!!
        assertEquals(parent.id, child.parentId)
        assertEquals(TerminalTab(child.tabs.single().id, TerminalProgram.Claude, "/work/a", "session"), added.focusedTab)
        assertEquals(workspace, workspace.addWorktreePanel(999, name = "a", directory = "/work/a", claudeSessionId = "session"))
    }

    @Test
    fun startDirectoryPrefersTheSelectedTabThenThePanelDirectory() {
        val workspace = TerminalWorkspace.initial().addPanel(directory = "/work").addTab(directory = "/work")
        val tab = workspace.focusedTab!!

        assertEquals("/work/api", workspace.setDirectory(tab.id, "/work/api").startDirectory())
        assertEquals("/work", workspace.copy(panels = workspace.panels.map { it.withTabDirectory(null) }).startDirectory())
        assertEquals("/work", workspace.closeTab(tab.id).startDirectory())
        assertEquals(null, TerminalWorkspace.initial().startDirectory())
    }

    @Test
    fun groupsBelongToTheSelectedPanel() {
        val workspace = TerminalWorkspace.initial().addTab().addPanel().addTab()
        val (first, second) = workspace.panels

        val split = workspace.split(SplitDirection.SideBySide)

        assertEquals(first, split.panels.first { it.id == first.id })
        assertEquals(2, split.panels.first { it.id == second.id }.groups.size)
    }

    @Test
    fun selectingAPanelShowsItsOwnGroups() {
        val workspace = TerminalWorkspace.initial().addTab()
        val first = workspace.panels.single()
        val withSecond = workspace.addPanel()

        val back = withSecond.selectPanel(first.id)

        assertEquals(first.groups, back.groups)
        assertEquals(first.focusedGroupId, back.focusedGroupId)
    }

    @Test
    fun renameTrimsAndIgnoresBlankNames() {
        val workspace = TerminalWorkspace.initial()
        val id = workspace.panels.single().id

        assertEquals("백엔드", workspace.renamePanel(id, "  백엔드 ").panels.single().name)
        assertEquals("패널 1", workspace.renamePanel(id, "   ").panels.single().name)
    }

    @Test
    fun theLastPanelIsNotClosed() {
        val workspace = TerminalWorkspace.initial()

        assertEquals(workspace, workspace.closePanel(workspace.panels.single().id))
    }

    @Test
    fun worktreePanelGoesRightAfterItsParentAndItsSiblingsAndIsSelected() {
        val workspace = TerminalWorkspace.initial().addPanel(name = "Jarvis", directory = "/work/jarvis").addPanel(name = "API")
        val (first, parent, api) = workspace.panels

        val one = workspace.addWorktreePanel(parent.id, name = "feature/login", directory = "/work/jarvis-worktrees/feature/login")
        val two = one.addWorktreePanel(parent.id, name = "fix", directory = "/work/jarvis-worktrees/fix")

        val (login, fix) = two.children(parent.id)
        assertEquals(listOf(first.id, parent.id, login.id, fix.id, api.id), two.panels.map { it.id })
        assertEquals(listOf(first, parent, api).map { it.id }, two.topLevelPanels.map { it.id })
        assertEquals(fix.id, two.selectedPanelId)
        assertEquals(parent.id, fix.parentId)
        assertEquals("fix", fix.name)
        assertEquals("/work/jarvis-worktrees/fix", fix.directory)
        assertNull(fix.root)
        assertEquals("/work/jarvis-worktrees/fix", two.startDirectory())
    }

    @Test
    fun worktreePanelMadeFromAWorktreePanelIsASiblingUnderTheSameParent() {
        val workspace = TerminalWorkspace.initial().addPanel(directory = "/work/jarvis")
        val parent = workspace.panels.last()
        val withChild = workspace.addWorktreePanel(parent.id, name = "a", directory = "/work/a")
        val child = withChild.selectedPanel!!

        val withSibling = withChild.addWorktreePanel(child.id, name = "b", directory = "/work/b")

        assertEquals(listOf("a", "b"), withSibling.children(parent.id).map { it.name })
        assertEquals(parent.id, withSibling.selectedPanel!!.parentId)
        assertEquals(emptyList(), withSibling.children(child.id))
    }

    @Test
    fun worktreePanelRemembersItsBranchAndBaseBranchAndDropsBlankOnes() {
        val workspace = TerminalWorkspace.initial().addPanel(directory = "/work/jarvis")
        val parent = workspace.panels.last()

        val remembered = workspace
            .addWorktreePanel(parent.id, name = "a", directory = "/work/a", branch = " a ", baseBranch = " main ")
            .selectedPanel!!
        val blank = workspace
            .addWorktreePanel(parent.id, name = "b", directory = "/work/b", branch = "b", baseBranch = "  ")
            .selectedPanel!!

        assertEquals("a", remembered.branch)
        assertEquals("main", remembered.baseBranch)
        assertEquals("b", blank.branch)
        assertNull(blank.baseBranch)
        assertNull(parent.branch)

        // 이름을 바꿔도 브랜치는 그대로다.
        val renamed = workspace.addWorktreePanel(parent.id, branch = "a", baseBranch = "main").let { it.renamePanel(it.selectedPanelId!!, "x") }
        assertEquals("x", renamed.selectedPanel!!.name)
        assertEquals("a", renamed.selectedPanel!!.branch)
    }

    @Test
    fun worktreePanelForAnUnknownParentChangesNothing() {
        val workspace = TerminalWorkspace.initial()

        assertEquals(workspace, workspace.addWorktreePanel(999, name = "a", directory = "/work/a"))
    }

    @Test
    fun closingAParentClosesItsWorktreePanelsToo() {
        val workspace = TerminalWorkspace.initial().addPanel(directory = "/work/jarvis").addTab()
        val parent = workspace.panels.last()
        val withChildren = workspace
            .addWorktreePanel(parent.id, name = "a", directory = "/work/a").addTab()
            .addWorktreePanel(parent.id, name = "b", directory = "/work/b").addTab()
            .addPanel(name = "after")
        val children = withChildren.children(parent.id)
        val selectedChild = withChildren.selectPanel(children.first().id)

        val closed = selectedChild.closePanel(parent.id)

        assertEquals(listOf(workspace.panels.first().id, withChildren.panels.last().id), closed.panels.map { it.id })
        assertEquals(withChildren.panels.last().id, closed.selectedPanelId)
        assertTrue(children.flatMap { it.tabs }.none { it.id in closed.tabIds })
        assertTrue(parent.tabs.none { it.id in closed.tabIds })
    }

    @Test
    fun closingAWorktreePanelKeepsItsParent() {
        val workspace = TerminalWorkspace.initial().addPanel(directory = "/work/jarvis")
        val parent = workspace.panels.last()
        val withChild = workspace.addWorktreePanel(parent.id, name = "a", directory = "/work/a")
        val child = withChild.selectedPanel!!

        val closed = withChild.closePanel(child.id)

        assertEquals(workspace.panels.map { it.id }, closed.panels.map { it.id })
        assertEquals(parent.id, closed.selectedPanelId)
    }

    @Test
    fun theLastFamilyIsNotClosed() {
        val workspace = TerminalWorkspace.initial()
        val parent = workspace.panels.single()
        val withChild = workspace.addWorktreePanel(parent.id, name = "a", directory = "/work/a")

        assertEquals(withChild, withChild.closePanel(parent.id))
        assertEquals(1, withChild.closePanel(withChild.selectedPanelId!!).panels.size)
    }

    @Test
    fun closingTheSelectedPanelSelectsTheNextOne() {
        val workspace = TerminalWorkspace.initial().addPanel().addTab().addPanel()
        val (first, middle, last) = workspace.panels

        val closed = workspace.selectPanel(middle.id).closePanel(middle.id)

        assertEquals(listOf(first.id, last.id), closed.panels.map { it.id })
        assertEquals(last.id, closed.selectedPanelId)
        assertTrue(middle.tabs.none { it.id in closed.tabIds })
    }

    @Test
    fun claudeTabKeepsItsProgramAndSessionId() {
        val workspace = TerminalWorkspace.initial()
            .addTab(program = TerminalProgram.Claude, directory = "/work", claudeSessionId = "session")

        val tab = workspace.focusedTab!!

        assertEquals(TerminalProgram.Claude, tab.program)
        assertEquals("/work", tab.directory)
        assertEquals("session", tab.claudeSessionId)
    }

    @Test
    fun splittingAClaudeGroupOpensAShellInTheGivenDirectory() {
        val workspace = TerminalWorkspace.initial()
            .addTab(program = TerminalProgram.Claude, claudeSessionId = "session")
            .split(SplitDirection.SideBySide, directory = "/work")

        val tab = workspace.focusedTab!!

        assertEquals(TerminalProgram.Shell, tab.program)
        assertEquals("/work", tab.directory)
        assertNull(tab.claudeSessionId)
    }

    @Test
    fun directoryIsRecordedOnTheTab() {
        val workspace = TerminalWorkspace.initial()
        val id = workspace.focusedTab!!.id

        assertEquals("/tmp", workspace.setDirectory(id, "/tmp").focusedTab!!.directory)
    }

    @Test
    fun dockingOnTheRightEdgeSplitsSideBySideWithTheDraggedTabSecond() {
        val workspace = TerminalWorkspace.initial().addTab()
        val group = workspace.groups.single()
        val (first, second) = group.tabs

        val docked = workspace.dockTab(second.id, group.id, DockEdge.Right)
        val root = assertIs<PaneNode.Split>(docked.root)

        assertEquals(SplitDirection.SideBySide, root.direction)
        assertEquals(PaneNode.Group(group.id, listOf(first), first.id), root.first)
        val added = assertIs<PaneNode.Group>(root.second)
        assertEquals(listOf(second), added.tabs)
        assertEquals(second.id, added.selectedTabId)
        assertEquals(added.id, docked.focusedGroupId)
        assertEquals(0.5f, root.ratio)
        assertEquals(workspace.tabIds.toSet(), docked.tabIds.toSet())
    }

    @Test
    fun dockingOnTheLeftOrTopEdgePutsTheNewGroupFirst() {
        val workspace = TerminalWorkspace.initial().addTab()
        val group = workspace.groups.single()
        val second = group.tabs.last()

        val left = assertIs<PaneNode.Split>(workspace.dockTab(second.id, group.id, DockEdge.Left).root)
        val top = assertIs<PaneNode.Split>(workspace.dockTab(second.id, group.id, DockEdge.Top).root)
        val bottom = assertIs<PaneNode.Split>(workspace.dockTab(second.id, group.id, DockEdge.Bottom).root)

        assertEquals(SplitDirection.SideBySide, left.direction)
        assertEquals(listOf(second), assertIs<PaneNode.Group>(left.first).tabs)
        assertEquals(SplitDirection.Stacked, top.direction)
        assertEquals(listOf(second), assertIs<PaneNode.Group>(top.first).tabs)
        assertEquals(SplitDirection.Stacked, bottom.direction)
        assertEquals(listOf(second), assertIs<PaneNode.Group>(bottom.second).tabs)
    }

    @Test
    fun dockingInTheCenterMovesTheTabIntoThatGroup() {
        val workspace = TerminalWorkspace.initial().split(SplitDirection.SideBySide).addTab()
        val (left, right) = workspace.groups
        val moved = right.tabs.last()

        val docked = workspace.dockTab(moved.id, left.id, DockEdge.Center)
        val (newLeft, newRight) = docked.groups

        assertEquals(left.tabs + moved, newLeft.tabs)
        assertEquals(moved.id, newLeft.selectedTabId)
        assertEquals(right.tabs - moved, newRight.tabs)
        assertEquals(left.id, docked.focusedGroupId)
        assertEquals(workspace.tabIds.toSet(), docked.tabIds.toSet())
    }

    @Test
    fun dockingTheLastTabOfAGroupRemovesThatGroup() {
        val workspace = TerminalWorkspace.initial().split(SplitDirection.SideBySide)
        val (left, right) = workspace.groups

        val docked = workspace.dockTab(right.selectedTabId, left.id, DockEdge.Center)
        val group = assertIs<PaneNode.Group>(docked.root)

        assertEquals(left.tabs + right.tabs, group.tabs)
        assertEquals(right.selectedTabId, group.selectedTabId)
    }

    @Test
    fun dockingIntoAnotherGroupsEdgeKeepsTheSourceGroup() {
        val workspace = TerminalWorkspace.initial().split(SplitDirection.SideBySide).addTab()
        val (left, right) = workspace.groups
        val moved = right.tabs.last()

        val docked = workspace.dockTab(moved.id, left.id, DockEdge.Bottom)
        val root = assertIs<PaneNode.Split>(docked.root)
        val leftSide = assertIs<PaneNode.Split>(root.first)

        assertEquals(SplitDirection.Stacked, leftSide.direction)
        assertEquals(left, leftSide.first)
        assertEquals(listOf(moved), assertIs<PaneNode.Group>(leftSide.second).tabs)
        assertEquals(right.tabs - moved, assertIs<PaneNode.Group>(root.second).tabs)
    }

    @Test
    fun dockingWhereNothingWouldChangeDoesNothing() {
        val single = TerminalWorkspace.initial()
        val group = single.groups.single()
        val tab = group.selectedTabId
        val two = single.addTab()

        assertEquals(single, single.dockTab(tab, group.id, DockEdge.Right))
        assertEquals(two, two.dockTab(tab, group.id, DockEdge.Center))
        assertEquals(two, two.dockTab(999, group.id, DockEdge.Right))
        assertEquals(two, two.dockTab(tab, 999, DockEdge.Right))
    }

    @Test
    fun dockingUsesFreshIds() {
        val workspace = TerminalWorkspace.initial().addTab()
        val group = workspace.groups.single()

        val docked = workspace.dockTab(group.tabs.last().id, group.id, DockEdge.Right)
        val root = assertIs<PaneNode.Split>(docked.root)

        assertEquals(workspace.nextId, root.id)
        assertEquals(workspace.nextId + 1, assertIs<PaneNode.Group>(root.second).id)
        assertEquals(workspace.nextId + 2, docked.nextId)
    }

    @Test
    fun browserTabIsAppendedToTheGroupAndSelected() {
        val workspace = TerminalWorkspace.initial()

        val added = workspace.addTab(program = TerminalProgram.Browser, url = "https://example.com")
        val tab = added.focusedTab!!

        assertEquals(2, added.groups.single().tabs.size)
        assertEquals(TerminalProgram.Browser, tab.program)
        assertEquals("https://example.com", tab.url)
    }

    @Test
    fun setUrlChangesOnlyThatTab() {
        val workspace = TerminalWorkspace.initial().addTab(program = TerminalProgram.Browser, url = "https://a.com")
        val (shell, browser) = workspace.groups.single().tabs

        val changed = workspace.setUrl(browser.id, "https://b.com")

        assertEquals(listOf(shell, browser.copy(url = "https://b.com")), changed.groups.single().tabs)
        assertTrue(changed.setUrl(browser.id, "https://b.com") === changed)
    }

    @Test
    fun splittingABrowserTabOpensAShellAtHome() {
        val workspace = TerminalWorkspace.initial().addTab(program = TerminalProgram.Browser, url = "https://a.com")

        val split = workspace.split(SplitDirection.SideBySide, workspace.focusedTab?.directory)
        val tab = split.focusedTab!!

        assertEquals(TerminalProgram.Shell, tab.program)
        assertNull(tab.directory)
        assertNull(tab.url)
    }

    @Test
    fun deviceTabIsAppendedToTheGroupAndSelected() {
        val workspace = TerminalWorkspace.initial()

        val added = workspace.addTab(program = TerminalProgram.Device, deviceId = "emulator-5554", deviceName = "Pixel 9")
        val tab = added.focusedTab!!

        assertEquals(2, added.groups.single().tabs.size)
        assertEquals(TerminalProgram.Device, tab.program)
        assertEquals("emulator-5554", tab.deviceId)
        assertEquals("Pixel 9", tab.deviceName)
    }

    @Test
    fun splittingADeviceTabOpensAShellInThePanelFolder() {
        val workspace = TerminalWorkspace.initial()
            .let { it.copy(panels = it.panels.map { panel -> panel.copy(directory = "/work") }) }
            .addTab(program = TerminalProgram.Device, deviceId = "emulator-5554", deviceName = "Pixel 9")

        val split = workspace.split(SplitDirection.SideBySide, workspace.startDirectory())
        val tab = split.focusedTab!!

        assertEquals(TerminalProgram.Shell, tab.program)
        assertEquals("/work", tab.directory)
        assertNull(tab.deviceId)
    }

    @Test
    fun appendedTabGoesToTheGroupEndWithoutTakingSelectionOrFocus() {
        // 패널 둘: 첫 패널을 나눠 그룹 둘, 둘째 패널이 선택된 상태에서 첫 패널의 첫 그룹에 붙인다.
        val split = TerminalWorkspace.initial().split(SplitDirection.SideBySide)
        val firstPanel = split.selectedPanel!!
        val target = firstPanel.groups.first()
        val workspace = split.addPanel().addTab()

        val appended = workspace.appendTab(target.id, TerminalProgram.Browser, url = "https://example.com")
        val group = appended.panels.first { it.id == firstPanel.id }.groups.first { it.id == target.id }

        assertEquals(target.tabs.size + 1, group.tabs.size)
        assertEquals(TerminalProgram.Browser, group.tabs.last().program)
        assertEquals("https://example.com", group.tabs.last().url)
        assertEquals(workspace.nextId, group.tabs.last().id)
        assertEquals(workspace.nextId + 1, appended.nextId)
        assertEquals(target.selectedTabId, group.selectedTabId)
        assertEquals(workspace.selectedPanelId, appended.selectedPanelId)
        assertEquals(firstPanel.focusedGroupId, appended.panels.first { it.id == firstPanel.id }.focusedGroupId)
        assertEquals(workspace.focusedTab, appended.focusedTab)
    }

    @Test
    fun appendingToAnUnknownGroupChangesNothing() {
        val workspace = TerminalWorkspace.initial()

        assertEquals(workspace, workspace.appendTab(groupId = -1, program = TerminalProgram.Browser))
    }

    @Test
    fun findsTheClaudeTabInAnyPanel() {
        val first = TerminalWorkspace.initial()
        val claudeGroup = first.focusedGroup!!.id
        val workspace = first
            .addTab(claudeGroup, TerminalProgram.Claude, claudeSessionId = "session-1")
            .addPanel()
            .addTab()

        val location = workspace.findClaudeTab("session-1")!!

        assertEquals(first.selectedPanelId, location.panel.id)
        assertEquals(claudeGroup, location.group.id)
        assertEquals("session-1", location.tab.claudeSessionId)
        assertNull(workspace.findClaudeTab("unknown"))
    }

    @Test
    fun tabKindFollowsTheProgramAndTheDevicePlatform() {
        assertEquals(TerminalTabKind.Terminal, TerminalTab(1).kind)
        assertEquals(TerminalTabKind.Claude, TerminalTab(1, TerminalProgram.Claude, claudeSessionId = "s").kind)
        assertEquals(TerminalTabKind.Browser, TerminalTab(1, TerminalProgram.Browser, url = "https://a.com").kind)
        assertEquals(TerminalTabKind.Android, TerminalTab(1, TerminalProgram.Device, deviceId = "d", devicePlatform = DevicePlatform.Android).kind)
        assertEquals(TerminalTabKind.IOS, TerminalTab(1, TerminalProgram.Device, deviceId = "d", devicePlatform = DevicePlatform.IOS).kind)
        assertEquals(TerminalTabKind.Device, TerminalTab(1, TerminalProgram.Device, deviceId = "d").kind)
        assertEquals(TerminalTabKind.File, TerminalTab(1, TerminalProgram.File, filePath = "/a").kind)
    }

    @Test
    fun openingAFileAppendsAFileTabToTheFocusedGroupAndSelectsIt() {
        val workspace = TerminalWorkspace.initial()

        val opened = workspace.openFile("/work/README.md")
        val tab = opened.focusedTab!!

        assertEquals(2, opened.groups.single().tabs.size)
        assertEquals(TerminalProgram.File, tab.program)
        assertEquals(TerminalTabKind.File, tab.kind)
        assertEquals("/work/README.md", tab.filePath)
    }

    @Test
    fun openingAFileInAnEmptyPanelCreatesAGroup() {
        val workspace = TerminalWorkspace.initial().addPanel(name = "빈", directory = "/work")

        val opened = workspace.openFile("/work/a.txt")

        assertEquals(listOf("/work/a.txt"), opened.selectedPanel!!.tabs.map { it.filePath })
        assertEquals(opened.groups.single().id, opened.focusedGroupId)
    }

    @Test
    fun openingAnOpenFileAgainSelectsItsTabInsteadOfAddingOne() {
        val workspace = TerminalWorkspace.initial().openFile("/work/a.txt").addTab()
        val fileTab = workspace.tabs.single { it.program == TerminalProgram.File }

        val opened = workspace.openFile("/work/a.txt")

        assertEquals(workspace.tabIds, opened.tabIds)
        assertEquals(fileTab.id, opened.focusedTab!!.id)
    }

    @Test
    fun theSameFileInAnotherPanelIsOpenedAgain() {
        val workspace = TerminalWorkspace.initial().openFile("/work/a.txt").addPanel().addTab()

        val opened = workspace.openFile("/work/a.txt")

        assertEquals(2, opened.tabs.count { it.filePath == "/work/a.txt" })
        assertEquals(opened.panels.last().id, opened.selectedPanelId)
    }

    @Test
    fun sideBarFollowsThePanelFolderThenTheFocusedTabDirectory() {
        val withFolder = TerminalWorkspace.initial().addPanel(directory = "/work").addTab(directory = "/work/app")
        assertEquals("/work", withFolder.sideBarDirectory)

        val withoutFolder = TerminalWorkspace.initial().addPanel().addTab(directory = "/tmp/x")
        assertEquals("/tmp/x", withoutFolder.sideBarDirectory)

        assertNull(TerminalWorkspace.initial().sideBarDirectory)
    }

    @Test
    fun deviceTabKeepsThePlatformItWasOpenedWith() {
        val added = TerminalWorkspace.initial()
            .addTab(program = TerminalProgram.Device, deviceId = "sim", deviceName = "iPhone 15", devicePlatform = DevicePlatform.IOS)

        assertEquals(DevicePlatform.IOS, added.focusedTab!!.devicePlatform)
        assertEquals(TerminalTabKind.IOS, added.focusedTab!!.kind)
    }

    @Test
    fun renamingATabTrimsTheName() {
        val workspace = TerminalWorkspace.initial()
        val tabId = workspace.focusedTab!!.id

        val renamed = workspace.renameTab(tabId, "  서버 로그  ")

        assertEquals("서버 로그", renamed.focusedTab!!.name)
    }

    @Test
    fun renamingATabToBlankClearsTheName() {
        val workspace = TerminalWorkspace.initial()
        val tabId = workspace.focusedTab!!.id

        val cleared = workspace.renameTab(tabId, "서버").renameTab(tabId, "   ")

        assertNull(cleared.focusedTab!!.name)
    }

    @Test
    fun renamingToTheSameNameReturnsTheSameWorkspace() {
        val workspace = TerminalWorkspace.initial().let { it.renameTab(it.focusedTab!!.id, "서버") }

        assertSame(workspace, workspace.renameTab(workspace.focusedTab!!.id, "서버"))
        assertSame(workspace, workspace.renameTab(-1, "다른"))
    }

    @Test
    fun theNameFollowsATabDockedIntoAnotherGroup() {
        val workspace = TerminalWorkspace.initial().split(SplitDirection.SideBySide)
        val (left, right) = workspace.groups
        val moved = left.tabs.single().id

        val docked = workspace.renameTab(moved, "서버").dockTab(moved, right.id, DockEdge.Center)

        assertEquals("서버", docked.tabs.single { it.id == moved }.name)
    }
}

private fun TerminalPanel.withTabDirectory(directory: String?): TerminalPanel =
    copy(root = (root as PaneNode.Group).let { group -> group.copy(tabs = group.tabs.map { it.copy(directory = directory) }) })
