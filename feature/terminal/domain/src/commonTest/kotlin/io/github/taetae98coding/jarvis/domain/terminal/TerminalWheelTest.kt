package io.github.taetae98coding.jarvis.domain.terminal

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TerminalWheelTest {
    private fun emulator(vararg sequences: String) = TerminalEmulator(columns = 80, rows = 24).apply {
        sequences.forEach(::feed)
    }

    private fun assertEncodes(expected: String, actual: ByteArray?) =
        assertContentEquals(expected.encodeToByteArray(), actual)

    @Test
    fun mainScreenWithoutMouseTrackingScrollsTheScrollback() {
        assertNull(encodeTerminalWheel(emulator(), lines = 3, column = 0, row = 0))
        assertNull(encodeTerminalWheel(emulator(), lines = -3, column = 0, row = 0))
    }

    @Test
    fun sgrReportsOneWheelEventPerLineAtTheOneBasedCell() {
        val emulator = emulator("\u001b[?1000h\u001b[?1006h")

        assertEncodes("\u001b[<64;5;3M\u001b[<64;5;3M", encodeTerminalWheel(emulator, lines = 2, column = 4, row = 2))
        assertEncodes("\u001b[<65;1;1M", encodeTerminalWheel(emulator, lines = -1, column = 0, row = 0))
    }

    @Test
    fun x10ReportsOffsetBytesAndClampsLargeCoordinates() {
        val emulator = TerminalEmulator(columns = 300, rows = 24).apply { feed("\u001b[?1002h") }

        assertContentEquals(
            byteArrayOf(0x1B, '['.code.toByte(), 'M'.code.toByte(), (32 + 64).toByte(), (32 + 5).toByte(), (32 + 3).toByte()),
            encodeTerminalWheel(emulator, lines = 1, column = 4, row = 2),
        )
        assertContentEquals(
            byteArrayOf(0x1B, '['.code.toByte(), 'M'.code.toByte(), (32 + 65).toByte(), (32 + 223).toByte(), (32 + 1).toByte()),
            encodeTerminalWheel(emulator, lines = -1, column = 299, row = 0),
        )
    }

    @Test
    fun coordinatesOutsideTheGridAreClampedToIt() {
        val emulator = emulator("\u001b[?1003h\u001b[?1006h")

        assertEncodes("\u001b[<64;80;24M", encodeTerminalWheel(emulator, lines = 1, column = 100, row = 50))
        assertEncodes("\u001b[<64;1;1M", encodeTerminalWheel(emulator, lines = 1, column = -1, row = -1))
    }

    @Test
    fun mouseTrackingWinsOverAlternateScroll() {
        val emulator = emulator("\u001b[?1049h\u001b[?1000h\u001b[?1006h")

        assertEncodes("\u001b[<65;1;1M", encodeTerminalWheel(emulator, lines = -1, column = 0, row = 0))
    }

    @Test
    fun alternateScreenWithoutMouseTrackingSendsArrowKeysPerLine() {
        assertEncodes("\u001b[A\u001b[A\u001b[A", encodeTerminalWheel(emulator("\u001b[?1049h"), lines = 3, column = 0, row = 0))
        assertEncodes("\u001bOB", encodeTerminalWheel(emulator("\u001b[?1049h\u001b[?1h"), lines = -1, column = 0, row = 0))
    }

    @Test
    fun alternateScrollCanBeTurnedOff() {
        val emulator = emulator("\u001b[?1049h\u001b[?1007l")

        assertFalse(emulator.alternateScroll)
        assertNull(encodeTerminalWheel(emulator, lines = 1, column = 0, row = 0))
    }

    @Test
    fun resettingAnyTrackingModeStopsReporting() {
        val emulator = emulator("\u001b[?1003h")
        assertTrue(emulator.mouseTracking)

        emulator.feed("\u001b[?1000l")

        assertFalse(emulator.mouseTracking)
        assertNull(encodeTerminalWheel(emulator, lines = 1, column = 0, row = 0))
    }

    @Test
    fun claudeCodeFullscreenModesReportSgrWheel() {
        // Claude Code 2.1.282 의 "tui": "fullscreen" 이 켜자마자 보내는 모드들.
        val emulator = emulator("\u001b[?2004h\u001b[?1004h\u001b[?1049h\u001b[?1000h\u001b[?1002h\u001b[?1003h\u001b[?1006h")

        assertEncodes("\u001b[<64;10;5M", encodeTerminalWheel(emulator, lines = 1, column = 9, row = 4))
    }

    @Test
    fun fullResetRestoresMouseDefaults() {
        val emulator = emulator("\u001b[?1000h\u001b[?1006h\u001b[?1007l")

        emulator.feed("\u001bc")

        assertFalse(emulator.mouseTracking)
        assertFalse(emulator.sgrMouse)
        assertTrue(emulator.alternateScroll)
    }

    @Test
    fun zeroLinesSendsNothing() {
        assertNull(encodeTerminalWheel(emulator("\u001b[?1000h"), lines = 0, column = 0, row = 0))
    }
}
