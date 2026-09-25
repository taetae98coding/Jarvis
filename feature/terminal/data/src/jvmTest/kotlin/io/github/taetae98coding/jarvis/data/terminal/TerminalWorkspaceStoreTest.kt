package io.github.taetae98coding.jarvis.data.terminal

import io.github.taetae98coding.jarvis.domain.terminal.AndroidRunChoice
import io.github.taetae98coding.jarvis.domain.terminal.ClaudeActivity
import io.github.taetae98coding.jarvis.domain.terminal.DevicePlatform
import io.github.taetae98coding.jarvis.domain.terminal.IosRunChoice
import io.github.taetae98coding.jarvis.domain.terminal.PaneNode
import io.github.taetae98coding.jarvis.domain.terminal.SplitDirection
import io.github.taetae98coding.jarvis.domain.terminal.TerminalProgram
import io.github.taetae98coding.jarvis.domain.terminal.TerminalTab
import io.github.taetae98coding.jarvis.domain.terminal.TerminalWorkspace
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toOkioPath
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * 실제 파일에 쓰고, 새 DataStore 로 다시 읽는다 — 앱을 끝냈다 다시 켠 것과 같다. 한 파일에 DataStore 가
 * 동시에 둘이면 안 되므로 파일마다 새 경로를 쓰고, 다시 읽을 때만 같은 경로를 쓴다.
 */
class TerminalWorkspaceStoreTest {
    private fun newPath(): Path =
        Files.createTempDirectory("jarvis-store").toOkioPath() / "nested" / TerminalWorkspaceFileName

    private fun repository(path: Path) = DefaultTerminalWorkspaceRepository(terminalWorkspaceStore(FileSystem.SYSTEM, path))

    @Test
    fun missingFileIsTheInitialWorkspace() = runTest {
        assertEquals(TerminalWorkspace.initial(), repository(newPath()).observeWorkspace().first())
    }

    @Test
    fun savedWorkspaceIsReadBackByANewStore() = runTest {
        val path = newPath()
        val change = repository(path).updateWorkspace { workspace ->
            val renamed = workspace.renamePanel(workspace.panels.single().id, "백엔드")
            renamed
                .addTab(program = TerminalProgram.Claude, directory = "/work", claudeSessionId = "e0c0")
                .addTab(program = TerminalProgram.Browser, url = "https://example.com")
                .addTab(program = TerminalProgram.Device, deviceId = "adb-R54T (2)._adb-tls-connect._tcp", deviceName = "SM-X906N")
                .split(SplitDirection.Stacked, directory = "/work")
                .addPanel()
        }

        val reopened = DefaultTerminalWorkspaceRepository(terminalWorkspaceStoreForRead(path)).observeWorkspace().first()

        assertEquals(change.after, reopened)
        assertEquals(listOf("백엔드", "패널 2"), reopened.panels.map { it.name })
    }

    @Test
    fun tabNamesAndDevicePlatformsAreReadBackByANewStore() = runTest {
        val path = newPath()
        val change = repository(path).updateWorkspace { workspace ->
            workspace
                .renameTab(workspace.focusedTab!!.id, "서버")
                .addTab(program = TerminalProgram.Device, deviceId = "emulator-5554", deviceName = "Pixel 9", devicePlatform = DevicePlatform.Android)
                .addTab(program = TerminalProgram.Device, deviceId = "sim", deviceName = "iPhone 15", devicePlatform = DevicePlatform.IOS)
        }

        val reopened = DefaultTerminalWorkspaceRepository(terminalWorkspaceStoreForRead(path)).observeWorkspace().first()

        assertEquals(change.after, reopened)
        assertEquals(listOf("서버", null, null), reopened.tabs.map { it.name })
        assertEquals(listOf(null, DevicePlatform.Android, DevicePlatform.IOS), reopened.tabs.map { it.devicePlatform })
    }

    // 이름·플랫폼 키가 없던 때의 파일. 두 값 모두 없는 것으로 읽는다.
    @Test
    fun fileWithoutTabNamesOrDevicePlatformsReadsThemAsNull() = runTest {
        val path = newPath()
        FileSystem.SYSTEM.createDirectories(path.parent!!)
        FileSystem.SYSTEM.write(path) {
            writeUtf8(
                """
                {"panels": [{"id": 1, "name": "패널 1", "focusedGroupId": 2,
                  "root": {"type": "group", "id": 2, "selectedTabId": 3,
                           "tabs": [{"id": 3, "program": "device", "deviceId": "emulator-5554", "deviceName": "Pixel 9"}]}}],
                 "selectedPanelId": 1, "nextId": 4}
                """.trimIndent(),
            )
        }

        val tab = repository(path).observeWorkspace().first().tabs.single()

        assertNull(tab.name)
        assertNull(tab.devicePlatform)
    }

    @Test
    fun claudeCheckedAtIsReadBackByANewStore() = runTest {
        val path = newPath()
        val change = repository(path).updateWorkspace { workspace ->
            workspace
                .addTab(program = TerminalProgram.Claude, claudeSessionId = "e0c0")
                .checkVisibleClaudeTabs(mapOf("e0c0" to ClaudeActivity.Finished(at = 1_790_000_000_000)))
        }

        val reopened = DefaultTerminalWorkspaceRepository(terminalWorkspaceStoreForRead(path)).observeWorkspace().first()

        assertEquals(change.after, reopened)
        assertEquals(1_790_000_000_000, reopened.tabs.single { it.claudeSessionId == "e0c0" }.claudeCheckedAt)
    }

    @Test
    fun fileTabsAreReadBackWithTheirPath() = runTest {
        val path = newPath()
        val change = repository(path).updateWorkspace { it.openFile("/work/README.md") }

        val reopened = DefaultTerminalWorkspaceRepository(terminalWorkspaceStoreForRead(path)).observeWorkspace().first()

        assertEquals(change.after, reopened)
        assertEquals("/work/README.md", reopened.tabs.single { it.program == TerminalProgram.File }.filePath)
    }

    @Test
    fun commitFileTabsAreReadBackWithTheirPathAndHash() = runTest {
        val path = newPath()
        val change = repository(path).updateWorkspace { it.openCommitFile("/work/README.md", "0123456789abcdef") }

        val reopened = DefaultTerminalWorkspaceRepository(terminalWorkspaceStoreForRead(path)).observeWorkspace().first()

        assertEquals(change.after, reopened)
        assertEquals("0123456789abcdef", reopened.tabs.single { it.program == TerminalProgram.File }.commitHash)
    }

    @Test
    fun aFileTabWithoutAPathIsReadAsAShell() {
        val dto = TerminalWorkspaceDto(
            panels = listOf(
                TerminalPanelDto(
                    id = 1,
                    name = "패널 1",
                    root = PaneNodeDto.Group(id = 2, tabs = listOf(TerminalTabDto(id = 3, program = "file"), TerminalTabDto(id = 4, program = "file", filePath = "/a"))),
                ),
            ),
            selectedPanelId = 1,
            nextId = 5,
        )

        assertEquals(listOf(TerminalProgram.Shell, TerminalProgram.File), dto.toDomain().tabs.map { it.program })
    }

    @Test
    fun commandTabsKeepTheirCommandWhileTheAppIsRunning() = runTest {
        val repository = repository(newPath())
        val change = repository.updateWorkspace { it.runInGroup(null, "/repo", "make", "빌드", typed = true) }

        val tab = repository.observeWorkspace().first().tabs.last()
        assertEquals(change.after.tabs.last(), tab)
        assertEquals("make", tab.command)
        assertEquals("빌드", tab.commandTitle)
        assertEquals(true, tab.commandTyped)
        assertEquals("make", repository.updateWorkspace { it }.after.tabs.last().command)
    }

    @Test
    fun commandsAndRunChoicesAreReadBackButCommandTabsBecomeShells() = runTest {
        val path = newPath()
        val change = repository(path).updateWorkspace { workspace ->
            workspace
                .addCommand(1, "테스트", "./gradlew test")
                .addCommand(1, null, "make")
                .rememberAndroidRun(1, AndroidRunChoice(":androidApp", "debug", "avd:Pixel_9"))
                .rememberIosRun(1, IosRunChoice("iosApp", "Debug", "UDID"))
                .runInGroup(null, "/repo", "make", "make", typed = true)
        }

        val reopened = DefaultTerminalWorkspaceRepository(terminalWorkspaceStoreForRead(path)).observeWorkspace().first()

        val panel = reopened.panels.single()
        assertEquals(change.after.panels.single().commands, panel.commands)
        assertEquals(AndroidRunChoice(":androidApp", "debug", "avd:Pixel_9"), panel.androidRun)
        assertEquals(IosRunChoice("iosApp", "Debug", "UDID"), panel.iosRun)
        val restored = reopened.tabs.last()
        assertEquals(TerminalTab(change.after.tabs.last().id, directory = "/repo"), restored)
    }

    @Test
    fun panelDirectoryIsReadBackByANewStore() = runTest {
        val path = newPath()
        val change = repository(path).updateWorkspace {
            it.addPanel(name = "API", directory = "/work/api")
        }

        val reopened = DefaultTerminalWorkspaceRepository(terminalWorkspaceStoreForRead(path)).observeWorkspace().first()

        assertEquals(change.after, reopened)
        assertEquals("/work/api", reopened.panels.last().directory)
    }

    @Test
    fun worktreePanelParentIsReadBackByANewStore() = runTest {
        val path = newPath()
        val change = repository(path).updateWorkspace {
            val withRepo = it.addPanel(name = "Jarvis", directory = "/work/jarvis")
            withRepo.addWorktreePanel(
                withRepo.selectedPanelId!!,
                name = "fix",
                directory = "/work/jarvis-worktrees/fix",
                branch = "fix",
                baseBranch = "main",
            )
        }

        val reopened = DefaultTerminalWorkspaceRepository(terminalWorkspaceStoreForRead(path)).observeWorkspace().first()

        assertEquals(change.after, reopened)
        assertEquals(reopened.panels[1].id, reopened.panels.last().parentId)
        assertEquals("fix", reopened.panels.last().branch)
        assertEquals("main", reopened.panels.last().baseBranch)
    }

    // 부모가 사라졌거나 부모 자신이 워크트리 패널인 parentId 는 최상위로 읽는다.
    @Test
    fun orphanedOrNestedParentsAreReadAsTopLevel() = runTest {
        val path = newPath()
        FileSystem.SYSTEM.createDirectories(path.parent!!)
        FileSystem.SYSTEM.write(path) {
            writeUtf8(
                """
                {"panels": [
                  {"id": 1, "name": "main", "focusedGroupId": 2, "root": {"type": "group", "id": 2, "selectedTabId": 3, "tabs": [{"id": 3}]}},
                  {"id": 4, "name": "child", "parentId": 1, "focusedGroupId": 5, "root": {"type": "group", "id": 5, "selectedTabId": 6, "tabs": [{"id": 6}]}},
                  {"id": 7, "name": "grandchild", "parentId": 4, "focusedGroupId": 8, "root": {"type": "group", "id": 8, "selectedTabId": 9, "tabs": [{"id": 9}]}},
                  {"id": 10, "name": "orphan", "parentId": 99, "focusedGroupId": 11, "root": {"type": "group", "id": 11, "selectedTabId": 12, "tabs": [{"id": 12}]}}],
                 "selectedPanelId": 1, "nextId": 13}
                """.trimIndent(),
            )
        }

        val workspace = repository(path).observeWorkspace().first()

        assertEquals(listOf(null, 1L, null, null), workspace.panels.map { it.parentId })
        // 이전 버전이 저장한 워크트리 패널에는 브랜치 키가 없다.
        assertEquals(listOf(null, null, null, null), workspace.panels.map { it.branch })
        assertEquals(listOf(null, null, null, null), workspace.panels.map { it.baseBranch })
    }

    @Test
    fun changeCarriesTheValueBeforeTheUpdate() = runTest {
        val repository = repository(newPath())

        val change = repository.updateWorkspace { it.addPanel() }

        assertEquals(TerminalWorkspace.initial(), change.before)
        assertEquals(2, change.after.panels.size)
    }

    @Test
    fun corruptFileFallsBackToTheInitialWorkspace() = runTest {
        val path = newPath()
        FileSystem.SYSTEM.createDirectories(path.parent!!)
        FileSystem.SYSTEM.write(path) { writeUtf8("{ not json") }

        assertEquals(TerminalWorkspace.initial(), repository(path).observeWorkspace().first())
    }

    @Test
    fun unknownValuesDoNotDiscardTheFile() = runTest {
        val path = newPath()
        FileSystem.SYSTEM.createDirectories(path.parent!!)
        FileSystem.SYSTEM.write(path) {
            writeUtf8(
                """
                {"panels": [{"id": 1, "name": "보존", "future": true, "focusedGroupId": 2,
                  "root": {"type": "group", "id": 2, "selectedTabId": 3, "tabs": [{"id": 3, "program": "future"}]}}],
                 "selectedPanelId": 1, "nextId": 4}
                """.trimIndent(),
            )
        }

        val workspace = repository(path).observeWorkspace().first()

        assertEquals("보존", workspace.panels.single().name)
        assertEquals(null, workspace.panels.single().directory)
        assertEquals(TerminalProgram.Shell, workspace.focusedTab!!.program)
    }

    // 그룹 이전 형식(패널마다 tabs, 탭마다 분할 트리). 변환하지 않고 처음 켠 것으로 읽는다.
    @Test
    fun preGroupFileIsReadAsTheInitialWorkspace() = runTest {
        val path = newPath()
        FileSystem.SYSTEM.createDirectories(path.parent!!)
        FileSystem.SYSTEM.write(path) {
            writeUtf8(
                """
                {"panels": [{"id": 1, "name": "옛 패널", "selectedTabId": 2,
                  "tabs": [{"id": 2, "focusedPaneId": 3, "root": {"type": "leaf", "paneId": 3}}]}],
                 "selectedPanelId": 1, "nextId": 4}
                """.trimIndent(),
            )
        }

        assertEquals(TerminalWorkspace.initial(), repository(path).observeWorkspace().first())
    }

    @Test
    fun emptyGroupsAreDroppedAndOneSidedSplitsCollapse() = runTest {
        val path = newPath()
        FileSystem.SYSTEM.createDirectories(path.parent!!)
        FileSystem.SYSTEM.write(path) {
            writeUtf8(
                """
                {"panels": [{"id": 1, "name": "패널 1", "focusedGroupId": 9,
                  "root": {"type": "split", "id": 5, "direction": "stacked",
                           "first": {"type": "group", "id": 6, "tabs": []},
                           "second": {"type": "group", "id": 7, "tabs": [{"id": 8}]}}}],
                 "selectedPanelId": 1, "nextId": 10}
                """.trimIndent(),
            )
        }

        val workspace = repository(path).observeWorkspace().first()

        assertEquals(PaneNode.Group(7, listOf(TerminalTab(8)), 8), workspace.selectedPanel!!.root)
        assertEquals(7, workspace.focusedGroup!!.id)
    }

    // 쓰기에 쓴 DataStore 는 테스트가 끝날 때까지 살아 있다. 같은 파일을 여는 두 번째 DataStore 가 되지 않도록
    // 파일을 복사해 연다.
    private fun terminalWorkspaceStoreForRead(path: Path) =
        (path.parent!! / "copy.json").let { copy ->
            FileSystem.SYSTEM.copy(path, copy)
            terminalWorkspaceStore(FileSystem.SYSTEM, copy)
        }
}
