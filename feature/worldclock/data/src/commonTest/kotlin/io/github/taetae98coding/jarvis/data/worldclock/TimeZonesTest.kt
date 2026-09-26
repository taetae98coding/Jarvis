package io.github.taetae98coding.jarvis.data.worldclock

import io.github.taetae98coding.jarvis.domain.worldclock.CivilDate
import io.github.taetae98coding.jarvis.domain.worldclock.CivilDateTime
import io.github.taetae98coding.jarvis.domain.worldclock.WorldCities
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock

/** 각 타깃의 진짜 시간대 데이터베이스를 부른다. 같은 답이 나와야 네 플랫폼의 세계 시계가 같다. */
class TimeZonesTest {
    @Test
    fun fixedOffsetZones() {
        assertEquals(9 * 3600, zoneOffsetSeconds("Asia/Seoul", utc(2026, 1, 15)))
        assertEquals(9 * 3600, zoneOffsetSeconds("Asia/Seoul", utc(2026, 7, 15)))
        assertEquals(0, zoneOffsetSeconds("UTC", utc(2026, 7, 15)))
        assertEquals(5 * 3600 + 30 * 60, zoneOffsetSeconds("Asia/Kolkata", utc(2026, 7, 15)))
        assertEquals(5 * 3600 + 45 * 60, zoneOffsetSeconds("Asia/Kathmandu", utc(2026, 7, 15)))
    }

    @Test
    fun daylightSavingTimeInBothHemispheres() {
        assertEquals(-5 * 3600, zoneOffsetSeconds("America/New_York", utc(2026, 1, 15)))
        assertEquals(-4 * 3600, zoneOffsetSeconds("America/New_York", utc(2026, 7, 15)))
        assertEquals(0, zoneOffsetSeconds("Europe/London", utc(2026, 1, 15)))
        assertEquals(3600, zoneOffsetSeconds("Europe/London", utc(2026, 7, 15)))
        assertEquals(11 * 3600, zoneOffsetSeconds("Australia/Sydney", utc(2026, 1, 15)))
        assertEquals(10 * 3600, zoneOffsetSeconds("Australia/Sydney", utc(2026, 7, 15)))
    }

    @Test
    fun transitionHappensAtTheRuleInstant() {
        // 2026-03-08 02:00 EST(07:00Z)에 EDT 로 바뀐다.
        assertEquals(-5 * 3600, zoneOffsetSeconds("America/New_York", utc(2026, 3, 8, 6, 59)))
        assertEquals(-4 * 3600, zoneOffsetSeconds("America/New_York", utc(2026, 3, 8, 7, 0)))
    }

    @Test
    fun unknownZoneIsNull() {
        assertNull(zoneOffsetSeconds("Mars/Olympus_Mons", utc(2026, 1, 1)))
    }

    @Test
    fun everyCuratedCityIsKnown() {
        WorldCities.all.forEach { city ->
            assertNotNull(zoneOffsetSeconds(city.zoneId, utc(2026, 1, 1)), city.zoneId)
        }
    }

    @Test
    fun systemZoneIsResolvable() {
        val zoneId = systemZoneId()

        assertTrue(zoneId.isNotBlank())
        assertNotNull(zoneOffsetSeconds(zoneId, Clock.System.now().epochSeconds), zoneId)
    }

    private fun utc(year: Int, month: Int, day: Int, hour: Int = 12, minute: Int = 0): Long =
        CivilDateTime(CivilDate(year, month, day), hour, minute).localEpochSeconds
}
