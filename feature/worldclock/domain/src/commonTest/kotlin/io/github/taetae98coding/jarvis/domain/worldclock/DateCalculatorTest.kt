package io.github.taetae98coding.jarvis.domain.worldclock

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DateCalculatorTest {
    @Test
    fun differenceInDaysWeeksAndCalendarUnits() {
        val difference = DateCalculator.difference(CivilDate(2000, 1, 1), CivilDate(2026, 9, 26))

        assertEquals(9_765, difference.totalDays)
        assertEquals(1_395, difference.weeks)
        assertEquals(0, difference.weekDays)
        assertEquals(26, difference.years)
        assertEquals(8, difference.months)
        assertEquals(25, difference.days)
    }

    @Test
    fun monthEndBorrowsLikeJavaPeriod() {
        // Period.between(2024-01-31, 2024-03-01) = P1M1D. 1월 31일 + 1개월은 2월 29일로 당겨진다.
        val leap = DateCalculator.difference(CivilDate(2024, 1, 31), CivilDate(2024, 3, 1))
        assertEquals(30, leap.totalDays)
        assertEquals(DateDifference(30, 4, 2, 0, 1, 1), leap)

        // Period.between(2023-01-31, 2023-02-28) = P28D.
        assertEquals(DateDifference(28, 4, 0, 0, 0, 28), DateCalculator.difference(CivilDate(2023, 1, 31), CivilDate(2023, 2, 28)))

        // Period.between(2004-02-29, 2005-02-28) = P11M30D.
        assertEquals(DateDifference(365, 52, 1, 0, 11, 30), DateCalculator.difference(CivilDate(2004, 2, 29), CivilDate(2005, 2, 28)))
    }

    @Test
    fun reversedOrderKeepsMagnitudeAndFlipsSign() {
        val forward = DateCalculator.difference(CivilDate(2025, 12, 25), CivilDate(2026, 9, 26))
        val backward = DateCalculator.difference(CivilDate(2026, 9, 26), CivilDate(2025, 12, 25))

        assertEquals(-forward.totalDays, backward.totalDays)
        assertTrue(backward.isNegative)
        assertEquals(forward.copy(totalDays = backward.totalDays), backward)
        assertEquals(DateDifference(0, 0, 0, 0, 0, 0), DateCalculator.difference(CivilDate(2026, 9, 26), CivilDate(2026, 9, 26)))
    }

    @Test
    fun addDaysCrossesYearsAndGoesBackwards() {
        assertEquals(CivilDate(2027, 1, 4), DateCalculator.addDays(CivilDate(2026, 9, 26), 100))
        assertEquals(CivilDate(2026, 8, 27), DateCalculator.addDays(CivilDate(2026, 9, 26), -30))
        assertEquals(CivilDate(2024, 2, 29), DateCalculator.addDays(CivilDate(2024, 2, 28), 1))
    }

    @Test
    fun internationalAgeGrowsOnBirthday() {
        val birth = CivilDate(2000, 9, 27)

        assertEquals(InternationalAge(25, CivilDate(2026, 9, 27), 1), DateCalculator.internationalAge(birth, CivilDate(2026, 9, 26)))
        assertEquals(InternationalAge(26, CivilDate(2027, 9, 27), 365), DateCalculator.internationalAge(birth, CivilDate(2026, 9, 27)))
        assertEquals(InternationalAge(0, CivilDate(2001, 9, 27), 365), DateCalculator.internationalAge(birth, birth))
    }

    @Test
    fun leapDayBirthdayGrowsOnMarchFirstInCommonYears() {
        val birth = CivilDate(2004, 2, 29)

        assertEquals(0, DateCalculator.internationalAge(birth, CivilDate(2005, 2, 28))?.years)
        assertEquals(CivilDate(2005, 3, 1), DateCalculator.internationalAge(birth, CivilDate(2005, 2, 28))?.nextBirthday)
        assertEquals(1, DateCalculator.internationalAge(birth, CivilDate(2005, 3, 1))?.years)
        assertEquals(3, DateCalculator.internationalAge(birth, CivilDate(2008, 2, 28))?.years)
        assertEquals(4, DateCalculator.internationalAge(birth, CivilDate(2008, 2, 29))?.years)
        assertEquals(CivilDate(2009, 3, 1), DateCalculator.internationalAge(birth, CivilDate(2008, 2, 29))?.nextBirthday)
    }

    @Test
    fun ageOfFutureBirthIsUnknown() {
        assertNull(DateCalculator.internationalAge(CivilDate(2026, 9, 27), CivilDate(2026, 9, 26)))
    }

    @Test
    fun daysUntilIsSignedDDay() {
        val today = CivilDate(2026, 9, 26)

        assertEquals(90, DateCalculator.daysUntil(CivilDate(2026, 12, 25), today))
        assertEquals(0, DateCalculator.daysUntil(today, today))
        assertEquals(-26, DateCalculator.daysUntil(CivilDate(2026, 8, 31), today))
    }
}
