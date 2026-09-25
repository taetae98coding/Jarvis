package io.github.taetae98coding.jarvis.domain.terminal

/**
 * 휠을 [lines] 줄(양수는 위로) 굴린 것을 셸에 보낼 바이트로 바꾼다. [column]·[row] 는 휠이 있는 칸이고 0부터 센다.
 * null 이면 셸에 보내지 않고 창이 스크롤백을 움직인다. 고르는 순서는 docs/common/terminal-scroll.html R1 이다.
 */
fun encodeTerminalWheel(emulator: TerminalEmulator, lines: Int, column: Int, row: Int): ByteArray? {
    if (lines == 0) return null

    val up = lines > 0
    val one = when {
        emulator.mouseTracking -> encodeWheelReport(
            up = up,
            column = column.coerceIn(0, emulator.columns - 1),
            row = row.coerceIn(0, emulator.rows - 1),
            sgr = emulator.sgrMouse,
        )

        emulator.isAlternateScreen && emulator.alternateScroll -> encodeTerminalKey(
            key = if (up) TerminalKey.Up else TerminalKey.Down,
            applicationCursorKeys = emulator.applicationCursorKeys,
        )

        else -> return null
    }

    val count = if (up) lines else -lines
    return ByteArray(one.size * count) { one[it % one.size] }
}

private fun encodeWheelReport(up: Boolean, column: Int, row: Int, sgr: Boolean): ByteArray {
    val button = if (up) WheelUpButton else WheelDownButton
    val x = column + 1
    val y = row + 1

    if (sgr) return "\u001b[<$button;$x;${y}M".encodeToByteArray()

    return byteArrayOf(
        0x1B,
        '['.code.toByte(),
        'M'.code.toByte(),
        (X10Offset + button).toByte(),
        (X10Offset + x.coerceAtMost(X10MaxCoordinate)).toByte(),
        (X10Offset + y.coerceAtMost(X10MaxCoordinate)).toByte(),
    )
}

private const val WheelUpButton = 64
private const val WheelDownButton = 65
private const val X10Offset = 32

// X10 인코딩은 좌표를 한 바이트(32 + n)에 담는다. 255 를 넘을 수 없다.
private const val X10MaxCoordinate = 223
