package io.github.taetae98coding.jarvis.domain.terminal

import io.github.taetae98coding.jarvis.domain.terminal.DiffedLine.Current
import io.github.taetae98coding.jarvis.domain.terminal.DiffedLine.Removed
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LineCommentTest {
    @Test
    fun rangeLabelNamesCurrentLinesOrRemovedLines() {
        assertEquals("12행", comment(Current(12, "a")).rangeLabel)
        assertEquals("12–14행", comment(Current(12, "a"), Removed(13, "b"), Current(14, "c")).rangeLabel)
        assertEquals("2행", comment(Removed(2, "b"), Current(2, "B", added = true)).rangeLabel)
        assertEquals("HEAD 5–6행 (지운 줄)", comment(Removed(5, "x"), Removed(6, "y")).rangeLabel)
        assertEquals("HEAD 5행 (지운 줄)", comment(Removed(5, "x")).rangeLabel)
    }

    @Test
    fun sameLineComparesNumbersOfTheSameKind() {
        assertTrue(Current(3, "a").sameLine(Current(3, "changed", added = true)))
        assertFalse(Current(3, "a").sameLine(Current(4, "a")))
        assertTrue(Removed(3, "a").sameLine(Removed(3, "b")))
        assertFalse(Removed(3, "a").sameLine(Current(3, "a")))
    }

    @Test
    fun promptListsCommentsWithRelativePathsAndCode() {
        val comments = listOf(
            LineComment(1, "/repo/src/Foo.kt", listOf(Removed(2, "b"), Current(2, "B", added = true)), "이 이름은 너무 짧다"),
            LineComment(2, "/repo/src/Foo.kt", listOf(Current(4, "d")), "  여기 null 이면?\n"),
        )

        assertEquals(
            """
            아래 코드 줄에 남긴 코멘트 2개를 반영해 주세요.

            1. src/Foo.kt:2
            ```
              − b
            2 + B
            ```
            이 이름은 너무 짧다

            2. src/Foo.kt:4
            ```
            4 d
            ```
            여기 null 이면?
            """.trimIndent(),
            lineCommentsPrompt(comments, "/repo/"),
        )
    }

    @Test
    fun promptGroupsCommentsByFileInFirstCommentOrder() {
        val comments = listOf(
            LineComment(1, "/repo/b.kt", listOf(Current(9, "x")), "b1"),
            LineComment(2, "/repo/a.kt", listOf(Current(1, "y")), "a1"),
            LineComment(3, "/repo/b.kt", listOf(Current(2, "z")), "b2"),
        )

        val headings = lineCommentsPrompt(comments, "/repo").lines().filter { it.firstOrNull()?.isDigit() == true && ". " in it }

        assertEquals(listOf("1. b.kt:9", "2. b.kt:2", "3. a.kt:1"), headings)
    }

    @Test
    fun promptKeepsAbsolutePathsOutsideTheDirectoryAndNamesRemovedOnlyRanges() {
        val comments = listOf(LineComment(1, "/other/Gone.kt", listOf(Removed(5, "x"), Removed(6, "y")), "왜 지웠지"))

        val prompt = lineCommentsPrompt(comments, "/repo")

        assertTrue("1. /other/Gone.kt (HEAD 5-6, 지운 줄)" in prompt.lines(), prompt)
        assertTrue(" − x" in prompt.lines(), prompt)
    }

    @Test
    fun promptWidensTheFenceAroundBackticksAndPadsNumbers() {
        val comments = listOf(LineComment(1, "/repo/README.md", listOf(Current(9, "```kotlin"), Current(10, "````")), "펜스"))

        val lines = lineCommentsPrompt(comments, "/repo").lines()

        assertTrue("`````" in lines, lines.toString())
        assertTrue(" 9 ```kotlin" in lines, lines.toString())
        assertTrue("10 ````" in lines, lines.toString())
    }

    private fun comment(vararg lines: DiffedLine) = LineComment(1, "/f", lines.toList(), "c")
}
