package io.github.taetae98coding.jarvis.ui.worldclock

import io.github.taetae98coding.jarvis.domain.worldclock.CivilDate
import io.github.taetae98coding.jarvis.domain.worldclock.CivilDateTime
import io.github.taetae98coding.jarvis.domain.worldclock.DateDifference
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class WorldClockFormatTest {
    @Test
    fun utcOffset() {
        assertEquals("UTC+9", utcOffsetLabel(9 * 3600))
        assertEquals("UTC+5:45", utcOffsetLabel(5 * 3600 + 45 * 60))
        assertEquals("UTC−3:30", utcOffsetLabel(-(3 * 3600 + 30 * 60)))
        assertEquals("UTC+0", utcOffsetLabel(0))
    }

    @Test
    fun offsetDifference() {
        assertEquals("+13시간", offsetDifferenceLabel(13 * 3600))
        assertEquals("−3시간 15분", offsetDifferenceLabel(-(3 * 3600 + 15 * 60)))
        assertEquals("+30분", offsetDifferenceLabel(30 * 60))
        assertEquals("같은 시각", offsetDifferenceLabel(0))
    }

    @Test
    fun dayDifferenceAndDDay() {
        assertEquals("+1일", dayDifferenceLabel(1))
        assertEquals("−1일", dayDifferenceLabel(-1))
        assertNull(dayDifferenceLabel(0))
        assertEquals("D-90", dDayLabel(90))
        assertEquals("D-Day", dDayLabel(0))
        assertEquals("D+26", dDayLabel(-26))
    }

    @Test
    fun datesAndClock() {
        val dateTime = CivilDateTime(CivilDate(2026, 9, 27), 9, 5, 7)
        assertEquals("09:05:07", dateTime.clockText(withSeconds = true))
        assertEquals("09:05", dateTime.clockText(withSeconds = false))
        assertEquals("9월 27일 (일)", dateTime.date.shortLabel())
        assertEquals("2026-09-27 (일)", dateTime.date.isoLabel())
    }

    @Test
    fun differenceLinesAndGrouping() {
        assertEquals(listOf("9,765일", "1,395주 0일", "26년 8개월 25일"), DateDifference(9_765, 1_395, 0, 26, 8, 25).lines())
        assertEquals(listOf("−30일", "−4주 2일", "−0년 1개월 1일"), DateDifference(-30, 4, 2, 0, 1, 1).lines())
        assertEquals("1,000,000", 1_000_000L.grouped())
        assertEquals("999", 999L.grouped())
    }
}
