package io.github.taetae98coding.jarvis.ui.calculator

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.ui.text.TextRange

/** 계산기 자판. [insert] 가 있는 키는 그 글자를 커서 자리에 넣는다. */
internal enum class CalculatorKey(val label: String, val insert: String?) {
    CLEAR("C", null),
    OPEN_PARENTHESIS("(", "("),
    CLOSE_PARENTHESIS(")", ")"),
    // 글자 대신 JarvisIcons.Backspace 를 그리고, label 은 contentDescription 이다.
    BACKSPACE("한 글자 지우기", null),
    SEVEN("7", "7"),
    EIGHT("8", "8"),
    NINE("9", "9"),
    DIVIDE("÷", "÷"),
    FOUR("4", "4"),
    FIVE("5", "5"),
    SIX("6", "6"),
    MULTIPLY("×", "×"),
    ONE("1", "1"),
    TWO("2", "2"),
    THREE("3", "3"),
    SUBTRACT("−", "−"),
    PERCENT("%", "%"),
    ZERO("0", "0"),
    DECIMAL(".", "."),
    ADD("+", "+"),
    POWER("^", "^"),
    EQUALS("=", null),
    ;

    val testTag: String get() = "calculator:key:${name.lowercase()}"

    val isDigit: Boolean get() = insert?.singleOrNull()?.isDigit() == true || this == DECIMAL

    companion object {
        val Rows: List<List<CalculatorKey>> = listOf(
            listOf(CLEAR, OPEN_PARENTHESIS, CLOSE_PARENTHESIS, BACKSPACE),
            listOf(SEVEN, EIGHT, NINE, DIVIDE),
            listOf(FOUR, FIVE, SIX, MULTIPLY),
            listOf(ONE, TWO, THREE, SUBTRACT),
            listOf(PERCENT, ZERO, DECIMAL, ADD),
            listOf(POWER, EQUALS),
        )
    }
}

/** 선택한 글자가 있으면 바꾸고, 커서를 넣은 글자 뒤에 둔다. */
internal fun TextFieldState.insertAtCursor(text: String) {
    edit {
        val start = selection.min
        replace(start, selection.max, text)
        selection = TextRange(start + text.length)
    }
}

/** 선택이 있으면 선택을, 없으면 커서 앞 한 글자를 지운다. */
internal fun TextFieldState.deleteBeforeCursor() {
    edit {
        val current = selection
        val start = if (current.collapsed) (current.min - 1).coerceAtLeast(0) else current.min
        if (start == current.max) return@edit
        replace(start, current.max, "")
        selection = TextRange(start)
    }
}
