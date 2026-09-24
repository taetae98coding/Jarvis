package io.github.taetae98coding.jarvis.domain.terminal

import io.github.taetae98coding.jarvis.domain.terminal.DiffedLine.Current
import io.github.taetae98coding.jarvis.domain.terminal.DiffedLine.Removed
import kotlin.test.Test
import kotlin.test.assertEquals

class DiffedLinesTest {
    private val lines = listOf("a", "B", "c", "d", "e")

    @Test
    fun withoutDiffEveryLineIsUnchanged() {
        val expected = lines.mapIndexed { index, text -> Current(index + 1, text) }

        assertEquals(expected, diffedLines(lines, null))
        assertEquals(expected, diffedLines(lines, GitFileDiff(emptyList())))
    }

    @Test
    fun changedLineShowsTheRemovedLineBeforeTheAddedOne() {
        // "a b c d" → "a B c d e"
        val diff = GitFileDiff(
            listOf(
                GitDiffHunk(oldStart = 2, oldCount = 1, newStart = 2, newCount = 1, removed = listOf("b")),
                GitDiffHunk(oldStart = 4, oldCount = 0, newStart = 5, newCount = 1, removed = emptyList()),
            ),
        )

        assertEquals(
            listOf(
                Current(1, "a"),
                Removed(2, "b"),
                Current(2, "B", added = true),
                Current(3, "c"),
                Current(4, "d"),
                Current(5, "e", added = true),
            ),
            diffedLines(lines, diff),
        )
        assertEquals(2, diff.added)
        assertEquals(1, diff.removed)
    }

    @Test
    fun pureRemovalGoesAfterTheLineItPoints() {
        // newCount 가 0 이면 newStart 는 그 줄 뒤다. 0 이면 맨 앞이다.
        val diff = GitFileDiff(
            listOf(
                GitDiffHunk(oldStart = 1, oldCount = 1, newStart = 0, newCount = 0, removed = listOf("first")),
                GitDiffHunk(oldStart = 4, oldCount = 2, newStart = 2, newCount = 0, removed = listOf("x", "y")),
                GitDiffHunk(oldStart = 9, oldCount = 1, newStart = 5, newCount = 0, removed = listOf("last")),
            ),
        )

        assertEquals(
            listOf(
                Removed(1, "first"),
                Current(1, "a"),
                Current(2, "B"),
                Removed(4, "x"),
                Removed(5, "y"),
                Current(3, "c"),
                Current(4, "d"),
                Current(5, "e"),
                Removed(9, "last"),
            ),
            diffedLines(lines, diff),
        )
    }

    @Test
    fun deletedFileIsOnlyRemovedLines() {
        val diff = GitFileDiff(listOf(GitDiffHunk(oldStart = 1, oldCount = 2, newStart = 0, newCount = 0, removed = listOf("a", "b"))))

        assertEquals(listOf(Removed(1, "a"), Removed(2, "b")), diffedLines(emptyList(), diff))
    }

    @Test
    fun marksOutsideTheLinesAreDropped() {
        // 파일이 잘려 앞 두 줄만 있다.
        val diff = GitFileDiff(
            listOf(
                GitDiffHunk(oldStart = 1, oldCount = 0, newStart = 2, newCount = 1, removed = emptyList()),
                GitDiffHunk(oldStart = 5, oldCount = 1, newStart = 6, newCount = 1, removed = listOf("gone")),
            ),
        )

        assertEquals(listOf(Current(1, "a"), Current(2, "B", added = true)), diffedLines(lines.take(2), diff))
    }
}
