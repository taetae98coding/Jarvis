package io.github.taetae98coding.jarvis.data.terminal

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
                .split(SplitDirection.Stacked, directory = "/work")
                .addPanel()
        }

        val reopened = DefaultTerminalWorkspaceRepository(terminalWorkspaceStoreForRead(path)).observeWorkspace().first()

        assertEquals(change.after, reopened)
        assertEquals(listOf("백엔드", "패널 2"), reopened.panels.map { it.name })
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
