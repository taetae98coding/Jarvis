package io.github.taetae98coding.jarvis.domain.terminal

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CloseWorktreePanelUseCaseTest {
    private val initial = TerminalWorkspace.initial()
        .addPanel(name = "Jarvis", directory = "/work/jarvis")
        .let { it.addWorktreePanel(it.panels.last().id, "fix", "/work/jarvis-worktrees/fix", branch = "fix") }
        .addTab()
    private val parent = initial.panels[1]
    private val child = initial.panels.last()
    private val workspace = InMemoryTerminalWorkspaceRepository(initial)
    private val terminal = RecordingTerminalRepository()

    private fun useCase(git: GitWorktreeRepository) =
        CloseWorktreePanelUseCase(workspace, git, UpdateTerminalWorkspaceUseCase(workspace, terminal))

    @Test
    fun removesTheWorktreeOfThePanelFolderThenClosesThePanel() = runTest {
        val git = RecordingGitWorktreeRepository()

        val change = useCase(git).invoke(child.id, removeWorktree = true, deleteDirectory = false).getOrThrow()

        val removed = git.removed.single()
        assertEquals("/work/jarvis-worktrees/fix", removed.directory)
        assertEquals(false, removed.deleteDirectory)
        assertNull(change.after.panels.firstOrNull { it.id == child.id })
        assertEquals(parent.id, change.after.selectedPanelId)
        assertEquals(child.tabs, change.removedTabs)
        assertEquals(change.after, workspace.workspace.value)
    }

    @Test
    fun withoutRemovingOnlyThePanelIsClosed() = runTest {
        val git = RecordingGitWorktreeRepository()

        val change = useCase(git).invoke(child.id, removeWorktree = false, deleteDirectory = true).getOrThrow()

        assertEquals(emptyList(), git.removed)
        assertNull(change.after.panels.firstOrNull { it.id == child.id })
    }

    @Test
    fun gitFailureLeavesTheWorkspaceAlone() = runTest {
        val message = "fatal: '/work/jarvis-worktrees/fix' contains modified or untracked files, use --force to delete it"

        val result = useCase(RecordingGitWorktreeRepository(failure = message)).invoke(child.id, removeWorktree = true, deleteDirectory = true)

        assertEquals(message, assertIs<GitWorktreeException>(result.exceptionOrNull()).message)
        assertEquals(initial, workspace.workspace.value)
    }

    @Test
    fun aPanelThatIsNotAWorktreePanelIsRejectedWithoutCallingGit() = runTest {
        val git = RecordingGitWorktreeRepository()

        assertTrue(useCase(git).invoke(parent.id, removeWorktree = true, deleteDirectory = true).isFailure)
        assertTrue(useCase(git).invoke(999, removeWorktree = false, deleteDirectory = false).isFailure)
        assertEquals(emptyList(), git.removed)
        assertEquals(initial, workspace.workspace.value)
    }
}
