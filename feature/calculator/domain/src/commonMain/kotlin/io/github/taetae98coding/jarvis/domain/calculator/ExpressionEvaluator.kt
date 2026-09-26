package io.github.taetae98coding.jarvis.domain.calculator

import kotlin.math.pow

/**
 * 사칙연산·거듭제곱·퍼센트·괄호 수식을 셈한다. 문법은 docs/common/calculator.html#implementation.
 *
 * 치는 동안 글자마다 부르므로 오류를 던지지 않고 [CalculationResult.Error] 로 돌려준다.
 */
object ExpressionEvaluator {
    const val MaxDepth = 100

    private const val MaxExponentDigits = 4

    fun evaluate(input: String): CalculationResult {
        if (input.isBlank()) return CalculationResult.Empty

        return try {
            val value = Parser(input).parse()
            if (value.isFinite()) {
                CalculationResult.Value(value, CalculatorNumberFormat.format(value))
            } else {
                CalculationResult.Error(ExpressionError.NotFinite)
            }
        } catch (e: ParseException) {
            CalculationResult.Error(e.error)
        }
    }

    // 깊은 재귀에서 한 번에 빠져나오려고 파서 안에서만 던진다. evaluate 밖으로 새지 않는다.
    private class ParseException(val error: ExpressionError) : Exception()

    private class Parser(private val input: String) {
        private var index = 0
        private var depth = 0

        fun parse(): Double {
            val value = expression()
            skipSpaces()
            if (index < input.length) {
                val char = input[index]
                fail(if (char == ')') ExpressionError.UnmatchedCloseParenthesis(index) else ExpressionError.UnexpectedCharacter(index, char))
            }
            return value
        }

        private fun expression(): Double {
            var value = term()
            while (true) {
                value = when (peek()) {
                    '+' -> {
                        advance()
                        value + term()
                    }

                    '-' -> {
                        advance()
                        value - term()
                    }

                    else -> return value
                }
            }
        }

        private fun term(): Double {
            var value = unary()
            while (true) {
                value = when (peek()) {
                    '*' -> {
                        advance()
                        value * unary()
                    }

                    '/' -> {
                        advance()
                        val divisor = unary()
                        if (divisor == 0.0) fail(ExpressionError.DivideByZero)
                        value / divisor
                    }

                    // 숫자·닫는 괄호 바로 뒤의 여는 괄호는 곱셈이다: 2(3+4).
                    '(' -> value * unary()

                    else -> return value
                }
            }
        }

        private fun unary(): Double =
            when (peek()) {
                '-' -> {
                    advance()
                    nested { -unary() }
                }

                '+' -> {
                    advance()
                    nested { unary() }
                }

                else -> power()
            }

        private fun power(): Double {
            val base = postfix()
            if (peek() != '^') return base
            advance()
            // 지수를 unary 로 읽어 오른쪽 결합(2^3^2 = 2^9)과 음수 지수(2^-1)를 함께 받는다.
            val exponent = nested { unary() }
            if (base == 0.0 && exponent < 0) fail(ExpressionError.DivideByZero)
            return base.pow(exponent)
        }

        private fun postfix(): Double {
            var value = primary()
            while (peek() == '%') {
                advance()
                value /= 100
            }
            return value
        }

        private fun primary(): Double {
            skipSpaces()
            if (index >= input.length) fail(ExpressionError.Incomplete)

            val char = input[index]
            return when {
                char == '(' -> {
                    advance()
                    val value = nested { expression() }
                    when {
                        peek() == ')' -> advance()
                        index >= input.length -> fail(ExpressionError.MissingCloseParenthesis)
                        else -> fail(ExpressionError.UnexpectedCharacter(index, input[index]))
                    }
                    value
                }

                char.isDecimalDigit() || char == '.' -> number()
                char == ')' -> fail(ExpressionError.UnmatchedCloseParenthesis(index))
                else -> fail(ExpressionError.UnexpectedCharacter(index, char))
            }
        }

        private fun number(): Double {
            val start = index
            while (index < input.length && (input[index].isDecimalDigit() || input[index] == '.')) index++
            val mantissa = parseDecimal(input.substring(start, index)) ?: fail(ExpressionError.InvalidNumber(start))
            val exponent = exponent() ?: return mantissa
            // 10^-n 은 2진수로 정확하지 않아 곱하지 않고 10^n 으로 나눈다.
            return if (exponent >= 0) mantissa * 10.0.pow(exponent) else mantissa / 10.0.pow(-exponent)
        }

        /**
         * `=` 이 입력 칸에 넣는 답(`1.5e20`)을 다시 셈할 수 있도록 `e±n` 을 읽는다. 뒤에 숫자가 없으면
         * 지수가 아니므로 읽지 않고 null 이다. 자릿수가 많아도 Double 범위를 넘으므로 네 자리에서 자른다.
         */
        private fun exponent(): Int? {
            if (index >= input.length || (input[index] != 'e' && input[index] != 'E')) return null

            var cursor = index + 1
            val negative = input.getOrNull(cursor) == '-' || input.getOrNull(cursor) == '−'
            if (negative || input.getOrNull(cursor) == '+') cursor++
            val digitsStart = cursor
            while (cursor < input.length && input[cursor].isDecimalDigit()) cursor++
            if (cursor == digitsStart) return null

            index = cursor
            val magnitude = input.substring(digitsStart, cursor).trimStart('0').take(MaxExponentDigits).ifEmpty { "0" }.toInt()
            return if (negative) -magnitude else magnitude
        }

        private inline fun nested(block: () -> Double): Double {
            if (++depth > MaxDepth) fail(ExpressionError.TooDeep)
            return block().also { depth-- }
        }

        /** 공백을 건너뛴 다음 글자. 연산자는 ASCII 로 바꿔 보인다. 입력 끝이면 null. */
        private fun peek(): Char? {
            skipSpaces()
            return input.getOrNull(index)?.let(::normalizeOperator)
        }

        private fun advance() {
            index++
        }

        private fun skipSpaces() {
            while (index < input.length && input[index].isWhitespace()) index++
        }

        private fun fail(error: ExpressionError): Nothing = throw ParseException(error)
    }

    private fun normalizeOperator(char: Char): Char =
        when (char) {
            '×' -> '*'
            '÷' -> '/'
            '−' -> '-'
            else -> char
        }
}
