package io.github.taetae98coding.jarvis.data.terminal

import io.github.taetae98coding.jarvis.domain.terminal.GitChange
import io.github.taetae98coding.jarvis.domain.terminal.GitChangeKind
import io.github.taetae98coding.jarvis.domain.terminal.GitCommit
import io.github.taetae98coding.jarvis.domain.terminal.GitDiffHunk
import io.github.taetae98coding.jarvis.domain.terminal.GitFileDiff
import io.github.taetae98coding.jarvis.domain.terminal.GitGraphLine
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GitStatusParserTest {
    private fun status(vararg fields: String) = parseGitStatus("/repo", fields.joinToString("\u0000", postfix = "\u0000"))

    @Test
    fun indexAndWorkTreeChangesAreSplitAndSortedByPath() {
        val status = status("## main...origin/main [ahead 1]", "M  z.kt", "MM b.kt", " M a.kt", "A  new.kt", " D gone.kt", "?? tmp/x.txt")

        assertEquals("main", status.branch)
        assertEquals(
            listOf(GitChange("b.kt", GitChangeKind.Modified), GitChange("new.kt", GitChangeKind.Added), GitChange("z.kt", GitChangeKind.Modified)),
            status.staged,
        )
        assertEquals(
            listOf(
                GitChange("a.kt", GitChangeKind.Modified),
                GitChange("b.kt", GitChangeKind.Modified),
                GitChange("gone.kt", GitChangeKind.Deleted),
                GitChange("tmp/x.txt", GitChangeKind.Untracked),
            ),
            status.unstaged,
        )
    }

    @Test
    fun renamesCarryTheOriginalPath() {
        val status = status("## main", "RM b.txt", "a.txt", "?? c.txt")

        assertEquals(listOf(GitChange("b.txt", GitChangeKind.Renamed, originalPath = "a.txt")), status.staged)
        assertEquals(listOf(GitChange("b.txt", GitChangeKind.Modified), GitChange("c.txt", GitChangeKind.Untracked)), status.unstaged)
    }

    @Test
    fun conflictsAreOnlyWorkTreeChanges() {
        val status = status("## main", "UU both.kt", "AA added.kt", "DU gone.kt")

        assertEquals(emptyList(), status.staged)
        assertEquals(listOf("added.kt", "both.kt", "gone.kt"), status.unstaged.map { it.path })
        assertTrue(status.unstaged.all { it.kind == GitChangeKind.Conflicted })
    }

    @Test
    fun pathsWithSpacesAreKeptAsIs() {
        assertEquals("my file.txt", status("## main", "?? my file.txt").unstaged.single().path)
    }

    @Test
    fun branchHeaderCoversUnbornAndDetachedHeads() {
        assertEquals("main", status("## No commits yet on main").branch)
        assertEquals("main", status("## Initial commit on main").branch)
        assertNull(status("## HEAD (no branch)").branch)
        assertEquals("work", status("## work").branch)
    }

    private fun header(field: String) = parseGitBranchHeader("## $field\u0000 M a.kt\u0000")

    @Test
    fun branchHeaderCarriesTheUpstreamAndTheCommitCounts() {
        assertEquals(GitBranchHeader("main", "origin/main", ahead = 1, behind = 2), header("main...origin/main [ahead 1, behind 2]"))
        assertEquals(GitBranchHeader("main", "origin/main", ahead = 3), header("main...origin/main [ahead 3]"))
        assertEquals(GitBranchHeader("main", "origin/main", behind = 4), header("main...origin/main [behind 4]"))
        assertEquals(GitBranchHeader("feature/x", "origin/main"), header("feature/x...origin/main"))
        assertEquals(GitBranchHeader("main"), header("main"))
    }

    // 원격에서 지워진 upstream 은 비교할 수 없어서 없는 것으로 읽는다.
    @Test
    fun aGoneUpstreamIsNoUpstream() {
        assertEquals(GitBranchHeader("main"), header("main...origin/main [gone]"))
    }

    @Test
    fun branchHeaderMarksUnbornAndDetachedHeads() {
        assertEquals(GitBranchHeader("main", unborn = true), header("No commits yet on main"))
        assertEquals(GitBranchHeader("main", "origin/main", unborn = true), header("No commits yet on main...origin/main"))
        assertEquals(GitBranchHeader("main", unborn = true), header("Initial commit on main"))
        assertEquals(GitBranchHeader(null), header("HEAD (no branch)"))
        assertNull(parseGitBranchHeader(" M a.kt\u0000"))
    }

    @Test
    fun graphLinesAreCommitsDetailsOrEdges() {
        val output = listOf(
            "* \u001fh1\u001fa1\u001fHEAD -> main, origin/main, tag: v1\u001fdev\u001f2026-09-25 10:00\u001f사이드 바 | 추가",
            "|\\  \u001e",
            "| * \u001fh2\u001fb2\u001f\u001fdev\u001f2026-09-24 09:00\u001f고침",
            "|/  \u001e",
            "* \u001fh3\u001fc3\u001f\u001fdev\u001f2026-09-20 08:00\u001f처음",
            "  \u001e",
        ).joinToString("\n")

        val lines = parseGitGraph(output)
        val head = GitCommit("h1", "a1", listOf("HEAD -> main", "origin/main", "tag: v1"), "dev", "2026-09-25 10:00", "사이드 바 | 추가")

        assertEquals(6, lines.size)
        assertEquals(GitGraphLine("*", head), lines[0])
        assertEquals(GitGraphLine("|\\", head, isDetail = true), lines[1])
        assertEquals("| *", lines[2].graph)
        assertEquals(emptyList(), lines[2].commit!!.refs)
        assertTrue(head.isHead)
        assertFalse(lines[2].commit!!.isHead)
        assertEquals(GitGraphLine("", lines[4].commit, isDetail = true), lines[5])
    }

    @Test
    fun nameStatusListsCommitFilesByPathWithRenamesOnTheNewPath() {
        val output = listOf("M", "z.kt", "A", "app/New.kt", "R094", "old/Name.kt", "app/Name.kt", "D", "gone.txt", "T", "link").joinToString("\u0000", postfix = "\u0000")

        assertEquals(
            listOf(
                GitChange("app/Name.kt", GitChangeKind.Renamed, "old/Name.kt"),
                GitChange("app/New.kt", GitChangeKind.Added),
                GitChange("gone.txt", GitChangeKind.Deleted),
                GitChange("link", GitChangeKind.TypeChanged),
                GitChange("z.kt", GitChangeKind.Modified),
            ),
            parseGitNameStatus(output),
        )
        assertEquals(emptyList(), parseGitNameStatus(""))
    }

    // docs/common/terminal-commit-file.html K4, K5
    @Test
    fun commitDiffPicksTheSectionOfOnePathAmongRenamedAndDeletedFiles() {
        val output = listOf(
            "diff --git a/old.txt b/새 이름.txt",
            "similarity index 83%",
            "rename from old.txt",
            "rename to 새 이름.txt",
            "index b8cb000..0970e47 100644",
            "--- a/old.txt",
            "+++ b/새 이름.txt\t",
            "@@ -5,0 +6 @@ l5",
            "+l6",
            "diff --git a/gone.txt b/gone.txt",
            "deleted file mode 100644",
            "--- a/gone.txt",
            "+++ /dev/null",
            "@@ -1,2 +0,0 @@",
            "-x",
            "-y",
            "diff --git a/a.txt b/a.txt",
            "--- a/a.txt",
            "+++ b/a.txt",
            "@@ -2 +2 @@ a",
            "-b",
            "+B",
        ).joinToString("\n")

        assertEquals(GitFileDiff(listOf(GitDiffHunk(5, 0, 6, 1, emptyList()))), parseGitCommitDiff(output, "새 이름.txt"))
        assertEquals(GitFileDiff(listOf(GitDiffHunk(1, 2, 0, 0, listOf("x", "y")))), parseGitCommitDiff(output, "gone.txt"))
        assertEquals(GitFileDiff(listOf(GitDiffHunk(2, 1, 2, 1, listOf("b")))), parseGitCommitDiff(output, "a.txt"))
        // 옛 경로로는 찾지 않는다 — 그 커밋에는 없는 이름이다.
        assertEquals(GitFileDiff(emptyList()), parseGitCommitDiff(output, "old.txt"))
        assertEquals(GitFileDiff(emptyList()), parseGitCommitDiff("", "a.txt"))
    }

    @Test
    fun aDetachedHeadIsStillTheHeadCommit() {
        val line = parseGitGraph("* \u001fh\u001fa\u001fHEAD, main\u001fdev\u001fd\u001fs").single()

        assertTrue(line.commit!!.isHead)
    }

    @Test
    fun edgeOnlyLinesHaveNoCommit() {
        assertEquals(listOf(GitGraphLine("|\\")), parseGitGraph("|\\  \n"))
    }

    @Test
    fun diffHunksReadCountsThatGitLeavesOut() {
        val diff = parseGitDiff(
            """
            diff --git a/f.txt b/f.txt
            index d68dd40..6fe8acc 100644
            --- a/f.txt
            +++ b/f.txt
            @@ -2 +2 @@ a
            -b
            +B
            @@ -4,0 +5,2 @@ d
            +e
            +f
            @@ -9,2 +10,0 @@
            --- 줄
            -끝
            \ No newline at end of file
            """.trimIndent(),
        )

        assertEquals(
            GitFileDiff(
                listOf(
                    GitDiffHunk(oldStart = 2, oldCount = 1, newStart = 2, newCount = 1, removed = listOf("b")),
                    GitDiffHunk(oldStart = 4, oldCount = 0, newStart = 5, newCount = 2, removed = emptyList()),
                    GitDiffHunk(oldStart = 9, oldCount = 2, newStart = 10, newCount = 0, removed = listOf("-- 줄", "끝")),
                ),
            ),
            diff,
        )
    }

    @Test
    fun emptyDiffHasNoHunks() {
        assertEquals(GitFileDiff(emptyList()), parseGitDiff(""))
    }
}
