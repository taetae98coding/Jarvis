package io.github.taetae98coding.jarvis.domain.calculator

import kotlin.test.Test
import kotlin.test.assertEquals

class CalculatorNumberFormatTest {
    private fun format(value: Double) = CalculatorNumberFormat.format(value)

    @Test
    fun integersKeepEveryDigit() {
        assertEquals("0", format(0.0))
        assertEquals("0", format(-0.0))
        assertEquals("42", format(42.0))
        assertEquals("-7", format(-7.0))
        assertEquals("123456789012345", format(123456789012345.0))
        assertEquals("999999999999999", format(999999999999999.0))
    }

    @Test
    fun fractionsRoundToTwelveSignificantDigits() {
        assertEquals("0.3", format(0.1 + 0.2))
        assertEquals("0.1", format(1 - 0.9))
        assertEquals("0.333333333333", format(1.0 / 3))
        assertEquals("0.666666666667", format(2.0 / 3))
        assertEquals("-0.666666666667", format(-2.0 / 3))
        assertEquals("3.14159265359", format(3.141592653589793))
        assertEquals("1234.5", format(1234.5))
        assertEquals("0.001", format(0.001))
    }

    @Test
    fun roundingCarriesIntoTheNextDigit() {
        assertEquals("10", format(9.9999999999999))
        assertEquals("1", format(0.99999999999999))
    }

    @Test
    fun smallNumbersStayPlainDownToNanoScale() {
        assertEquals("0.000000001", format(1e-9))
        assertEquals("0.00000000123", format(1.23e-9))
    }

    @Test
    fun outsidePlainRangeUsesExponent() {
        assertEquals("1e15", format(1e15))
        assertEquals("1.5e20", format(1.5e20))
        assertEquals("-2.5e16", format(-2.5e16))
        assertEquals("1e-10", format(1e-10))
        assertEquals("1.23456789012e-12", format(1.23456789012345e-12))
        assertEquals("1.79769313486e308", format(Double.MAX_VALUE))
        assertEquals("4.94065645841e-324", format(Double.MIN_VALUE))
    }

    @Test
    fun largeFractionsRoundedIntoPlainInteger() {
        // 12자리로 반올림하므로 정수가 아닌 큰 수는 끝자리가 0 이 된다.
        assertEquals("123456789012000", format(123456789012345.5))
    }
}
