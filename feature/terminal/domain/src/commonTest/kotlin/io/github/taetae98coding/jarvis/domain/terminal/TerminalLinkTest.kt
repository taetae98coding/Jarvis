package io.github.taetae98coding.jarvis.domain.terminal

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TerminalLinkTest {
    private fun urls(text: String): List<String> = findUrls(text).map(text::substring)

    private fun cells(row: Int, columns: IntRange): List<TerminalCell> = columns.map { TerminalCell(row, it) }

    @Test
    fun findsHttpAndHttpsUrlsInText() {
        assertEquals(
            listOf("https://a.io/x?y=1&z=2", "http://b.io"),
            urls("see https://a.io/x?y=1&z=2 and http://b.io."),
        )
    }

    @Test
    fun trailingPunctuationAndUnbalancedBracketsAreNotPartOfTheUrl() {
        assertEquals(listOf("https://a.io/x"), urls("(https://a.io/x)."))
        assertEquals(listOf("https://a.io/x_(y)"), urls("https://a.io/x_(y),"))
        assertEquals(listOf("https://a.io/x"), urls("[https://a.io/x]?!"))
    }

    @Test
    fun quotesAngleBracketsAndWideCharactersEndTheUrl() {
        assertEquals(listOf("https://a.io/x"), urls("\"https://a.io/x\""))
        assertEquals(listOf("https://a.io/x"), urls("<https://a.io/x>"))
        assertEquals(listOf("https://a.io/x"), urls("https://a.io/x한글"))
        assertEquals(listOf("https://a.io/x"), urls("'https://a.io/x'"))
    }

    @Test
    fun schemeAloneIsNotAUrl() {
        assertEquals(emptyList(), urls("https:// and http://"))
        assertEquals(emptyList(), urls("httpx://a.io"))
    }

    @Test
    fun plainTextIsNotALink() {
        val e = TerminalEmulator(20, 3)
        e.feed("hello https://a.io")

        assertNull(e.linkAt(0, 0))
        assertNull(e.linkAt(0, 5))
        assertNull(e.linkAt(0, 19))
        assertNull(e.linkAt(3, 0))
        assertNull(e.linkAt(0, 20))
    }

    @Test
    fun linkCoversEveryCellOfTheUrl() {
        val e = TerminalEmulator(20, 3)
        e.feed("go https://a.io/x.")

        val link = e.linkAt(0, 8)

        assertEquals(TerminalLink("https://a.io/x", cells(0, 3..16)), link)
        assertEquals(link, e.linkAt(0, 3))
        assertEquals(link, e.linkAt(0, 16))
        assertNull(e.linkAt(0, 17))
    }

    @Test
    fun urlContinuesAcrossAutoWrappedRows() {
        val e = TerminalEmulator(10, 3)
        e.feed("x https://a.io/abc ok")

        val link = e.linkAt(1, 2)

        assertEquals("https://a.io/abc", link?.url)
        assertEquals(cells(0, 2..9) + cells(1, 0..7), link?.cells)
        assertEquals(link, e.linkAt(0, 5))
    }

    @Test
    fun urlBrokenByTheProgramAtTheLastColumnContinuesPastTheIndent() {
        val e = TerminalEmulator(10, 4)
        // 프로그램이 마지막 칸에서 끊고 다음 줄을 두 칸 들여 썼다. 자동 줄바꿈이 아니다. 둘째 줄은 마지막 칸 앞에서 끝나 그 다음 줄로는 잇지 않는다.
        e.feed("> https://\r\n  a.io/ab\r\n  next")

        val link = e.linkAt(1, 4)

        assertEquals("https://a.io/ab", link?.url)
        assertEquals(cells(0, 2..9) + cells(1, 2..8), link?.cells)
        assertEquals(link, e.linkAt(0, 3))
        assertNull(e.linkAt(1, 0))
        assertNull(e.linkAt(2, 2))
    }

    @Test
    fun urlThatStopsBeforeTheLastColumnDoesNotContinue() {
        val e = TerminalEmulator(14, 3)
        e.feed("https://a.io\r\nmore")

        assertEquals("https://a.io", e.linkAt(0, 0)?.url)
        assertNull(e.linkAt(1, 0))

        val f = TerminalEmulator(10, 3)
        f.feed("https://ab\r\n\r\nc")
        assertEquals("https://ab", f.linkAt(0, 0)?.url)
    }

    @Test
    fun scrollbackRowsAreSearchedToo() {
        val e = TerminalEmulator(20, 2)
        e.feed("https://a.io\r\n1\r\n2")

        assertEquals("https://a.io", e.linkAt(-1, 3)?.url)
        assertEquals(cells(-1, 0..11), e.linkAt(-1, 3)?.cells)
    }

    @Test
    fun hyperlinkCellsFormTheLinkRegardlessOfText() {
        val e = TerminalEmulator(6, 3)
        e.feed("\u001b]8;;https://a.io\u0007my PR\u001b]8;;\u0007 x")

        assertEquals(TerminalLink("https://a.io", cells(0, 0..4)), e.linkAt(0, 2))
        assertNull(e.linkAt(0, 5))
    }

    @Test
    fun hyperlinkSpanningRowsIsOneLink() {
        val e = TerminalEmulator(4, 3)
        e.feed("\u001b]8;;https://a.io\u0007abcdef\u001b]8;;\u0007")

        val link = e.linkAt(1, 1)

        assertEquals(cells(0, 0..3) + cells(1, 0..1), link?.cells)
        assertEquals(link, e.linkAt(0, 0))
    }
}
