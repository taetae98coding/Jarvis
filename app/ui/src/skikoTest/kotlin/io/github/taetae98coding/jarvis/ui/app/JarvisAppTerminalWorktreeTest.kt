package io.github.taetae98coding.jarvis.ui.app

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.v2.runComposeUiTest
import io.github.taetae98coding.jarvis.domain.terminal.GitWorktree
import io.github.taetae98coding.jarvis.domain.terminal.TerminalProgram
import io.github.taetae98coding.jarvis.domain.terminal.TerminalWorkspace
import io.github.taetae98coding.jarvis.ui.terminal.TerminalNewWorktreeBranchTestTag
import io.github.taetae98coding.jarvis.ui.terminal.TerminalNewWorktreeCancelTestTag
import io.github.taetae98coding.jarvis.ui.terminal.TerminalNewWorktreeClaudeTestTag
import io.github.taetae98coding.jarvis.ui.terminal.TerminalNewWorktreeDialogTestTag
import io.github.taetae98coding.jarvis.ui.terminal.TerminalNewWorktreeDirectoryTestTag
import io.github.taetae98coding.jarvis.ui.terminal.TerminalNewWorktreeErrorTestTag
import io.github.taetae98coding.jarvis.ui.terminal.TerminalNewWorktreeShellTestTag
import io.github.taetae98coding.jarvis.ui.terminal.TerminalTestTag
import io.github.taetae98coding.jarvis.ui.terminal.terminalNewWorktreeTestTag
import io.github.taetae98coding.jarvis.ui.terminal.terminalPanelCloseTestTag
import io.github.taetae98coding.jarvis.ui.terminal.terminalWorktreePanelTestTag
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
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

    /** 저장소 폴더의 패널에서 + 를 눌러 [branch] 의 워크트리를 만든다. 폴더는 기본값이다. */
    private fun ComposeUiTest.addWorktree(parentId: Long, branch: String, buttonTag: String = TerminalNewWorktreeShellTestTag) {
        awaitTag(terminalNewWorktreeTestTag(parentId))
        onNodeWithTag(terminalNewWorktreeTestTag(parentId)).performClick()
        onNodeWithTag(TerminalNewWorktreeBranchTestTag).performTextReplacement(branch)
        onNodeWithTag(buttonTag).performClick()
    }

    // "패널 1"(폴더 없음), "Jarvis"(저장소), "API"(저장소가 아닌 폴더). Jarvis 가 선택돼 있다.
    private val initial = TerminalWorkspace.initial()
        .addPanel(name = "API", directory = "/work/api")
        .addPanel(name = "Jarvis", directory = "/work/jarvis")

    private val jarvis = initial.panels.last()
    private val api = initial.panels[1]
    private val plain = initial.panels.first()

    private fun git() = FakeGitWorktreeRepository(mapOf("/work/jarvis" to GitWorktree("/work/jarvis", "/work/jarvis")))

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
        onNodeWithTag(TerminalNewWorktreeShellTestTag).assertIsNotEnabled()
        onNodeWithTag(TerminalNewWorktreeClaudeTestTag).assertIsNotEnabled()

        onNodeWithTag(TerminalNewWorktreeBranchTestTag).performTextReplacement("feature/login")
        assertEquals("/work/jarvis-worktrees/feature/login", fieldText(TerminalNewWorktreeDirectoryTestTag))
        onNodeWithTag(TerminalNewWorktreeShellTestTag).assertIsEnabled()

        onNodeWithTag(TerminalNewWorktreeDirectoryTestTag).performTextReplacement("/tmp/login")
        onNodeWithTag(TerminalNewWorktreeBranchTestTag).performTextReplacement("fix")
        assertEquals("/tmp/login", fieldText(TerminalNewWorktreeDirectoryTestTag))

        onNodeWithTag(TerminalNewWorktreeCancelTestTag).performClick()
        assertEquals(0, count(TerminalNewWorktreeDialogTestTag))
        assertEquals(3, panelCount())
    }

    @Test
    fun creatingAWorktreeAddsAnIndentedPanelUnderTheParentAndShowsItsTabs() = runComposeUiTest {
        val terminal = FakeTerminalRepository()
        val workspace = FakeTerminalWorkspaceRepository(initial)
        val git = git()
        setContent { TestJarvisApp(terminal = terminal, terminalWorkspace = workspace, gitWorktree = git) }
        openTerminal()
        awaitSessions(terminal, 1)

        addWorktree(jarvis.id, " feature/login ")

        awaitSessions(terminal, 2)
        assertEquals(0, count(TerminalNewWorktreeDialogTestTag))
        val added = git.added.single()
        assertEquals("/work/jarvis", added.repositoryDirectory)
        assertEquals("feature/login", added.branch)
        assertEquals("/work/jarvis-worktrees/feature/login", added.path)

        val current = workspace.workspace.value
        val child = current.selectedPanel!!
        assertEquals(jarvis.id, child.parentId)
        assertEquals("feature/login", child.name)
        assertEquals("/work/jarvis-worktrees/feature/login", child.directory)
        assertEquals(listOf(plain.id, api.id, jarvis.id, child.id), current.panels.map { it.id })
        assertEquals(TerminalProgram.Shell, terminal.sessions[1].program)
        assertEquals("/work/jarvis-worktrees/feature/login", terminal.sessions[1].tab.directory)

        assertEquals(4, panelCount())
        onNodeWithTag(terminalWorktreePanelTestTag(child.id)).assertIsDisplayed()
        onNodeWithText("feature/login").assertIsDisplayed()
        assertEquals(1, tabCount())
        assertNotNull(terminal.sessions[1])
    }

    @Test
    fun claudeButtonOpensClaudeInTheWorktree() = runComposeUiTest {
        val terminal = FakeTerminalRepository()
        val workspace = FakeTerminalWorkspaceRepository(initial)
        setContent { TestJarvisApp(terminal = terminal, terminalWorkspace = workspace, gitWorktree = git()) }
        openTerminal()
        awaitSessions(terminal, 1)

        addWorktree(jarvis.id, "fix", buttonTag = TerminalNewWorktreeClaudeTestTag)

        awaitSessions(terminal, 2)
        assertEquals(TerminalProgram.Claude, terminal.sessions[1].program)
        assertNotNull(terminal.sessions[1].tab.claudeSessionId)
        assertEquals("/work/jarvis-worktrees/fix", terminal.sessions[1].tab.directory)
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
        onNodeWithTag(TerminalNewWorktreeShellTestTag).assertIsEnabled()

        // 고쳐서 다시 누르면 된다.
        git.failure = null
        onNodeWithTag(TerminalNewWorktreeShellTestTag).performClick()

        awaitSessions(terminal, 2)
        assertEquals(0, count(TerminalNewWorktreeDialogTestTag))
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
        awaitSessions(terminal, 2)
        val first = workspace.workspace.value.selectedPanel!!

        addWorktree(first.id, "b")

        awaitSessions(terminal, 3)
        val current = workspace.workspace.value
        assertEquals(listOf("a", "b"), current.children(jarvis.id).map { it.name })
        assertEquals(jarvis.id, current.selectedPanel!!.parentId)
        assertEquals(emptyList(), current.children(first.id))
    }

    @Test
    fun closingTheParentClosesItsWorktreePanelsAndTheirShells() = runComposeUiTest {
        val terminal = FakeTerminalRepository()
        val workspace = FakeTerminalWorkspaceRepository(initial)
        setContent { TestJarvisApp(terminal = terminal, terminalWorkspace = workspace, gitWorktree = git()) }
        openTerminal()
        awaitSessions(terminal, 1)
        addWorktree(jarvis.id, "a")
        awaitSessions(terminal, 2)
        val child = workspace.workspace.value.selectedPanel!!

        onNodeWithTag(terminalPanelCloseTestTag(jarvis.id)).performClick()

        waitUntil(timeoutMillis = FrameTimeoutMillis) { panelCount() == 2 }
        val current = workspace.workspace.value
        assertEquals(listOf(plain.id, api.id), current.panels.map { it.id })
        assertNull(current.panels.firstOrNull { it.id == child.id })
        assertTrue(terminal.sessions[0].closed)
        assertTrue(terminal.sessions[1].closed)
    }

    @Test
    fun closingAWorktreePanelKeepsTheParentSelected() = runComposeUiTest {
        val terminal = FakeTerminalRepository()
        val workspace = FakeTerminalWorkspaceRepository(initial)
        setContent { TestJarvisApp(terminal = terminal, terminalWorkspace = workspace, gitWorktree = git()) }
        openTerminal()
        awaitSessions(terminal, 1)
        addWorktree(jarvis.id, "a")
        awaitSessions(terminal, 2)
        val child = workspace.workspace.value.selectedPanel!!

        onNodeWithTag(terminalPanelCloseTestTag(child.id)).performClick()

        waitUntil(timeoutMillis = FrameTimeoutMillis) { panelCount() == 3 }
        assertEquals(jarvis.id, workspace.workspace.value.selectedPanelId)
        assertTrue(terminal.sessions[1].closed)
    }

    private companion object {
        const val FrameTimeoutMillis = 10_000L
    }
}
