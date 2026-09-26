package io.github.taetae98coding.jarvis.domain.calculator

import kotlin.test.Test
import kotlin.test.assertEquals

class PercentCalculatorTest {
    private fun calculate(mode: PercentMode, first: String, second: String) = PercentCalculator.calculate(mode, first, second)

    @Test
    fun percentOf() {
        assertEquals(PercentResult.Value(30.0, "30"), calculate(PercentMode.PERCENT_OF, "200", "15"))
        assertEquals(PercentResult.Value(-2.5, "-2.5"), calculate(PercentMode.PERCENT_OF, "-50", "5"))
        assertEquals("0.3", (calculate(PercentMode.PERCENT_OF, "3", "10") as PercentResult.Value).text)
    }

    @Test
    fun ratio() {
        assertEquals(PercentResult.Value(25.0, "25"), calculate(PercentMode.RATIO, "50", "200"))
        assertEquals("33.3333333333", (calculate(PercentMode.RATIO, "1", "3") as PercentResult.Value).text)
        assertEquals(PercentResult.DivideByZero, calculate(PercentMode.RATIO, "5", "0"))
    }

    @Test
    fun changeTextIsMagnitudeAndValueCarriesDirection() {
        assertEquals(PercentResult.Value(25.0, "25"), calculate(PercentMode.CHANGE, "80", "100"))
        assertEquals(PercentResult.Value(-20.0, "20"), calculate(PercentMode.CHANGE, "100", "80"))
        assertEquals(PercentResult.Value(0.0, "0"), calculate(PercentMode.CHANGE, "7", "7"))
        // 음수에서 커지면 증가다.
        assertEquals(PercentResult.Value(50.0, "50"), calculate(PercentMode.CHANGE, "-100", "-50"))
        assertEquals(PercentResult.DivideByZero, calculate(PercentMode.CHANGE, "0", "10"))
    }

    @Test
    fun blankFieldGivesNoAnswer() {
        PercentMode.entries.forEach { mode ->
            assertEquals(PercentResult.Empty, calculate(mode, "", "5"))
            assertEquals(PercentResult.Empty, calculate(mode, "5", " "))
        }
    }

    @Test
    fun invalidFieldIsNamed() {
        assertEquals(PercentResult.InvalidNumber(PercentOperand.FIRST), calculate(PercentMode.PERCENT_OF, "1,000", "5"))
        assertEquals(PercentResult.InvalidNumber(PercentOperand.SECOND), calculate(PercentMode.PERCENT_OF, "1000", "5%"))
        assertEquals(PercentResult.InvalidNumber(PercentOperand.FIRST), calculate(PercentMode.RATIO, "1.2.3", "5"))
        assertEquals(PercentResult.InvalidNumber(PercentOperand.SECOND), calculate(PercentMode.CHANGE, "5", "-"))
    }

    @Test
    fun acceptsSignsDecimalsAndSpaces() {
        assertEquals(PercentResult.Value(0.5, "0.5"), calculate(PercentMode.PERCENT_OF, " .5 ", "100"))
        assertEquals(PercentResult.Value(-1.0, "-1"), calculate(PercentMode.PERCENT_OF, "−10", "10"))
        assertEquals(PercentResult.Value(1.0, "1"), calculate(PercentMode.PERCENT_OF, "+10", "10"))
    }

    @Test
    fun overflowIsNotFinite() {
        val huge = "1" + "0".repeat(308)
        assertEquals(PercentResult.NotFinite, calculate(PercentMode.PERCENT_OF, huge, huge))
    }
}
