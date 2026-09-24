package io.github.taetae98coding.jarvis.domain.terminal

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertNull

class TerminalKeyTest {
    private fun assertEncodes(expected: String, actual: ByteArray) =
        assertContentEquals(expected.encodeToByteArray(), actual)

    @Test
    fun arrowsFollowCursorKeyMode() {
        assertEncodes("\u001b[A", encodeTerminalKey(TerminalKey.Up))
        assertEncodes("\u001bOA", encodeTerminalKey(TerminalKey.Up, applicationCursorKeys = true))
    }

    @Test
    fun modifiedArrowsUseCsiEvenInApplicationMode() {
        assertEncodes(
            "\u001b[1;5C",
            encodeTerminalKey(TerminalKey.Right, TerminalKeyModifiers(ctrl = true), applicationCursorKeys = true),
        )
    }

    @Test
    fun editingKeys() {
        assertEncodes("\r", encodeTerminalKey(TerminalKey.Enter))
        assertEncodes("\u007f", encodeTerminalKey(TerminalKey.Backspace))
        assertEncodes("\t", encodeTerminalKey(TerminalKey.Tab))
        assertEncodes("\u001b[Z", encodeTerminalKey(TerminalKey.Tab, TerminalKeyModifiers(shift = true)))
        assertEncodes("\u001b[3~", encodeTerminalKey(TerminalKey.Delete))
        assertEncodes("\u001b[5;2~", encodeTerminalKey(TerminalKey.PageUp, TerminalKeyModifiers(shift = true)))
    }

    @Test
    fun functionKeys() {
        assertEncodes("\u001bOP", encodeTerminalKey(TerminalKey.F1))
        assertEncodes("\u001b[15~", encodeTerminalKey(TerminalKey.F5))
        assertEncodes("\u001b[24~", encodeTerminalKey(TerminalKey.F12))
    }

    @Test
    fun controlCharacters() {
        assertContentEquals(byteArrayOf(3), encodeControlCharacter('c'))
        assertContentEquals(byteArrayOf(3), encodeControlCharacter('C'))
        assertContentEquals(byteArrayOf(27), encodeControlCharacter('['))
        assertContentEquals(byteArrayOf(0), encodeControlCharacter(' '))
        assertContentEquals(byteArrayOf(0x1B, 1), encodeControlCharacter('a', alt = true))
        assertNull(encodeControlCharacter('1'))
    }
}
