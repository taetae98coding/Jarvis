package io.github.taetae98coding.jarvis.domain.unitconverter

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class UnitNumberFormatTest {
    @Test
    fun removesFloatingNoise() {
        assertEquals("0.3", UnitNumberFormat.format(0.1 + 0.2))
        assertEquals("100", UnitNumberFormat.format(100.00000000000006))
        assertEquals("212", UnitNumberFormat.format(211.99999999999997))
    }

    @Test
    fun roundsToTwelveSignificantDigits() {
        assertEquals("1234567.89123", UnitNumberFormat.format(1234567.891234567))
        assertEquals("0.333333333333", UnitNumberFormat.format(1.0 / 3))
        assertEquals("0.000123456789012", UnitNumberFormat.format(0.000123456789012345))
        // 0.5 는 올린다. 2^40 + 0.5 는 Double 로 정확해서 짝수 쪽으로 가는 반올림과 구별된다.
        assertEquals("1099511627777", UnitNumberFormat.format(1099511627776.5))
    }

    @Test
    fun roundingCanCarryIntoANewDigit() {
        assertEquals("10", UnitNumberFormat.format(9.9999999999995))
        assertEquals("1E21", UnitNumberFormat.format(9.99999999999999e20))
    }

    @Test
    fun keepsWholeIntegerPartBelowPlainMax() {
        assertEquals("1099511627776", UnitNumberFormat.format(1099511627776.0))
        assertEquals("999999999999999", UnitNumberFormat.format(999_999_999_999_999.0))
    }

    @Test
    fun zeroAndSign() {
        assertEquals("0", UnitNumberFormat.format(0.0))
        assertEquals("0", UnitNumberFormat.format(-0.0))
        assertEquals("-2.5", UnitNumberFormat.format(-2.5))
        assertEquals("-1.5E-9", UnitNumberFormat.format(-1.5e-9))
    }

    @Test
    fun scientificOutsidePlainRange() {
        assertEquals("0.000001", UnitNumberFormat.format(0.000001))
        assertEquals("1E-7", UnitNumberFormat.format(1e-7))
        assertEquals("1E15", UnitNumberFormat.format(1e15))
        assertEquals("1.23456789012E17", UnitNumberFormat.format(123_456_789_012_345_678.0))
        assertEquals("1.13686837722E-13", UnitNumberFormat.format(1.0 / (8.0 * 1024 * 1024 * 1024 * 1024)))
    }

    @Test
    fun nonFiniteHasNoText() {
        assertNull(UnitNumberFormat.format(Double.NaN))
        assertNull(UnitNumberFormat.format(Double.POSITIVE_INFINITY))
    }

    @Test
    fun parsesPlainNumbers() {
        assertEquals(42.0, UnitNumberFormat.parse(" 42 "))
        assertEquals(1234.5, UnitNumberFormat.parse("1,234.5"))
        assertEquals(-40.0, UnitNumberFormat.parse("-40"))
        assertEquals(3.0, UnitNumberFormat.parse("+3"))
        assertEquals(0.5, UnitNumberFormat.parse(".5"))
        assertEquals(5.0, UnitNumberFormat.parse("5."))
        assertEquals(1000.0, UnitNumberFormat.parse("1e3"))
        assertEquals(0.0025, UnitNumberFormat.parse("2.5E-3"))
    }

    @Test
    fun rejectsWhatIsNotAPlainNumber() {
        listOf("", "abc", "1m", "0x1p3", "1f", "1d", "NaN", "Infinity", "1e400", ".", "1..2", "1 2", "--1").forEach {
            assertNull(UnitNumberFormat.parse(it), it)
        }
    }
}
