package io.github.taetae98coding.jarvis.domain.calculator

sealed interface CalculationResult {
    data object Empty : CalculationResult

    /** [text] 는 [CalculatorNumberFormat] 으로 적은 [value]. */
    data class Value(val value: Double, val text: String) : CalculationResult

    data class Error(val error: ExpressionError) : CalculationResult
}

/** `index` 는 입력 문자열에서 0 부터 센 위치다. */
sealed interface ExpressionError {
    /** 아직 치는 중이라 생기는 오류. 화면은 오류 색 대신 흐린 글자로 보인다. */
    val isIncomplete: Boolean get() = false

    data object Incomplete : ExpressionError {
        override val isIncomplete: Boolean get() = true
    }

    data object MissingCloseParenthesis : ExpressionError {
        override val isIncomplete: Boolean get() = true
    }

    data class UnmatchedCloseParenthesis(val index: Int) : ExpressionError

    data class UnexpectedCharacter(val index: Int, val char: Char) : ExpressionError

    data class InvalidNumber(val index: Int) : ExpressionError

    data object DivideByZero : ExpressionError

    /** 무한대나 NaN. `10^400`, `(-8)^0.5`. */
    data object NotFinite : ExpressionError

    data object TooDeep : ExpressionError
}
