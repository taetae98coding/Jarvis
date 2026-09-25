package io.github.taetae98coding.jarvis.domain.terminal

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertSame

class TerminalRunWorkspaceTest {
    // 최상위 패널 1(폴더 /repo)과 그 워크트리 패널.
    private val workspace = TerminalWorkspace.initial()
        .let { it.replaceDirectory() }
        .addWorktreePanel(parentId = 1, name = "feature", directory = "/repo-feature")

    private val worktreePanelId = workspace.panels.last().id

    private fun TerminalWorkspace.replaceDirectory(): TerminalWorkspace =
        TerminalWorkspace(panels = panels.map { it.copy(directory = "/repo") }, selectedPanelId = selectedPanelId, nextId = nextId)

    @Test
    fun commandsLiveOnTheTopLevelPanelAndAreSharedWithWorktrees() {
        val added = workspace.addCommand(worktreePanelId, "  테스트 ", "  ./gradlew test  ")

        val command = added.commandsOf(1).single()
        assertEquals(TerminalCommand(workspace.nextId, "테스트", "./gradlew test"), command)
        assertEquals(listOf(command), added.commandsOf(worktreePanelId))
        assertEquals(emptyList(), added.panels.last().commands)
    }

    @Test
    fun blankCommandsAreNotAdded() {
        assertSame(workspace, workspace.addCommand(1, "이름", "   "))
    }

    @Test
    fun anEmptyTitleFallsBackToTheCommand() {
        val command = workspace.addCommand(1, " ", "make").commandsOf(1).single()

        assertNull(command.title)
        assertEquals("make", command.label)
    }

    @Test
    fun commandsAreEditedAndRemovedThroughAWorktreePanel() {
        val added = workspace.addCommand(1, null, "make")
        val id = added.commandsOf(1).single().id

        val edited = added.editCommand(worktreePanelId, id, "빌드", "make all")
        assertEquals(listOf(TerminalCommand(id, "빌드", "make all")), edited.commandsOf(1))

        assertEquals(emptyList(), edited.removeCommand(worktreePanelId, id).commandsOf(1))
    }

    @Test
    fun runOpensACommandTabAtTheEndOfTheGroupAndFocusesIt() {
        val group = workspace.selectPanel(1).focusedGroup!!

        val run = workspace.selectPanel(1).runInGroup(group.id, "/repo", "make", "빌드")

        val runGroup = run.focusedGroup!!
        val tab = runGroup.selectedTab
        assertEquals(group.id, runGroup.id)
        assertEquals(TerminalProgram.Shell, tab.program)
        assertEquals("make", tab.command)
        assertEquals("빌드", tab.commandTitle)
        assertEquals("/repo", tab.directory)
        assertEquals(TerminalTabKind.Run, tab.kind)
    }

    @Test
    fun aMirrorSplitsTheRunGroupAndKeepsFocusOnTheRunTab() {
        val start = workspace.selectPanel(1)
        val group = start.focusedGroup!!
        val mirror = RunMirror("emulator-5554", "Pixel 9", DevicePlatform.Android)

        val run = start.runInGroup(group.id, "/repo", "gradle", "debug · Pixel 9", mirror)

        val root = assertIs<PaneNode.Split>(run.selectedPanel!!.root)
        assertEquals(SplitDirection.SideBySide, root.direction)
        assertEquals(TerminalWorkspace.RunRatio, root.ratio)
        val deviceTab = assertIs<PaneNode.Group>(root.second).selectedTab
        assertEquals(TerminalProgram.Device, deviceTab.program)
        assertEquals("emulator-5554", deviceTab.deviceId)
        assertEquals("Pixel 9", deviceTab.deviceName)
        assertEquals(DevicePlatform.Android, deviceTab.devicePlatform)
        assertEquals(group.id, run.focusedGroup!!.id)
        assertEquals("gradle", run.focusedTab!!.command)
    }

    @Test
    fun anExistingDeviceTabIsSelectedInsteadOfOpeningAnother() {
        val start = workspace.selectPanel(1)
            .addTab(program = TerminalProgram.Device, deviceId = "emulator-5554", deviceName = "Pixel 9")
            .split(SplitDirection.Stacked)
        val deviceTab = start.tabs.first { it.program == TerminalProgram.Device }
        val deviceGroup = start.groups.first { group -> group.tabs.any { it.id == deviceTab.id } }
            .let { start.selectTab(it.tabs.first().id) }
        val focused = deviceGroup.groups.last()

        val run = deviceGroup.focusGroup(focused.id).runInGroup(focused.id, "/repo", "gradle", "t", RunMirror("emulator-5554", "Pixel 9", DevicePlatform.Android))

        assertEquals(1, run.tabs.count { it.program == TerminalProgram.Device })
        assertEquals(deviceTab.id, run.groups.first().selectedTabId)
        assertEquals(focused.id, run.focusedGroup!!.id)
    }

    @Test
    fun runChoicesAreRememberedOnTheTopLevelPanel() {
        val android = AndroidRunChoice(":androidApp", "debug", "emulator-5554")
        val ios = IosRunChoice("iosApp", "Debug", "UDID")

        val remembered = workspace.rememberAndroidRun(worktreePanelId, android).rememberIosRun(worktreePanelId, ios)

        assertEquals(android, remembered.runOwner(worktreePanelId)!!.androidRun)
        assertEquals(ios, remembered.panels.first().iosRun)
        assertNull(remembered.panels.last().androidRun)
    }
}
