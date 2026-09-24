package io.github.taetae98coding.jarvis.domain.terminal

import kotlin.test.Test
import kotlin.test.assertEquals

class LineDisciplineTest {
    private fun LineDiscipline.type(text: String) = input(text.encodeToByteArray())

    @Test
    fun echoesButHoldsTheLineUntilEnter() {
        val discipline = LineDiscipline()

        val typed = discipline.type("ls")
        assertEquals("ls", typed.echo.decodeToString())
        assertEquals("", typed.send.decodeToString())

        val entered = discipline.type("\r")
        assertEquals("\r\n", entered.echo.decodeToString())
        assertEquals("ls\n", entered.send.decodeToString())
    }

    @Test
    fun backspaceErasesTheLastCharacter() {
        val discipline = LineDiscipline()

        discipline.type("lx")
        assertEquals("\b \b", discipline.type("\u007f").echo.decodeToString())
        discipline.type("s")

        assertEquals("ls\n", discipline.type("\r").send.decodeToString())
    }

    @Test
    fun backspaceErasesBothCellsOfAWideCharacter() {
        val discipline = LineDiscipline()

        discipline.type("한")

        assertEquals("\b\b  \b\b", discipline.type("\u007f").echo.decodeToString())
    }

    @Test
    fun backspaceOnAnEmptyLineDoesNothing() {
        assertEquals("", LineDiscipline().type("\u007f").echo.decodeToString())
    }

    @Test
    fun ctrlCDropsTheLine() {
        val discipline = LineDiscipline()

        discipline.type("rm -rf")
        val result = discipline.type("\u0003")

        assertEquals("^C\r\n", result.echo.decodeToString())
        assertEquals("\n", result.send.decodeToString())
        assertEquals("\n", discipline.type("\r").send.decodeToString())
    }

    @Test
    fun ctrlUErasesTheWholeLine() {
        val discipline = LineDiscipline()

        discipline.type("ab")
        assertEquals("\b \b\b \b", discipline.type("\u0015").echo.decodeToString())
    }

    @Test
    fun escapeSequencesAreDropped() {
        val discipline = LineDiscipline()

        discipline.type("a\u001b[A\u001bOBb")

        assertEquals("ab\n", discipline.type("\r").send.decodeToString())
    }

    @Test
    fun outputGetsCarriageReturns() {
        assertEquals("a\r\nb\r\n", LineDiscipline().output("a\nb\n".encodeToByteArray()).decodeToString())
    }
}
