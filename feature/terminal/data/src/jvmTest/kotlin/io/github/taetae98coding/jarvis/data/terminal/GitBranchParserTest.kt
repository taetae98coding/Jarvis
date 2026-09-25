package io.github.taetae98coding.jarvis.data.terminal

import io.github.taetae98coding.jarvis.domain.terminal.GitBranch
import kotlin.test.Test
import kotlin.test.assertEquals

class GitBranchParserTest {
    private fun refs(vararg lines: Pair<String, String>): String = lines.joinToString("\n") { (ref, symref) -> "$ref\u0000$symref" } + "\n"

    @Test
    fun localBranchesComeFirstAndEachGroupKeepsGitsOrder() {
        val output = refs(
            "refs/remotes/origin/feature" to "",
            "refs/heads/work" to "",
            "refs/remotes/origin/main" to "",
            "refs/heads/main" to "",
        )

        assertEquals(
            listOf(GitBranch("work"), GitBranch("main"), GitBranch("origin/feature", "origin"), GitBranch("origin/main", "origin")),
            parseGitBranches(output, remotes = listOf("origin")),
        )
    }

    @Test
    fun symbolicRefsLikeOriginHeadAreLeftOut() {
        val output = refs("refs/remotes/origin/HEAD" to "refs/remotes/origin/main", "refs/remotes/origin/main" to "")

        assertEquals(listOf(GitBranch("origin/main", "origin")), parseGitBranches(output, remotes = listOf("origin")))
    }

    @Test
    fun theLongestMatchingRemoteOwnsARemoteBranch() {
        val output = refs("refs/remotes/team/alice/fix" to "", "refs/remotes/team/main" to "")

        assertEquals(
            listOf(GitBranch("team/alice/fix", "team/alice"), GitBranch("team/main", "team")),
            parseGitBranches(output, remotes = listOf("team", "team/alice")),
        )
    }

    @Test
    fun anUnknownRemoteIsTheTextBeforeTheFirstSlash() {
        val output = refs("refs/remotes/gone/feature/x" to "")

        assertEquals(listOf(GitBranch("gone/feature/x", "gone")), parseGitBranches(output, remotes = emptyList()))
    }

    @Test
    fun slashesInLocalBranchNamesAreKeptAndOtherRefsIgnored() {
        val output = refs("refs/heads/feature/login" to "", "refs/tags/v1" to "")

        assertEquals(listOf(GitBranch("feature/login")), parseGitBranches(output, remotes = emptyList()))
        assertEquals(emptyList(), parseGitBranches("", remotes = emptyList()))
    }
}
