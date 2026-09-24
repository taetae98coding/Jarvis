package io.github.taetae98coding.jarvis.domain.terminal

import kotlin.test.Test
import kotlin.test.assertEquals

class GitWorktreeTest {
    @Test
    fun defaultPathIsNextToTheMainWorktree() {
        val worktree = GitWorktree(path = "/work/jarvis-worktrees/old", mainPath = "/work/jarvis")

        assertEquals("/work/jarvis-worktrees/feature/login", worktree.defaultWorktreePath("feature/login"))
    }

    @Test
    fun defaultPathTrimsTheBranchAndATrailingSlash() {
        val worktree = GitWorktree(path = "/work/jarvis/", mainPath = "/work/jarvis/")

        assertEquals("/work/jarvis-worktrees/fix", worktree.defaultWorktreePath("  fix "))
    }
}
