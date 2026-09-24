package io.github.taetae98coding.jarvis.domain.terminal

/** 글자로 나오지 않는 키. 글자 키는 입력된 문자열 그대로 셸에 간다. */
enum class TerminalKey {
    Enter,
    Backspace,
    Tab,
    Escape,
    Up,
    Down,
    Right,
    Left,
    Home,
    End,
    PageUp,
    PageDown,
    Insert,
    Delete,
    F1,
    F2,
    F3,
    F4,
    F5,
    F6,
    F7,
    F8,
    F9,
    F10,
    F11,
    F12,
}

data class TerminalKeyModifiers(
    val shift: Boolean = false,
    val alt: Boolean = false,
    val ctrl: Boolean = false,
) {
    // xterm 의 수정자 인자. 1 이 "수정자 없음" 이라 CSI 에서 생략된다.
    internal val parameter: Int get() = 1 + (if (shift) 1 else 0) + (if (alt) 2 else 0) + (if (ctrl) 4 else 0)

    companion object {
        val None = TerminalKeyModifiers()
    }
}

/** xterm 이 보내는 바이트와 같다. `TERM=xterm-256color` 의 terminfo 가 이 값을 기대한다. */
fun encodeTerminalKey(
    key: TerminalKey,
    modifiers: TerminalKeyModifiers = TerminalKeyModifiers.None,
    applicationCursorKeys: Boolean = false,
): ByteArray {
    val m = modifiers.parameter

    val sequence = when (key) {
        TerminalKey.Enter -> altPrefixed(modifiers, "\r")
        TerminalKey.Backspace -> if (modifiers.ctrl) "\b" else altPrefixed(modifiers, "\u007f")
        TerminalKey.Tab -> if (modifiers.shift) "\u001b[Z" else altPrefixed(modifiers, "\t")
        TerminalKey.Escape -> "\u001b"
        TerminalKey.Up -> cursor('A', m, applicationCursorKeys)
        TerminalKey.Down -> cursor('B', m, applicationCursorKeys)
        TerminalKey.Right -> cursor('C', m, applicationCursorKeys)
        TerminalKey.Left -> cursor('D', m, applicationCursorKeys)
        TerminalKey.Home -> cursor('H', m, applicationCursorKeys)
        TerminalKey.End -> cursor('F', m, applicationCursorKeys)
        TerminalKey.Insert -> tilde(2, m)
        TerminalKey.Delete -> tilde(3, m)
        TerminalKey.PageUp -> tilde(5, m)
        TerminalKey.PageDown -> tilde(6, m)
        TerminalKey.F1 -> ss3OrCsi('P', m)
        TerminalKey.F2 -> ss3OrCsi('Q', m)
        TerminalKey.F3 -> ss3OrCsi('R', m)
        TerminalKey.F4 -> ss3OrCsi('S', m)
        TerminalKey.F5 -> tilde(15, m)
        TerminalKey.F6 -> tilde(17, m)
        TerminalKey.F7 -> tilde(18, m)
        TerminalKey.F8 -> tilde(19, m)
        TerminalKey.F9 -> tilde(20, m)
        TerminalKey.F10 -> tilde(21, m)
        TerminalKey.F11 -> tilde(23, m)
        TerminalKey.F12 -> tilde(24, m)
    }

    return sequence.encodeToByteArray()
}

/**
 * Ctrl 과 함께 누른 글자의 제어 문자. 대응이 없는 글자면 null 이다 — 그때는 글자 그대로 보낸다.
 * `Ctrl+2`·`Ctrl+Space` 가 NUL, `Ctrl+[` 가 ESC 인 것은 VT100 키보드 배치에서 온 관례다.
 */
fun encodeControlCharacter(char: Char, alt: Boolean = false): ByteArray? {
    val code = when (char.lowercaseChar()) {
        in 'a'..'z' -> char.lowercaseChar() - 'a' + 1
        '@', ' ', '2' -> 0
        '[', '3' -> 27
        '\\', '4' -> 28
        ']', '5' -> 29
        '^', '6' -> 30
        '_', '/', '7' -> 31
        '8', '?' -> 127
        else -> return null
    }

    val bytes = byteArrayOf(code.toByte())

    return if (alt) byteArrayOf(0x1B) + bytes else bytes
}

private fun altPrefixed(modifiers: TerminalKeyModifiers, text: String): String =
    if (modifiers.alt) "\u001b$text" else text

// 수정자가 있으면 애플리케이션 모드여도 CSI 형태가 된다. `ESC O 1;5A` 같은 것은 없다.
private fun cursor(final: Char, m: Int, application: Boolean): String =
    when {
        m > 1 -> "\u001b[1;$m$final"
        application -> "\u001bO$final"
        else -> "\u001b[$final"
    }

private fun ss3OrCsi(final: Char, m: Int): String =
    if (m > 1) "\u001b[1;$m$final" else "\u001bO$final"

private fun tilde(code: Int, m: Int): String =
    if (m > 1) "\u001b[$code;$m~" else "\u001b[$code~"
