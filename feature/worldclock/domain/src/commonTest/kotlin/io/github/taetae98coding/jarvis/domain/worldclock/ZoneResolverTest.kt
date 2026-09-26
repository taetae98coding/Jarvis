package io.github.taetae98coding.jarvis.domain.worldclock

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ZoneResolverTest {
    private val zones = FakeTimeZoneRepository()
    private val resolver = ZoneResolver(zones::offsetSecondsAt)

    @Test
    fun instantToZonedAppliesOffset() {
        val seoul = resolver.toZoned(utc(2026, 9, 26, 15, 30), "Asia/Seoul")!!
        assertEquals(CivilDateTime(CivilDate(2026, 9, 27), 0, 30), seoul.dateTime)
        assertEquals(9 * 3600, seoul.offsetSeconds)

        val kathmandu = resolver.toZoned(utc(2026, 9, 26, 15, 30), "Asia/Kathmandu")!!
        assertEquals(CivilDateTime(CivilDate(2026, 9, 26), 21, 15), kathmandu.dateTime)

        assertNull(resolver.toZoned(utc(2026, 9, 26), "Mars/Olympus_Mons"))
    }

    @Test
    fun summerAndWinterOffsetsDiffer() {
        assertEquals(-5 * 3600, resolver.toZoned(utc(2026, 1, 15, 12), "America/New_York")!!.offsetSeconds)
        assertEquals(-4 * 3600, resolver.toZoned(utc(2026, 7, 15, 12), "America/New_York")!!.offsetSeconds)
    }

    @Test
    fun ordinaryLocalTimeIsUnique() {
        val resolved = resolver.resolve(CivilDateTime(CivilDate(2025, 1, 15), 12, 0), "America/New_York")

        assertEquals(ResolvedTime(utc(2025, 1, 15, 17), LocalTimeKind.UNIQUE), resolved)
    }

    @Test
    fun skippedLocalTimeMovesForwardByTheGap() {
        // 2025-03-09 02:00 EST 에 03:00 EDT 로 건너뛴다. 02:30 은 없고, java.time 처럼 03:30 EDT 가 된다.
        val resolved = resolver.resolve(CivilDateTime(CivilDate(2025, 3, 9), 2, 30), "America/New_York")!!

        assertEquals(ResolvedTime(utc(2025, 3, 9, 7, 30), LocalTimeKind.GAP), resolved)
        assertEquals(CivilDateTime(CivilDate(2025, 3, 9), 3, 30), resolver.toZoned(resolved.instant, "America/New_York")!!.dateTime)
    }

    @Test
    fun repeatedLocalTimePicksTheEarlierInstant() {
        // 2025-11-02 02:00 EDT 에 01:00 EST 로 돌아간다. 01:30 이 두 번 있고 앞의 것(EDT)을 고른다.
        val resolved = resolver.resolve(CivilDateTime(CivilDate(2025, 11, 2), 1, 30), "America/New_York")

        assertEquals(ResolvedTime(utc(2025, 11, 2, 5, 30), LocalTimeKind.OVERLAP), resolved)
    }

    @Test
    fun unknownZoneCannotBeResolved() {
        assertNull(resolver.resolve(CivilDateTime(CivilDate(2025, 1, 1), 0, 0), "Nowhere/Land"))
    }
}
