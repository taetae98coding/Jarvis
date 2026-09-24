package io.github.taetae98coding.jarvis.data.emulator.mirror

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertFailsWith

class ControlMessagesTest {
    @Test
    fun keycodeIsTypeActionKeycodeRepeatMeta() {
        assertContentEquals(
            byteArrayOf(0, 1, 0, 0, 0, 66, 0, 0, 0, 0, 0, 0, 0, 0),
            ControlMessages.encodeKeycode(ControlMessages.KeyActionUp, keycode = 66),
        )
    }

    @Test
    fun textIsLengthPrefixedUtf8() {
        assertContentEquals(byteArrayOf(1, 0, 0, 0, 2, 'h'.code.toByte(), 'i'.code.toByte()), ControlMessages.encodeText("hi"))
        assertFailsWith<IllegalArgumentException> { ControlMessages.encodeText("a".repeat(ControlMessages.InjectTextMaxBytes + 1)) }
    }

    @Test
    fun clipboardCarriesSequencePasteFlagAndUtf8() {
        val encoded = ControlMessages.encodeSetClipboard("가", paste = true)

        assertContentEquals(
            byteArrayOf(9, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 3) + "가".encodeToByteArray(),
            encoded,
        )
    }
}
