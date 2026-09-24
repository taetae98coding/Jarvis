package io.github.taetae98coding.jarvis.domain.terminal

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AddWorktreePanelUseCaseTest {
    private val initial = TerminalWorkspace.initial().addPanel(name = "Jarvis", directory = "/work/jarvis")
    private val parent = initial.panels.last()
    private val workspace = InMemoryTerminalWorkspaceRepository(initial)
    private val terminal = RecordingTerminalRepository()

    private fun useCase(git: GitWorktreeRepository) =
        AddWorktreePanelUseCase(workspace, git, UpdateTerminalWorkspaceUseCase(workspace, terminal))

    @Test
    fun addsTheWorktreeToTheParentFolderRepositoryAndAddsAnEmptyPanelForIt() = runTest {
        val git = RecordingGitWorktreeRepository()

        val result = useCase(git).invoke(parent.id, " feature/login ", " main ", "/work/jarvis-worktrees/feature/login")

        val added = git.added.single()
        assertEquals("/work/jarvis", added.repositoryDirectory)
        assertEquals("feature/login", added.branch)
        assertEquals("main", added.baseBranch)
        assertEquals("/work/jarvis-worktrees/feature/login", added.path)

        val after = result.getOrThrow().after
        val child = after.selectedPanel!!
        assertEquals("feature/login", child.name)
        assertEquals("feature/login", child.branch)
        assertEquals("main", child.baseBranch)
        assertEquals("/work/jarvis-worktrees/feature/login", child.directory)
        assertEquals(parent.id, child.parentId)
        assertNull(child.root)
        assertEquals("/work/jarvis-worktrees/feature/login", after.startDirectory())
        assertEquals(after, workspace.workspace.value)
    }

    @Test
    fun aBlankBaseBranchIsPassedAsNullAndNotRemembered() = runTest {
        val git = RecordingGitWorktreeRepository()

        val result = useCase(git).invoke(parent.id, "fix", "   ", "/tmp/fix")

        assertNull(git.added.single().baseBranch)
        val child = result.getOrThrow().after.selectedPanel!!
        assertEquals("fix", child.branch)
        assertNull(child.baseBranch)
    }

    @Test
    fun gitFailureLeavesTheWorkspaceAlone() = runTest {
        val result = useCase(RecordingGitWorktreeRepository(failure = "fatal: 'fix' is already checked out")).invoke(parent.id, "fix", "main", "/tmp/fix")

        val error = assertIs<GitWorktreeException>(result.exceptionOrNull())
        assertEquals("fatal: 'fix' is already checked out", error.message)
        assertEquals(initial, workspace.workspace.value)
    }

    @Test
    fun aPanelWithoutAFolderOrABlankBranchIsRejectedWithoutCallingGit() = runTest {
        val git = RecordingGitWorktreeRepository()
        val noFolder = initial.panels.first()

        assertTrue(useCase(git).invoke(noFolder.id, "fix", "main", "/tmp/fix").isFailure)
        assertTrue(useCase(git).invoke(parent.id, "   ", "main", "/tmp/fix").isFailure)
        assertTrue(useCase(git).invoke(parent.id, "fix", "main", "  ").isFailure)
        assertTrue(useCase(git).invoke(999, "fix", "main", "/tmp/fix").isFailure)
        assertEquals(emptyList(), git.added)
        assertEquals(initial, workspace.workspace.value)
    }
}
