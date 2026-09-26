package io.github.taetae98coding.jarvis.domain.calculator

import kotlin.math.abs

/** percentagecalculator.net 의 세 가지 꼴. 입력 순서는 화면의 문장 순서(X, Y)다. */
enum class PercentMode {
    /** X 의 Y% 는? */
    PERCENT_OF,

    /** X 는 Y 의 몇 %? */
    RATIO,

    /** X 에서 Y 로 몇 % 증감? */
    CHANGE,
    ;

    val storedValue: String get() = name.lowercase()
}

enum class PercentOperand { FIRST, SECOND }

sealed interface PercentResult {
    data object Empty : PercentResult

    /** [PercentMode.CHANGE] 의 [text] 는 절댓값이다. 증가·감소는 [value] 의 부호로 가른다. */
    data class Value(val value: Double, val text: String) : PercentResult

    data class InvalidNumber(val operand: PercentOperand) : PercentResult

    data object DivideByZero : PercentResult

    data object NotFinite : PercentResult
}

object PercentCalculator {
    fun calculate(mode: PercentMode, first: String, second: String): PercentResult {
        if (first.isBlank() || second.isBlank()) return PercentResult.Empty

        val x = parseSignedDecimal(first) ?: return PercentResult.InvalidNumber(PercentOperand.FIRST)
        val y = parseSignedDecimal(second) ?: return PercentResult.InvalidNumber(PercentOperand.SECOND)

        val value = when (mode) {
            PercentMode.PERCENT_OF -> x * y / 100
            PercentMode.RATIO -> if (y == 0.0) return PercentResult.DivideByZero else x / y * 100
            // 음수에서 출발해도 "커졌으면 증가" 가 되도록 절댓값으로 나눈다.
            PercentMode.CHANGE -> if (x == 0.0) return PercentResult.DivideByZero else (y - x) / abs(x) * 100
        }
        if (!value.isFinite()) return PercentResult.NotFinite

        val shown = if (mode == PercentMode.CHANGE) abs(value) else value
        return PercentResult.Value(value, CalculatorNumberFormat.format(shown))
    }
}
