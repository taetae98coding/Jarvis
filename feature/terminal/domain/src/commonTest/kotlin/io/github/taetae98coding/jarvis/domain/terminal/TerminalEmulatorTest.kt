package io.github.taetae98coding.jarvis.domain.terminal

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TerminalEmulatorTest {
    private fun emulator(columns: Int = 10, rows: Int = 4) = TerminalEmulator(columns, rows)

    private fun TerminalEmulator.row(index: Int) = line(index).text()

    @Test
    fun printsTextAndMovesCursor() {
        val e = emulator()

        e.feed("hi")

        assertEquals("hi", e.row(0))
        assertEquals(0 to 2, e.cursorRow to e.cursorColumn)
    }

    @Test
    fun carriageReturnAndLineFeed() {
        val e = emulator()

        e.feed("ab\r\ncd")

        assertEquals("ab", e.row(0))
        assertEquals("cd", e.row(1))
    }

    @Test
    fun wrapsAtTheRightEdgeOnlyWhenTheNextCharacterArrives() {
        val e = emulator(columns = 3)

        e.feed("abc")
        assertEquals(0 to 2, e.cursorRow to e.cursorColumn)

        e.feed("d")
        assertEquals("abc", e.row(0))
        assertEquals("d", e.row(1))
        assertTrue(e.line(0).wrapped)
    }

    @Test
    fun scrollsIntoScrollback() {
        val e = emulator(rows = 2)

        e.feed("1\r\n2\r\n3")

        assertEquals("2", e.row(0))
        assertEquals("3", e.row(1))
        assertEquals(1, e.scrollbackSize)
        assertEquals("1", e.row(-1))
    }

    @Test
    fun scrolledLinesCountsLinesPushedIntoScrollback() {
        val e = TerminalEmulator(columns = 10, rows = 3, scrollbackLimit = 1)

        e.feed("1\r\n2\r\n3\r\n4\r\n5")
        assertEquals(2L, e.scrolledLines)
        assertEquals(1, e.scrollbackSize)

        // 위쪽 여백이 0 이 아닌 스크롤 영역과 대체 화면의 스크롤은 스크롤백에 들어가지 않으므로 세지 않는다.
        e.feed("\u001b[2;3r\u001b[3;1H\n\n\u001b[r")
        assertEquals(2L, e.scrolledLines)
        e.feed("\u001b[?1049h\u001b[3;1H\n\n\u001b[?1049l")
        assertEquals(2L, e.scrolledLines)

        // 창 높이를 줄여 커서 위의 줄이 올라가는 것은 센다.
        e.feed("\u001b[3;1Hx")
        e.resize(10, 1)
        assertEquals(4L, e.scrolledLines)
    }

    @Test
    fun scrollbackIsCapped() {
        val e = TerminalEmulator(columns = 5, rows = 1, scrollbackLimit = 3)

        repeat(10) { e.feed("$it\r\n") }

        assertEquals(3, e.scrollbackSize)
        assertEquals("7", e.row(-3))
    }

    @Test
    fun cursorPositionIsOneBased() {
        val e = emulator()

        e.feed("\u001b[3;5HX")

        assertEquals("    X", e.row(2))
    }

    @Test
    fun relativeCursorMovesStopAtTheEdges() {
        val e = emulator()

        e.feed("\u001b[99B\u001b[99C")

        assertEquals(3 to 9, e.cursorRow to e.cursorColumn)
    }

    @Test
    fun eraseLineFromCursor() {
        val e = emulator()

        e.feed("abcdef\u001b[3G\u001b[K")

        assertEquals("ab", e.row(0))
    }

    @Test
    fun eraseDisplayClearsEverything() {
        val e = emulator()

        e.feed("a\r\nb\u001b[2J")

        assertEquals("", e.row(0))
        assertEquals("", e.row(1))
    }

    @Test
    fun deleteAndInsertCharacters() {
        val e = emulator()

        e.feed("abcdef\u001b[1G\u001b[2P")
        assertEquals("cdef", e.row(0))

        e.feed("\u001b[2@")
        assertEquals("  cdef", e.row(0))
    }

    @Test
    fun sgrSetsPaletteAndTrueColors() {
        val e = emulator()

        e.feed("\u001b[1;31mA\u001b[38;5;200mB\u001b[38;2;1;2;3mC\u001b[48:2::4:5:6mD\u001b[0mE")

        val line = e.line(0)
        assertTrue(line.styleAt(0).bold)
        assertEquals(1, line.styleAt(0).foreground.paletteIndex)
        assertEquals(200, line.styleAt(1).foreground.paletteIndex)
        assertEquals(0x010203, line.styleAt(2).foreground.rgb)
        assertEquals(0x040506, line.styleAt(3).background.rgb)
        assertEquals(TerminalStyle.Default, line.styleAt(4))
    }

    @Test
    fun brightColorsMapToTheUpperPalette() {
        val e = emulator()

        e.feed("\u001b[92;104mA")

        assertEquals(10, e.line(0).styleAt(0).foreground.paletteIndex)
        assertEquals(12, e.line(0).styleAt(0).background.paletteIndex)
    }

    @Test
    fun scrollRegionKeepsLinesOutsideIt() {
        val e = emulator(rows = 4)

        e.feed("top\u001b[4;1Hbottom\u001b[2;3r\u001b[3;1Ha\r\nb\r\nc")

        assertEquals("top", e.row(0))
        assertEquals("b", e.row(1))
        assertEquals("c", e.row(2))
        assertEquals("bottom", e.row(3))
        // 영역이 화면 맨 위가 아니면 밀려난 줄을 기록하지 않는다.
        assertEquals(0, e.scrollbackSize)
    }

    @Test
    fun alternateScreenRestoresMainScreen() {
        val e = emulator()

        e.feed("shell\u001b[?1049h")
        assertTrue(e.isAlternateScreen)
        assertEquals("", e.row(0))

        e.feed("vim\u001b[?1049l")
        assertFalse(e.isAlternateScreen)
        assertEquals("shell", e.row(0))
        assertEquals(0 to 5, e.cursorRow to e.cursorColumn)
    }

    @Test
    fun wideCharactersTakeTwoCells() {
        val e = emulator()

        e.feed("한a")

        val line = e.line(0)
        assertEquals("한", line.textAt(0))
        assertTrue(line.isWideTail(1))
        assertEquals("a", line.textAt(2))
        assertEquals(3, e.cursorColumn)
    }

    @Test
    fun wideCharacterAtTheLastColumnWrapsFirst() {
        val e = emulator(columns = 3)

        e.feed("ab한")

        assertEquals("ab", e.row(0))
        assertEquals("한", e.row(1))
    }

    @Test
    fun decomposedHangulStaysInTwoCells() {
        val e = emulator()

        // macOS 파일 이름처럼 초성 ㅎ + 중성 ㅏ + 종성 ㄴ 으로 나뉘어 온다.
        e.feed("한x")

        assertEquals("한", e.line(0).textAt(0))
        assertEquals("x", e.line(0).textAt(2))
    }

    @Test
    fun overwritingHalfOfAWideCharacterClearsTheOtherHalf() {
        val e = emulator()

        e.feed("한\u001b[2Gx")

        assertEquals(" x", e.row(0))
    }

    @Test
    fun utf8SplitAcrossChunksIsReassembled() {
        val e = emulator()
        val bytes = "가".encodeToByteArray()

        e.feed(bytes, 0, 1)
        e.feed(bytes, 1, 2)

        assertEquals("가", e.row(0))
    }

    @Test
    fun oscSetsTitleWithEitherTerminator() {
        val e = emulator()

        e.feed("\u001b]0;first\u0007")
        assertEquals("first", e.title)

        e.feed("\u001b]2;second\u001b\\after")
        assertEquals("second", e.title)
        assertEquals("after", e.row(0))
    }

    @Test
    fun oscHyperlinkAttachesToPrintedCells() {
        val e = emulator(columns = 12)

        e.feed("a\u001b]8;;https://x.io\u0007b가\u001b]8;;\u001b\\c")

        assertEquals("ab가c", e.row(0))
        assertEquals(null, e.line(0).linkAt(0))
        assertEquals("https://x.io", e.line(0).linkAt(1))
        assertEquals("https://x.io", e.line(0).linkAt(2))
        assertEquals("https://x.io", e.line(0).linkAt(3))
        assertEquals(null, e.line(0).linkAt(4))
    }

    @Test
    fun oscHyperlinkIgnoresParamsAndEndsOnReset() {
        val e = emulator()

        e.feed("\u001b]8;id=1;https://x.io\u0007a")
        assertEquals("https://x.io", e.line(0).linkAt(0))

        e.feed("\u001bcb")
        assertEquals("b", e.row(0))
        assertEquals(null, e.line(0).linkAt(0))
    }

    @Test
    fun cursorPositionReportIsAnswered() {
        val e = emulator()

        e.feed("\u001b[2;3H\u001b[6n")

        assertEquals("\u001b[2;3R", e.drainResponses().decodeToString())
        assertEquals(0, e.drainResponses().size)
    }

    @Test
    fun unknownSequencesLeaveNoGarbage() {
        val e = emulator()

        e.feed("\u001b[>4;2m\u001b[?1000h\u001bP+q544e\u001b\\\u001b[5 qok")

        assertEquals("ok", e.row(0))
    }

    @Test
    fun applicationCursorKeysModeIsTracked() {
        val e = emulator()

        e.feed("\u001b[?1h")
        assertTrue(e.applicationCursorKeys)

        e.feed("\u001b[?1l")
        assertFalse(e.applicationCursorKeys)
    }

    @Test
    fun focusReportingModeIsTracked() {
        val e = emulator()

        e.feed("\u001b[?2004h\u001b[?2031h\u001b[?1004h")
        assertTrue(e.focusReporting)
        assertTrue(e.bracketedPaste)

        e.feed("\u001b[?1004l")
        assertFalse(e.focusReporting)
    }

    @Test
    fun lineDrawingCharsetMapsToBoxCharacters() {
        val e = emulator()

        e.feed("\u001b(0lqk\u001b(Bq")

        assertEquals("┌─┐q", e.row(0))
    }

    @Test
    fun shrinkingRowsKeepsTheCursorOnScreen() {
        val e = emulator(rows = 4)

        e.feed("1\r\n2\r\n3\r\n4")
        e.resize(10, 2)

        assertEquals("3", e.row(0))
        assertEquals("4", e.row(1))
        assertEquals(1, e.cursorRow)
        assertEquals("2", e.row(-1))
    }

    @Test
    fun shrinkingRowsDropsBlankLinesBelowTheCursorFirst() {
        val e = emulator(rows = 4)

        e.feed("prompt")
        e.resize(10, 2)

        assertEquals("prompt", e.row(0))
        assertEquals(0, e.scrollbackSize)
    }

    @Test
    fun narrowingTruncatesLines() {
        val e = emulator()

        e.feed("abcdefghij")
        e.resize(4, 4)

        assertEquals("abcd", e.row(0))
        assertEquals(3, e.cursorColumn)
    }

    @Test
    fun reverseIndexAtTopScrollsDown() {
        val e = emulator(rows = 2)

        e.feed("a\u001bM")

        assertEquals("", e.row(0))
        assertEquals("a", e.row(1))
    }

    @Test
    fun eraseUsesTheCurrentBackground() {
        val e = emulator()

        e.feed("\u001b[41m\u001b[2K")

        assertEquals(1, e.line(0).styleAt(5).background.paletteIndex)
    }
}
