package io.github.taetae98coding.jarvis.domain.terminal

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TerminalSelectionTest {
    private fun cell(row: Int, column: Int) = TerminalCell(row, column)

    private fun selection(anchor: TerminalCell, focus: TerminalCell, scrolledLines: Long = 0) =
        TerminalSelection(anchor, focus, scrolledLines)

    private fun emulator(text: String, columns: Int = 10, rows: Int = 4, scrollbackLimit: Int = 2000) =
        TerminalEmulator(columns, rows, scrollbackLimit).apply { feed(text) }

    @Test
    fun draggingBackwardsGivesTheSameOrderedBounds() {
        val e = emulator("abc\r\ndef")

        val forward = e.selectionBounds(selection(cell(0, 1), cell(1, 1)))
        val backward = e.selectionBounds(selection(cell(1, 1), cell(0, 1)))

        assertEquals(TerminalSelectionBounds(cell(0, 1), cell(1, 1)), forward)
        assertEquals(forward, backward)
        assertEquals("bc\nde", e.selectedText(selection(cell(1, 1), cell(0, 1))))
    }

    @Test
    fun firstRowRunsToTheEndMiddleRowsAreWholeAndTheLastRowStartsAtTheFirstColumn() {
        val e = emulator("aaaa\r\nbbbb\r\ncccc")
        val bounds = e.selectionBounds(selection(cell(0, 2), cell(2, 1)))!!

        assertEquals(2..9, bounds.columnsAt(0, 10))
        assertEquals(0..9, bounds.columnsAt(1, 10))
        assertEquals(0..1, bounds.columnsAt(2, 10))
        assertNull(bounds.columnsAt(3, 10))
        assertEquals("aa\nbbbb\ncc", e.selectedText(selection(cell(0, 2), cell(2, 1))))
    }

    @Test
    fun wideCharactersAreSelectedAsAWhole() {
        val e = emulator("가나 x")

        val bounds = e.selectionBounds(selection(cell(0, 1), cell(0, 2)))

        assertEquals(TerminalSelectionBounds(cell(0, 0), cell(0, 3)), bounds)
        assertEquals("가나", e.selectedText(selection(cell(0, 1), cell(0, 2))))
    }

    @Test
    fun trailingBlanksAreTrimmedAndBlankCellsInsideAreSpaces() {
        val e = emulator("a b")

        assertEquals("a b", e.selectedText(selection(cell(0, 0), cell(0, 9))))
        assertEquals("", e.selectedText(selection(cell(1, 0), cell(2, 9))))
    }

    @Test
    fun wrappedRowsJoinWithoutALineFeed() {
        val e = emulator("abcd", columns = 3)

        assertEquals("abcd", e.selectedText(selection(cell(0, 0), cell(1, 0))))
        assertEquals("abc", e.selectedText(selection(cell(0, 0), cell(0, 2))))
    }

    @Test
    fun rowsThatTheProgramBrokeJoinWithALineFeed() {
        val e = emulator("ab\r\ncd")

        assertEquals("ab\ncd", e.selectedText(selection(cell(0, 0), cell(1, 9))))
    }

    @Test
    fun trailingEmptyRowsAreDropped() {
        val e = emulator("ab")

        assertEquals("ab", e.selectedText(selection(cell(0, 0), cell(3, 9))))
    }

    @Test
    fun scrollbackRowsAreSelectable() {
        val e = emulator("1\r\n2\r\n3", rows = 2)

        assertEquals("1\n2", e.selectedText(selection(cell(-1, 0), cell(0, 0), scrolledLines = e.scrolledLines)))
    }

    @Test
    fun selectionFollowsTextPushedIntoScrollback() {
        val e = emulator("1\r\n2", rows = 2)
        val selection = selection(cell(0, 0), cell(1, 0), scrolledLines = e.scrolledLines)
        assertEquals("1\n2", e.selectedText(selection))

        e.feed("\r\n3")

        assertEquals(TerminalSelectionBounds(cell(-1, 0), cell(0, 0)), e.selectionBounds(selection))
        assertEquals("1\n2", e.selectedText(selection))
    }

    @Test
    fun selectionVanishesOnceItHasLeftTheScrollback() {
        val e = emulator("1\r\n2", rows = 2, scrollbackLimit = 0)
        val selection = selection(cell(0, 0), cell(0, 0), scrolledLines = e.scrolledLines)

        e.feed("\r\n3\r\n4")

        assertNull(e.selectionBounds(selection))
        assertEquals("", e.selectedText(selection))
    }

    @Test
    fun startAboveTheScrollbackIsClampedToItsOldestRow() {
        val e = emulator("1\r\n2", rows = 2, scrollbackLimit = 0)
        val selection = selection(cell(0, 5), cell(1, 0), scrolledLines = e.scrolledLines)

        e.feed("\r\n3")

        assertEquals(TerminalSelectionBounds(cell(0, 0), cell(0, 0)), e.selectionBounds(selection))
        assertEquals("2", e.selectedText(selection))
    }
}
