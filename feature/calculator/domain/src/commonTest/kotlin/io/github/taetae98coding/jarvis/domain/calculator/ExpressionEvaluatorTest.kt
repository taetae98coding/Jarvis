package io.github.taetae98coding.jarvis.domain.calculator

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ExpressionEvaluatorTest {
    private fun text(input: String): String {
        val result = ExpressionEvaluator.evaluate(input)
        check(result is CalculationResult.Value) { "$input → $result" }
        return result.text
    }

    private fun error(input: String): ExpressionError {
        val result = ExpressionEvaluator.evaluate(input)
        check(result is CalculationResult.Error) { "$input → $result" }
        return result.error
    }

    @Test
    fun blankIsEmpty() {
        assertEquals(CalculationResult.Empty, ExpressionEvaluator.evaluate(""))
        assertEquals(CalculationResult.Empty, ExpressionEvaluator.evaluate("   "))
    }

    @Test
    fun precedence() {
        assertEquals("7", text("1+2*3"))
        assertEquals("9", text("(1+2)*3"))
        assertEquals("2", text("8/2/2"))
        assertEquals("-1", text("1-2"))
        assertEquals("4", text("10-3-3"))
        assertEquals("14", text("2+3*4"))
    }

    @Test
    fun unaryMinusBindsLooserThanPower() {
        assertEquals("-4", text("-2^2"))
        assertEquals("4", text("(-2)^2"))
        assertEquals("-5", text("-(2+3)"))
        assertEquals("1", text("--1"))
        assertEquals("3", text("1--2"))
        assertEquals("-6", text("2*-3"))
        assertEquals("3", text("+3"))
    }

    @Test
    fun powerIsRightAssociativeAndTakesNegativeExponent() {
        assertEquals("512", text("2^3^2"))
        assertEquals("0.5", text("2^-1"))
        assertEquals("1024", text("2^10"))
        assertEquals("3", text("9^0.5"))
    }

    @Test
    fun percentDividesByHundred() {
        assertEquals("0.5", text("50%"))
        assertEquals("50.1", text("50+10%"))
        assertEquals("20", text("200*10%"))
        assertEquals("0.01", text("100%%"))
        // % 가 ^ 보다 먼저 붙는다.
        assertEquals("0.25", text("50%^2"))
    }

    @Test
    fun implicitMultiplicationBeforeParenthesis() {
        assertEquals("14", text("2(3+4)"))
        assertEquals("21", text("(1+2)(3+4)"))
        assertEquals("-14", text("-2(3+4)"))
    }

    @Test
    fun unicodeOperatorsAndSpaces() {
        assertEquals("42", text("6 × 7"))
        assertEquals("2.5", text("5 ÷ 2"))
        assertEquals("-1", text("2 − 3"))
        assertEquals("30", text(" 12 × ( 3 − 0.5 ) "))
    }

    @Test
    fun decimalsWithoutLeadingOrTrailingDigits() {
        assertEquals("1", text(".5+.5"))
        assertEquals("5", text("5."))
    }

    @Test
    fun exponentNotationRoundTripsFormattedAnswers() {
        assertEquals("1.5e20", text("1.5e20"))
        assertEquals("3e20", text("1.5e20*2"))
        assertEquals("1e-10", text("1E-10"))
        assertEquals("0.0015", text("1.5e−3"))
        assertEquals("200", text("2e+2"))
        assertEquals(ExpressionError.UnexpectedCharacter(1, 'e'), error("2e"))
        assertEquals(ExpressionError.UnexpectedCharacter(1, 'e'), error("2e+"))
        assertEquals(ExpressionError.NotFinite, error("1e99999"))
        assertEquals("0", text("1e-99999"))

        listOf("2^70", "1/7", "-1/3e12", "10^-12").forEach { input ->
            val shown = text(input)
            assertEquals(shown, text(shown), input)
        }
    }

    @Test
    fun floatingNoiseIsHidden() {
        assertEquals("0.3", text("0.1+0.2"))
        assertEquals("0.1", text("1-0.9"))
        assertEquals("3.3", text("1.1*3"))
        assertEquals("0.333333333333", text("1/3"))
    }

    @Test
    fun valueKeepsFullPrecision() {
        val result = ExpressionEvaluator.evaluate("1/3") as CalculationResult.Value
        assertEquals(1.0 / 3, result.value)
    }

    @Test
    fun incompleteExpressions() {
        assertEquals(ExpressionError.Incomplete, error("3+"))
        assertEquals(ExpressionError.Incomplete, error("-"))
        assertEquals(ExpressionError.Incomplete, error("2^"))
        assertEquals(ExpressionError.MissingCloseParenthesis, error("(1+2"))
        assertEquals(ExpressionError.MissingCloseParenthesis, error("2*(3"))
        assertTrue(error("3+").isIncomplete)
        assertTrue(error("(1").isIncomplete)
    }

    @Test
    fun malformedExpressionsPointAtTheCharacter() {
        assertEquals(ExpressionError.UnmatchedCloseParenthesis(3), error("1+2)"))
        assertEquals(ExpressionError.UnmatchedCloseParenthesis(0), error(")"))
        assertEquals(ExpressionError.UnexpectedCharacter(1, 'a'), error("2a"))
        assertEquals(ExpressionError.UnexpectedCharacter(2, '*'), error("1+*2"))
        assertEquals(ExpressionError.UnexpectedCharacter(3, '3'), error("(2)3"))
        assertEquals(ExpressionError.InvalidNumber(0), error("1.2.3"))
        assertEquals(ExpressionError.InvalidNumber(2), error("1+."))
        assertFalse(error("2a").isIncomplete)
    }

    @Test
    fun divideByZero() {
        assertEquals(ExpressionError.DivideByZero, error("1/0"))
        assertEquals(ExpressionError.DivideByZero, error("5 ÷ (2-2)"))
        assertEquals(ExpressionError.DivideByZero, error("0^-1"))
        assertEquals("0", text("0/5"))
    }

    @Test
    fun nonFiniteResults() {
        assertEquals(ExpressionError.NotFinite, error("10^400"))
        assertEquals(ExpressionError.NotFinite, error("(-8)^0.5"))
    }

    @Test
    fun nestingLimitStopsBeforeStackOverflow() {
        val ok = "(".repeat(ExpressionEvaluator.MaxDepth) + "1" + ")".repeat(ExpressionEvaluator.MaxDepth)
        assertEquals("1", text(ok))

        val tooDeep = "(".repeat(10_000) + "1" + ")".repeat(10_000)
        assertEquals(ExpressionError.TooDeep, error(tooDeep))
        assertEquals(ExpressionError.TooDeep, error("-".repeat(10_000) + "1"))
    }
}
