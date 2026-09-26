package io.github.taetae98coding.jarvis.domain.worldclock

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CivilDateTest {
    @Test
    fun epochDayMatchesKnownDates() {
        assertEquals(0, CivilDate(1970, 1, 1).epochDay)
        assertEquals(11_017, CivilDate(2000, 3, 1).epochDay)
        assertEquals(-1, CivilDate(1969, 12, 31).epochDay)
        // java.time.LocalDate.of(1, 1, 1).toEpochDay()
        assertEquals(-719_162, CivilDate(1, 1, 1).epochDay)
        assertEquals(2_932_896, CivilDate(9999, 12, 31).epochDay)
    }

    @Test
    fun epochDayRoundTripsAcrossCenturiesAndBeforeYearZero() {
        var day = -1_000_000L
        while (day <= 3_000_000L) {
            val date = CivilDate.fromEpochDay(day)
            assertEquals(day, date.epochDay, date.toString())
            day += 997
        }
    }

    @Test
    fun consecutiveDaysAdvanceByOne() {
        var date = CivilDate(1899, 12, 1)
        repeat(365 * 5) {
            val next = date.plusDays(1)
            assertEquals(date.epochDay + 1, next.epochDay)
            date = next
        }
        assertEquals(CivilDate(1904, 11, 30), date)
    }

    @Test
    fun leapYearsFollowGregorianRule() {
        assertTrue(CivilDate.isLeapYear(2024))
        assertTrue(CivilDate.isLeapYear(2000))
        assertTrue(CivilDate.isLeapYear(1600))
        assertFalse(CivilDate.isLeapYear(1900))
        assertFalse(CivilDate.isLeapYear(2026))
        assertFailsWith<IllegalArgumentException> { CivilDate(1900, 2, 29) }
        assertEquals(CivilDate(2000, 3, 1), CivilDate(2000, 2, 29).plusDays(1))
    }

    @Test
    fun dayOfWeek() {
        assertEquals(DayOfWeek.THURSDAY, CivilDate(1970, 1, 1).dayOfWeek)
        assertEquals(DayOfWeek.SATURDAY, CivilDate(2026, 9, 26).dayOfWeek)
        assertEquals(DayOfWeek.MONDAY, CivilDate(2024, 1, 1).dayOfWeek)
        assertEquals(DayOfWeek.WEDNESDAY, CivilDate(1969, 12, 31).dayOfWeek)
    }

    @Test
    fun plusMonthsClampsToMonthEnd() {
        assertEquals(CivilDate(2024, 2, 29), CivilDate(2024, 1, 31).plusMonths(1))
        assertEquals(CivilDate(2023, 2, 28), CivilDate(2023, 1, 31).plusMonths(1))
        assertEquals(CivilDate(2025, 1, 15), CivilDate(2024, 11, 15).plusMonths(2))
        assertEquals(CivilDate(2023, 12, 31), CivilDate(2024, 3, 31).plusMonths(-3))
    }

    @Test
    fun parseAcceptsCommonForms() {
        val expected = CivilDate(2026, 9, 26)
        assertEquals(expected, CivilDate.parse("2026-09-26"))
        assertEquals(expected, CivilDate.parse(" 2026.9.26 "))
        assertEquals(expected, CivilDate.parse("2026. 9. 26."))
        assertEquals(expected, CivilDate.parse("2026/09/26"))
        assertEquals(expected, CivilDate.parse("20260926"))
        assertEquals("2026-09-26", expected.toString())
        assertEquals("0005-01-02", CivilDate(5, 1, 2).toString())
    }

    @Test
    fun parseRejectsImpossibleOrMalformedDates() {
        assertNull(CivilDate.parse(""))
        assertNull(CivilDate.parse("2026-02-30"))
        assertNull(CivilDate.parse("2026-13-01"))
        assertNull(CivilDate.parse("0000-01-01"))
        assertNull(CivilDate.parse("2026-09"))
        assertNull(CivilDate.parse("2026-0a-01"))
        assertNull(CivilDate.parse("12026-01-01"))
    }

    @Test
    fun timeOfDayParse() {
        assertEquals(TimeOfDay(14, 30), TimeOfDay.parse("14:30"))
        assertEquals(TimeOfDay(9, 5), TimeOfDay.parse("9:05"))
        assertEquals(TimeOfDay(14, 30), TimeOfDay.parse("1430"))
        assertEquals(TimeOfDay(14, 30, 15), TimeOfDay.parse("14:30:15"))
        assertNull(TimeOfDay.parse("24:00"))
        assertNull(TimeOfDay.parse("12:60"))
        assertNull(TimeOfDay.parse("12"))
        assertEquals("09:05", TimeOfDay(9, 5).toString())
    }

    @Test
    fun dateTimeLocalSecondsRoundTrip() {
        val dateTime = CivilDateTime(CivilDate(1969, 12, 31), 23, 59, 59)
        assertEquals(-1, dateTime.localEpochSeconds)
        assertEquals(dateTime, CivilDateTime.fromLocalEpochSeconds(-1))
        assertEquals(CivilDateTime(CivilDate(2026, 9, 26), 15, 30), CivilDateTime.fromLocalEpochSeconds(utc(2026, 9, 26, 15, 30).epochSeconds))
    }
}
