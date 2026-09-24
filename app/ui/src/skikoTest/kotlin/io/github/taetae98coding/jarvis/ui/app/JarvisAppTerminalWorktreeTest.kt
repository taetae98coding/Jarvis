package io.github.taetae98coding.jarvis.ui.app

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.v2.runComposeUiTest
import io.github.taetae98coding.jarvis.domain.terminal.GitWorktree
import io.github.taetae98coding.jarvis.domain.terminal.TerminalProgram
import io.github.taetae98coding.jarvis.domain.terminal.TerminalWorkspace
import io.github.taetae98coding.jarvis.ui.terminal.TerminalCloseWorktreeCancelTestTag
import io.github.taetae98coding.jarvis.ui.terminal.TerminalCloseWorktreeConfirmTestTag
import io.github.taetae98coding.jarvis.ui.terminal.TerminalCloseWorktreeDeleteDirectoryTestTag
import io.github.taetae98coding.jarvis.ui.terminal.TerminalCloseWorktreeDialogTestTag
import io.github.taetae98coding.jarvis.ui.terminal.TerminalCloseWorktreeErrorTestTag
import io.github.taetae98coding.jarvis.ui.terminal.TerminalCloseWorktreeRemoveTestTag
import io.github.taetae98coding.jarvis.ui.terminal.TerminalEmptyPanelTestTag
import io.github.taetae98coding.jarvis.ui.terminal.TerminalNewShellTabTestTag
import io.github.taetae98coding.jarvis.ui.terminal.TerminalNewWorktreeBaseTestTag
import io.github.taetae98coding.jarvis.ui.terminal.TerminalNewWorktreeBranchTestTag
import io.github.taetae98coding.jarvis.ui.terminal.TerminalNewWorktreeCancelTestTag
import io.github.taetae98coding.jarvis.ui.terminal.TerminalNewWorktreeConfirmTestTag
import io.github.taetae98coding.jarvis.ui.terminal.TerminalNewWorktreeDialogTestTag
import io.github.taetae98coding.jarvis.ui.terminal.TerminalNewWorktreeDirectoryTestTag
import io.github.taetae98coding.jarvis.ui.terminal.TerminalNewWorktreeErrorTestTag
import io.github.taetae98coding.jarvis.ui.terminal.TerminalTestTag
import io.github.taetae98coding.jarvis.ui.terminal.terminalNewWorktreeTestTag
import io.github.taetae98coding.jarvis.ui.terminal.terminalPanelBranchTestTag
import io.github.taetae98coding.jarvis.ui.terminal.terminalPanelCloseTestTag
import io.github.taetae98coding.jarvis.ui.terminal.terminalWorktreePanelTestTag
import kotlinx.coroutines.flow.update
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class JarvisAppTerminalWorktreeTest {
    private fun tagPrefix(prefix: String) = SemanticsMatcher("testTag starts with $prefix") {
        it.config.getOrNull(SemanticsProperties.TestTag)?.startsWith(prefix) == true
    }

    private val panel = tagPrefix("terminal:panel:")
    private val tab = tagPrefix("terminal:tab:")

    private fun ComposeUiTest.panelCount() = onAllNodes(panel).fetchSemanticsNodes().size

    private fun ComposeUiTest.tabCount() = onAllNodes(tab).fetchSemanticsNodes().size

    private fun ComposeUiTest.count(tag: String) = onAllNodesWithTag(tag).fetchSemanticsNodes().size

    private fun ComposeUiTest.fieldText(tag: String): String =
        onNodeWithTag(tag).fetchSemanticsNode().config[SemanticsProperties.EditableText].text

    private fun ComposeUiTest.openTerminal() {
        onNodeWithTag(TerminalTestTag).performClick()
    }

    private fun ComposeUiTest.awaitSessions(terminal: FakeTerminalRepository, count: Int) {
        waitUntil(timeoutMillis = FrameTimeoutMillis) { terminal.sessions.size == count }
    }

    private fun ComposeUiTest.awaitTag(tag: String) {
        waitUntil(timeoutMillis = FrameTimeoutMillis) { count(tag) == 1 }
    }

    /** 저장소 폴더의 패널에서 + 를 눌러 [branch] 의 워크트리를 만든다. 기준 브랜치([baseBranch] 가 null 이면)와 폴더는 기본값이다. */
    private fun ComposeUiTest.addWorktree(parentId: Long, branch: String, baseBranch: String? = null) {
        awaitTag(terminalNewWorktreeTestTag(parentId))
        onNodeWithTag(terminalNewWorktreeTestTag(parentId)).performClick()
        onNodeWithTag(TerminalNewWorktreeBranchTestTag).performTextReplacement(branch)
        baseBranch?.let { onNodeWithTag(TerminalNewWorktreeBaseTestTag).performTextReplacement(it) }
        onNodeWithTag(TerminalNewWorktreeConfirmTestTag).performClick()
    }

    /** [addWorktree] 가 끝나 창이 닫히고 빈 워크트리 패널이 선택될 때까지. */
    private fun ComposeUiTest.awaitWorktreePanel(panels: Int) {
        waitUntil(timeoutMillis = FrameTimeoutMillis) { count(TerminalNewWorktreeDialogTestTag) == 0 && panelCount() == panels }
        awaitTag(TerminalEmptyPanelTestTag)
    }

    private fun ComposeUiTest.openShellTab() {
        onNode(newTabButton).performClick()
        onNodeWithTag(TerminalNewShellTabTestTag).performClick()
    }

    // "패널 1"(폴더 없음), "Jarvis"(저장소, 셸 탭 하나), "API"(저장소가 아닌 폴더). Jarvis 가 선택돼 있다.
    private val initial = TerminalWorkspace.initial()
        .addPanel(name = "API", directory = "/work/api")
        .addPanel(name = "Jarvis", directory = "/work/jarvis")
        .addTab()

    private val jarvis = initial.panels.last()
    private val api = initial.panels[1]
    private val plain = initial.panels.first()

    private fun git() = FakeGitWorktreeRepository(mapOf("/work/jarvis" to GitWorktree("/work/jarvis", "/work/jarvis", "main")))

    @Test
    fun onlyPanelsInsideAGitRepositoryHaveAWorktreeButton() = runComposeUiTest {
        val terminal = FakeTerminalRepository()
        setContent { TestJarvisApp(terminal = terminal, terminalWorkspace = FakeTerminalWorkspaceRepository(initial), gitWorktree = git()) }
        openTerminal()
        awaitSessions(terminal, 1)

        awaitTag(terminalNewWorktreeTestTag(jarvis.id))

        assertEquals(0, count(terminalNewWorktreeTestTag(api.id)))
        assertEquals(0, count(terminalNewWorktreeTestTag(plain.id)))
        assertEquals(0, count(terminalWorktreePanelTestTag(jarvis.id)))
    }

    @Test
    fun theButtonFollowsTheFolderBecomingARepository() = runComposeUiTest {
        val terminal = FakeTerminalRepository()
        val git = git()
        setContent { TestJarvisApp(terminal = terminal, terminalWorkspace = FakeTerminalWorkspaceRepository(initial), gitWorktree = git) }
        openTerminal()
        awaitSessions(terminal, 1)
        awaitTag(terminalNewWorktreeTestTag(jarvis.id))

        git.worktrees.value = git.worktrees.value + ("/work/api" to GitWorktree("/work/api", "/work/api"))

        awaitTag(terminalNewWorktreeTestTag(api.id))
    }

    @Test
    fun folderFollowsTheBranchUntilEditedAndAnEmptyBranchCannotCreate() = runComposeUiTest {
        val terminal = FakeTerminalRepository()
        setContent { TestJarvisApp(terminal = terminal, terminalWorkspace = FakeTerminalWorkspaceRepository(initial), gitWorktree = git()) }
        openTerminal()
        awaitSessions(terminal, 1)
        awaitTag(terminalNewWorktreeTestTag(jarvis.id))

        onNodeWithTag(terminalNewWorktreeTestTag(jarvis.id)).performClick()

        assertEquals(1, count(TerminalNewWorktreeDialogTestTag))
        onNodeWithTag(TerminalNewWorktreeConfirmTestTag).assertIsNotEnabled()
        assertEquals(0, onAllNodesWithText("Claude (YOLO)").fetchSemanticsNodes().size)
        // 기준 브랜치는 + 를 누른 패널의 현재 브랜치로 시작한다.
        assertEquals("main", fieldText(TerminalNewWorktreeBaseTestTag))

        onNodeWithTag(TerminalNewWorktreeBranchTestTag).performTextReplacement("feature/login")
        assertEquals("/work/jarvis-worktrees/feature/login", fieldText(TerminalNewWorktreeDirectoryTestTag))
        onNodeWithTag(TerminalNewWorktreeConfirmTestTag).assertIsEnabled()

        onNodeWithTag(TerminalNewWorktreeDirectoryTestTag).performTextReplacement("/tmp/login")
        onNodeWithTag(TerminalNewWorktreeBranchTestTag).performTextReplacement("fix")
        assertEquals("/tmp/login", fieldText(TerminalNewWorktreeDirectoryTestTag))

        onNodeWithTag(TerminalNewWorktreeCancelTestTag).performClick()
        assertEquals(0, count(TerminalNewWorktreeDialogTestTag))
        assertEquals(3, panelCount())
    }

    @Test
    fun creatingAWorktreeAddsAnEmptyIndentedPanelUnderTheParentWhoseFirstShellOpensInIt() = runComposeUiTest {
        val terminal = FakeTerminalRepository()
        val workspace = FakeTerminalWorkspaceRepository(initial)
        val git = git()
        setContent { TestJarvisApp(terminal = terminal, terminalWorkspace = workspace, gitWorktree = git) }
        openTerminal()
        awaitSessions(terminal, 1)

        addWorktree(jarvis.id, " feature/login ")

        awaitWorktreePanel(panels = 4)
        val added = git.added.single()
        assertEquals("/work/jarvis", added.repositoryDirectory)
        assertEquals("feature/login", added.branch)
        assertEquals("main", added.baseBranch)
        assertEquals("/work/jarvis-worktrees/feature/login", added.path)

        val current = workspace.workspace.value
        val child = current.selectedPanel!!
        assertEquals(jarvis.id, child.parentId)
        assertEquals("feature/login", child.name)
        assertEquals("feature/login", child.branch)
        assertEquals("main", child.baseBranch)
        assertEquals("/work/jarvis-worktrees/feature/login", child.directory)
        assertEquals(emptyList(), child.tabs)
        assertEquals(listOf(plain.id, api.id, jarvis.id, child.id), current.panels.map { it.id })
        assertEquals(1, terminal.sessions.size)

        onNodeWithTag(terminalWorktreePanelTestTag(child.id)).assertIsDisplayed()
        onNodeWithText("feature/login").assertIsDisplayed()
        onNodeWithTag(terminalPanelBranchTestTag(child.id), useUnmergedTree = true).assertTextEquals("main → feature/login")
        assertEquals(0, onAllNodesWithTag(terminalPanelBranchTestTag(jarvis.id), useUnmergedTree = true).fetchSemanticsNodes().size)
        assertEquals(0, tabCount())

        openShellTab()

        awaitSessions(terminal, 2)
        assertEquals(1, tabCount())
        assertEquals(TerminalProgram.Shell, terminal.sessions[1].program)
        assertEquals("/work/jarvis-worktrees/feature/login", terminal.sessions[1].tab.directory)
    }

    @Test
    fun theBaseBranchCanBeChangedAndTheBranchLineFollowsTheObservedHead() = runComposeUiTest {
        val terminal = FakeTerminalRepository()
        val workspace = FakeTerminalWorkspaceRepository(initial)
        val git = git()
        setContent { TestJarvisApp(terminal = terminal, terminalWorkspace = workspace, gitWorktree = git) }
        openTerminal()
        awaitSessions(terminal, 1)

        addWorktree(jarvis.id, "hotfix", baseBranch = " release ")

        awaitWorktreePanel(panels = 4)
        assertEquals("release", git.added.single().baseBranch)
        val child = workspace.workspace.value.selectedPanel!!
        assertEquals("release", child.baseBranch)
        onNodeWithTag(terminalPanelBranchTestTag(child.id), useUnmergedTree = true).assertTextEquals("release → hotfix")

        // 셸에서 브랜치를 바꾼 것처럼 관측 값이 바뀌면 줄이 따라간다.
        git.worktrees.update { it + (child.directory!! to it.getValue(child.directory!!).copy(branch = "hotfix-2")) }

        waitUntil(timeoutMillis = FrameTimeoutMillis) {
            onNodeWithTag(terminalPanelBranchTestTag(child.id), useUnmergedTree = true).fetchSemanticsNode().config[SemanticsProperties.Text].joinToString() == "release → hotfix-2"
        }
    }

    @Test
    fun gitFailureKeepsTheDialogOpenWithTheMessage() = runComposeUiTest {
        val terminal = FakeTerminalRepository()
        val workspace = FakeTerminalWorkspaceRepository(initial)
        val git = git().apply { failure = "fatal: 'fix' is already checked out at '/elsewhere'" }
        setContent { TestJarvisApp(terminal = terminal, terminalWorkspace = workspace, gitWorktree = git) }
        openTerminal()
        awaitSessions(terminal, 1)

        addWorktree(jarvis.id, "fix")

        awaitTag(TerminalNewWorktreeErrorTestTag)
        onNodeWithText("fatal: 'fix' is already checked out at '/elsewhere'").assertIsDisplayed()
        assertEquals(1, count(TerminalNewWorktreeDialogTestTag))
        assertEquals(initial, workspace.workspace.value)
        onNodeWithTag(TerminalNewWorktreeConfirmTestTag).assertIsEnabled()

        // 고쳐서 다시 누르면 된다.
        git.failure = null
        onNodeWithTag(TerminalNewWorktreeConfirmTestTag).performClick()

        awaitWorktreePanel(panels = 4)
        assertEquals(jarvis.id, workspace.workspace.value.selectedPanel!!.parentId)
    }

    @Test
    fun theButtonOnAWorktreePanelMakesASiblingUnderTheSameParent() = runComposeUiTest {
        val terminal = FakeTerminalRepository()
        val workspace = FakeTerminalWorkspaceRepository(initial)
        setContent { TestJarvisApp(terminal = terminal, terminalWorkspace = workspace, gitWorktree = git()) }
        openTerminal()
        awaitSessions(terminal, 1)
        addWorktree(jarvis.id, "a")
        awaitWorktreePanel(panels = 4)
        val first = workspace.workspace.value.selectedPanel!!

        // 워크트리 패널에서 누르면 기준 브랜치의 기본값은 그 워크트리의 브랜치다.
        awaitTag(terminalNewWorktreeTestTag(first.id))
        onNodeWithTag(terminalNewWorktreeTestTag(first.id)).performClick()
        assertEquals("a", fieldText(TerminalNewWorktreeBaseTestTag))
        onNodeWithTag(TerminalNewWorktreeBranchTestTag).performTextReplacement("b")
        onNodeWithTag(TerminalNewWorktreeConfirmTestTag).performClick()

        awaitWorktreePanel(panels = 5)
        val current = workspace.workspace.value
        assertEquals(listOf("a", "b"), current.children(jarvis.id).map { it.name })
        assertEquals(jarvis.id, current.selectedPanel!!.parentId)
        assertEquals("a", current.selectedPanel!!.baseBranch)
        assertEquals(emptyList(), current.children(first.id))
        onNodeWithTag(terminalPanelBranchTestTag(current.selectedPanelId!!), useUnmergedTree = true).assertTextEquals("a → b")
    }

    @Test
    fun closingTheParentClosesItsWorktreePanelsAndTheirShells() = runComposeUiTest {
        val terminal = FakeTerminalRepository()
        val workspace = FakeTerminalWorkspaceRepository(initial)
        setContent { TestJarvisApp(terminal = terminal, terminalWorkspace = workspace, gitWorktree = git()) }
        openTerminal()
        awaitSessions(terminal, 1)
        addWorktree(jarvis.id, "a")
        awaitWorktreePanel(panels = 4)
        openShellTab()
        awaitSessions(terminal, 2)
        val child = workspace.workspace.value.selectedPanel!!

        onNodeWithTag(terminalPanelCloseTestTag(jarvis.id)).performClick()

        assertEquals(0, count(TerminalCloseWorktreeDialogTestTag))
        waitUntil(timeoutMillis = FrameTimeoutMillis) { panelCount() == 2 }
        val current = workspace.workspace.value
        assertEquals(listOf(plain.id, api.id), current.panels.map { it.id })
        assertNull(current.panels.firstOrNull { it.id == child.id })
        assertTrue(terminal.sessions[0].closed)
        assertTrue(terminal.sessions[1].closed)
    }

    /** 워크트리 패널 "a" 를 만들고 셸 탭을 하나 연 뒤, 그 패널의 ✕ 로 "워크트리 닫기" 창을 띄운다. */
    private fun ComposeUiTest.openCloseWorktreeDialog(workspace: FakeTerminalWorkspaceRepository, terminal: FakeTerminalRepository): Long {
        openTerminal()
        awaitSessions(terminal, 1)
        addWorktree(jarvis.id, "a")
        awaitWorktreePanel(panels = 4)
        openShellTab()
        awaitSessions(terminal, 2)
        val child = workspace.workspace.value.selectedPanel!!

        onNodeWithTag(terminalPanelCloseTestTag(child.id)).performClick()
        awaitTag(TerminalCloseWorktreeDialogTestTag)
        return child.id
    }

    private fun ComposeUiTest.awaitClosed(panels: Int) {
        waitUntil(timeoutMillis = FrameTimeoutMillis) { count(TerminalCloseWorktreeDialogTestTag) == 0 && panelCount() == panels }
    }

    @Test
    fun closingAWorktreePanelAsksAndRemovesTheWorktreeBranchAndFolderByDefault() = runComposeUiTest {
        val terminal = FakeTerminalRepository()
        val workspace = FakeTerminalWorkspaceRepository(initial)
        val git = git()
        setContent { TestJarvisApp(terminal = terminal, terminalWorkspace = workspace, gitWorktree = git) }

        val child = openCloseWorktreeDialog(workspace, terminal)

        onNodeWithTag(TerminalCloseWorktreeRemoveTestTag).assertIsOn()
        onNodeWithTag(TerminalCloseWorktreeDeleteDirectoryTestTag).assertIsOn()
        assertEquals(4, panelCount())
        assertEquals(emptyList(), git.removed)

        onNodeWithTag(TerminalCloseWorktreeConfirmTestTag).performClick()

        awaitClosed(panels = 3)
        val removed = git.removed.single()
        assertEquals("/work/jarvis-worktrees/a", removed.directory)
        assertTrue(removed.deleteDirectory)
        assertNull(workspace.workspace.value.panels.firstOrNull { it.id == child })
        assertEquals(jarvis.id, workspace.workspace.value.selectedPanelId)
        assertTrue(terminal.sessions[1].closed)
    }

    @Test
    fun theFolderChoiceFollowsTheWorktreeChoiceAndCanKeepTheFolder() = runComposeUiTest {
        val terminal = FakeTerminalRepository()
        val workspace = FakeTerminalWorkspaceRepository(initial)
        val git = git()
        setContent { TestJarvisApp(terminal = terminal, terminalWorkspace = workspace, gitWorktree = git) }
        openCloseWorktreeDialog(workspace, terminal)

        onNodeWithTag(TerminalCloseWorktreeDeleteDirectoryTestTag).performClick()
        onNodeWithTag(TerminalCloseWorktreeDeleteDirectoryTestTag).assertIsOff()

        onNodeWithTag(TerminalCloseWorktreeRemoveTestTag).performClick()
        onNodeWithTag(TerminalCloseWorktreeRemoveTestTag).assertIsOff()
        onNodeWithTag(TerminalCloseWorktreeDeleteDirectoryTestTag).assertIsOff().assertIsNotEnabled()

        // 다시 켜면 폴더 선택은 끄기 전 값(끔)으로 돌아온다.
        onNodeWithTag(TerminalCloseWorktreeRemoveTestTag).performClick()
        onNodeWithTag(TerminalCloseWorktreeDeleteDirectoryTestTag).assertIsOff().assertIsEnabled()

        onNodeWithTag(TerminalCloseWorktreeConfirmTestTag).performClick()

        awaitClosed(panels = 3)
        val removed = git.removed.single()
        assertEquals("/work/jarvis-worktrees/a", removed.directory)
        assertEquals(false, removed.deleteDirectory)
    }

    @Test
    fun withBothChoicesOffOnlyThePanelIsClosed() = runComposeUiTest {
        val terminal = FakeTerminalRepository()
        val workspace = FakeTerminalWorkspaceRepository(initial)
        val git = git()
        setContent { TestJarvisApp(terminal = terminal, terminalWorkspace = workspace, gitWorktree = git) }
        openCloseWorktreeDialog(workspace, terminal)

        onNodeWithTag(TerminalCloseWorktreeRemoveTestTag).performClick()
        onNodeWithTag(TerminalCloseWorktreeConfirmTestTag).performClick()

        awaitClosed(panels = 3)
        assertEquals(emptyList(), git.removed)
        assertTrue(terminal.sessions[1].closed)
    }

    @Test
    fun gitFailureKeepsTheCloseDialogAndThePanel() = runComposeUiTest {
        val terminal = FakeTerminalRepository()
        val workspace = FakeTerminalWorkspaceRepository(initial)
        val git = git()
        setContent { TestJarvisApp(terminal = terminal, terminalWorkspace = workspace, gitWorktree = git) }
        val child = openCloseWorktreeDialog(workspace, terminal)
        git.failure = "fatal: '/work/jarvis-worktrees/a' contains modified or untracked files, use --force to delete it"

        onNodeWithTag(TerminalCloseWorktreeConfirmTestTag).performClick()

        awaitTag(TerminalCloseWorktreeErrorTestTag)
        onNodeWithText("fatal: '/work/jarvis-worktrees/a' contains modified or untracked files, use --force to delete it").assertIsDisplayed()
        assertEquals(1, count(TerminalCloseWorktreeDialogTestTag))
        assertEquals(4, panelCount())
        assertTrue(!terminal.sessions[1].closed)
        onNodeWithTag(TerminalCloseWorktreeConfirmTestTag).assertIsEnabled()

        // 폴더를 남기면 된다.
        git.failure = null
        onNodeWithTag(TerminalCloseWorktreeDeleteDirectoryTestTag).performClick()
        onNodeWithTag(TerminalCloseWorktreeConfirmTestTag).performClick()

        awaitClosed(panels = 3)
        assertEquals(false, git.removed.last().deleteDirectory)
        assertNull(workspace.workspace.value.panels.firstOrNull { it.id == child })
    }

    @Test
    fun cancelClosesNothing() = runComposeUiTest {
        val terminal = FakeTerminalRepository()
        val workspace = FakeTerminalWorkspaceRepository(initial)
        val git = git()
        setContent { TestJarvisApp(terminal = terminal, terminalWorkspace = workspace, gitWorktree = git) }
        openCloseWorktreeDialog(workspace, terminal)

        onNodeWithTag(TerminalCloseWorktreeCancelTestTag).performClick()

        assertEquals(0, count(TerminalCloseWorktreeDialogTestTag))
        assertEquals(4, panelCount())
        assertEquals(emptyList(), git.removed)
        assertTrue(!terminal.sessions[1].closed)
    }

    @Test
    fun aWorktreePanelWhoseWorktreeCannotBeObservedClosesWithoutAsking() = runComposeUiTest {
        val terminal = FakeTerminalRepository()
        val workspace = FakeTerminalWorkspaceRepository(initial)
        val git = git()
        setContent { TestJarvisApp(terminal = terminal, terminalWorkspace = workspace, gitWorktree = git) }
        openTerminal()
        awaitSessions(terminal, 1)
        addWorktree(jarvis.id, "a")
        awaitWorktreePanel(panels = 4)
        val child = workspace.workspace.value.selectedPanel!!
        awaitTag(terminalNewWorktreeTestTag(child.id))

        // 셸에서 폴더를 지운 것처럼 더는 저장소가 아니다.
        git.worktrees.update { it - child.directory!! }
        waitUntil(timeoutMillis = FrameTimeoutMillis) { count(terminalNewWorktreeTestTag(child.id)) == 0 }

        onNodeWithTag(terminalPanelCloseTestTag(child.id)).performClick()

        waitUntil(timeoutMillis = FrameTimeoutMillis) { panelCount() == 3 }
        assertEquals(0, count(TerminalCloseWorktreeDialogTestTag))
        assertEquals(emptyList(), git.removed)
        assertEquals(jarvis.id, workspace.workspace.value.selectedPanelId)
    }

    private companion object {
        const val FrameTimeoutMillis = 10_000L
    }
}
