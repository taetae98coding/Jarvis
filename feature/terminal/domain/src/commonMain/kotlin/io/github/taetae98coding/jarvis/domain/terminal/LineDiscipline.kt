package io.github.taetae98coding.jarvis.domain.terminal

/**
 * pty 가 없는 세션에서 tty 의 줄 편집(canonical mode)을 흉내 낸다.
 *
 * pty 가 있으면 커널이 입력을 되비추고 Backspace 를 처리하고 Enter 에 한 줄을 넘긴다. 파이프에는
 * 그 층이 없어서 셸이 아무것도 되비추지 않는다. 그 일을 여기서 한다. 방향키 같은 이스케이프
 * 시퀀스는 편집할 방법이 없어 버린다.
 */
class LineDiscipline {
    private val line = StringBuilder()
    private var escape = EscapeState.None

    class Result(
        /** 화면에 되비출 바이트. 에뮬레이터에 먹인다. */
        val echo: ByteArray,
        /** 셸에 보낼 바이트. Enter 를 누르기 전까지는 비어 있다. */
        val send: ByteArray,
    )

    fun input(bytes: ByteArray): Result {
        val echo = StringBuilder()
        val send = StringBuilder()

        forEachCodePoint(bytes.decodeToString()) { codePoint ->
            when {
                escape != EscapeState.None -> skipEscape(codePoint)
                codePoint == 0x1B -> escape = EscapeState.Start
                codePoint == '\r'.code || codePoint == '\n'.code -> {
                    echo.append("\r\n")
                    send.append(line).append('\n')
                    line.clear()
                }

                codePoint == 0x7F || codePoint == 0x08 -> if (line.isNotEmpty()) echo.append(eraseLast())
                // 실행 중인 명령을 끊을 신호를 보낼 수 없다. 입력 중인 줄을 버리고 빈 줄을 보내 프롬프트를
                // 새로 받는다.
                codePoint == 0x03 -> {
                    echo.append("^C\r\n")
                    send.append('\n')
                    line.clear()
                }

                codePoint == 0x15 -> while (line.isNotEmpty()) echo.append(eraseLast())
                codePoint < 0x20 -> Unit
                else -> {
                    val text = codePointToString(codePoint)
                    line.append(text)
                    echo.append(text)
                }
            }
        }

        return Result(echo = echo.toString().encodeToByteArray(), send = send.toString().encodeToByteArray())
    }

    /** tty 의 `onlcr`. 셸 출력의 LF 앞에 CR 을 붙여 커서가 줄 맨 앞으로 돌아가게 한다. */
    fun output(bytes: ByteArray): ByteArray {
        val count = bytes.count { it == LF }
        if (count == 0) return bytes

        val result = ByteArray(bytes.size + count)
        var j = 0
        for (b in bytes) {
            if (b == LF) result[j++] = CR
            result[j++] = b
        }

        return result
    }

    private fun eraseLast(): String {
        val high = line.length - 1
        val start = if (high > 0 && line[high].isLowSurrogate() && line[high - 1].isHighSurrogate()) high - 1 else high
        val codePoint = line.substring(start).let { if (it.length == 2) toCodePoint(it[0], it[1]) else it[0].code }
        line.setLength(start)

        val width = terminalCharWidth(codePoint)

        return "\b".repeat(width) + " ".repeat(width) + "\b".repeat(width)
    }

    private fun skipEscape(codePoint: Int) {
        escape = when (escape) {
            EscapeState.Start -> if (codePoint == '['.code || codePoint == 'O'.code) EscapeState.Sequence else EscapeState.None
            EscapeState.Sequence -> if (codePoint in 0x40..0x7E) EscapeState.None else EscapeState.Sequence
            EscapeState.None -> EscapeState.None
        }
    }

    private enum class EscapeState { None, Start, Sequence }

    private companion object {
        const val LF: Byte = 0x0A
        const val CR: Byte = 0x0D
    }
}

internal inline fun forEachCodePoint(text: String, action: (Int) -> Unit) {
    var i = 0
    while (i < text.length) {
        val c = text[i]
        if (c.isHighSurrogate() && i + 1 < text.length && text[i + 1].isLowSurrogate()) {
            action(toCodePoint(c, text[i + 1]))
            i += 2
        } else {
            action(c.code)
            i++
        }
    }
}

private fun toCodePoint(high: Char, low: Char): Int = ((high.code - 0xD800) shl 10) + (low.code - 0xDC00) + 0x10000
