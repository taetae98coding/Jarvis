package io.github.taetae98coding.jarvis.domain.calculator

import kotlin.test.Test
import kotlin.test.assertEquals

class BmiCalculatorTest {
    private fun value(height: String, weight: String): BmiResult.Value {
        val result = BmiCalculator.calculate(height, weight)
        check(result is BmiResult.Value) { "$height, $weight → $result" }
        return result
    }

    @Test
    fun computesAndRoundsToOneDecimal() {
        val result = value("175", "70")

        assertEquals("22.9", result.text)
        assertEquals(BmiCategory.NORMAL, result.category)
        assertEquals(70 / (1.75 * 1.75), result.bmi)
    }

    @Test
    fun wholeNumberBmiStillShowsOneDecimal() {
        assertEquals("25.0", value("100", "25").text)
        assertEquals(BmiCategory.OBESE_1, value("100", "25").category)
    }

    @Test
    fun categoryBoundariesFollowKoreanCriteria() {
        assertEquals(BmiCategory.UNDERWEIGHT, BmiCalculator.categoryOf(184))
        assertEquals(BmiCategory.NORMAL, BmiCalculator.categoryOf(185))
        assertEquals(BmiCategory.NORMAL, BmiCalculator.categoryOf(229))
        assertEquals(BmiCategory.PRE_OBESE, BmiCalculator.categoryOf(230))
        assertEquals(BmiCategory.PRE_OBESE, BmiCalculator.categoryOf(249))
        assertEquals(BmiCategory.OBESE_1, BmiCalculator.categoryOf(250))
        assertEquals(BmiCategory.OBESE_1, BmiCalculator.categoryOf(299))
        assertEquals(BmiCategory.OBESE_2, BmiCalculator.categoryOf(300))
        assertEquals(BmiCategory.OBESE_2, BmiCalculator.categoryOf(349))
        assertEquals(BmiCategory.OBESE_3, BmiCalculator.categoryOf(350))
    }

    @Test
    fun categoryUsesTheRoundedValueShownOnScreen() {
        // 100cm 에서 22.96kg → BMI 22.96 → 23.0 으로 보이므로 비만 전단계다.
        val result = value("100", "22.96")

        assertEquals("23.0", result.text)
        assertEquals(BmiCategory.PRE_OBESE, result.category)

        assertEquals(BmiCategory.UNDERWEIGHT, value("100", "18.44").category)
        assertEquals(BmiCategory.NORMAL, value("100", "18.46").category)
    }

    @Test
    fun normalWeightRangeForHeight() {
        val result = value("170", "60")

        assertEquals("53.5", result.normalWeightMinText)
        assertEquals("66.2", result.normalWeightMaxText)
    }

    @Test
    fun blankGivesNoAnswer() {
        assertEquals(BmiResult.Empty, BmiCalculator.calculate("", "70"))
        assertEquals(BmiResult.Empty, BmiCalculator.calculate("170", ""))
    }

    @Test
    fun outOfRangeOrNotNumber() {
        assertEquals(BmiResult.InvalidHeight, BmiCalculator.calculate("29.9", "70"))
        assertEquals(BmiResult.InvalidHeight, BmiCalculator.calculate("301", "70"))
        assertEquals(BmiResult.InvalidHeight, BmiCalculator.calculate("abc", "70"))
        assertEquals(BmiResult.InvalidHeight, BmiCalculator.calculate("-170", "70"))
        assertEquals(BmiResult.InvalidWeight, BmiCalculator.calculate("170", "0.5"))
        assertEquals(BmiResult.InvalidWeight, BmiCalculator.calculate("170", "501"))
        assertEquals(BmiResult.InvalidWeight, BmiCalculator.calculate("170", "7O"))
        assertEquals(BmiCategory.OBESE_3, value("150", "90").category)
    }
}
