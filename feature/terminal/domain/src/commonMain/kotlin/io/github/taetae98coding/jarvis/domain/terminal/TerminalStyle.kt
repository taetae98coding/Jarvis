package io.github.taetae98coding.jarvis.domain.terminal

import kotlin.jvm.JvmInline

/**
 * 셀의 색 하나. 0 은 기본색, 1–256 은 팔레트 번호 + 1, 그 위는 [TrueColorFlag] 가 붙은 RGB 다.
 * 셀마다 객체를 만들지 않으려고 [TerminalStyle] 에 비트로 채워 넣는다.
 */
@JvmInline
value class TerminalColor(val bits: Int) {
    val isDefault: Boolean get() = bits == 0

    val paletteIndex: Int? get() = if (bits in 1..256) bits - 1 else null

    val rgb: Int? get() = if (bits and TrueColorFlag != 0) bits and 0xFFFFFF else null

    companion object {
        val Default = TerminalColor(0)

        internal const val TrueColorFlag = 1 shl 24

        fun palette(index: Int): TerminalColor = TerminalColor(index.coerceIn(0, 255) + 1)

        fun rgb(red: Int, green: Int, blue: Int): TerminalColor =
            TerminalColor(
                TrueColorFlag or
                    (red.coerceIn(0, 255) shl 16) or
                    (green.coerceIn(0, 255) shl 8) or
                    blue.coerceIn(0, 255),
            )
    }
}

/** 글자색 25비트, 배경색 25비트, 속성 8비트를 Long 하나에 담는다. */
@JvmInline
value class TerminalStyle(val bits: Long) {
    val foreground: TerminalColor get() = TerminalColor((bits and ColorMask).toInt())

    val background: TerminalColor get() = TerminalColor(((bits shr BackgroundShift) and ColorMask).toInt())

    val bold: Boolean get() = has(Bold)
    val dim: Boolean get() = has(Dim)
    val italic: Boolean get() = has(Italic)
    val underline: Boolean get() = has(Underline)
    val inverse: Boolean get() = has(Inverse)
    val hidden: Boolean get() = has(Hidden)
    val strikethrough: Boolean get() = has(Strikethrough)

    fun withForeground(color: TerminalColor): TerminalStyle =
        TerminalStyle((bits and ColorMask.inv()) or color.bits.toLong())

    fun withBackground(color: TerminalColor): TerminalStyle =
        TerminalStyle((bits and (ColorMask shl BackgroundShift).inv()) or (color.bits.toLong() shl BackgroundShift))

    internal fun with(flag: Int, enabled: Boolean): TerminalStyle {
        val mask = flag.toLong() shl FlagShift

        return TerminalStyle(if (enabled) bits or mask else bits and mask.inv())
    }

    private fun has(flag: Int): Boolean = (bits shr FlagShift) and flag.toLong() != 0L

    companion object {
        val Default = TerminalStyle(0)

        private const val ColorMask = (1L shl 25) - 1
        private const val BackgroundShift = 25
        private const val FlagShift = 50

        internal const val Bold = 1
        internal const val Dim = 2
        internal const val Italic = 4
        internal const val Underline = 8
        internal const val Inverse = 16
        internal const val Hidden = 32
        internal const val Strikethrough = 64
    }
}
