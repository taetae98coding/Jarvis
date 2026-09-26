package io.github.taetae98coding.jarvis.domain.worldclock

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds

class WorldClockUseCasesTest {
    private val zones = FakeTimeZoneRepository()

    @Test
    fun stateShowsLocalAndSavedCitiesWithDayAndOffsetDifference() = runTest {
        // 서울은 이미 27일 00:30, 뉴욕(EDT)은 아직 26일 11:30 이다.
        val clock = FakeClockRepository(utc(2026, 9, 26, 15, 30))
        val saved = FakeSavedCitiesRepository(listOf("America/New_York", "Asia/Kathmandu", "UTC"))
        val state = ObserveWorldClockUseCase(clock, zones, saved)(backgroundScope)

        val value = state.value
        assertEquals("서울", value.local.city.name)
        assertEquals(CivilDateTime(CivilDate(2026, 9, 27), 0, 30), value.local.time.dateTime)

        val (newYork, kathmandu, utcTime) = value.cities
        assertEquals("뉴욕", newYork.city.name)
        assertEquals(CivilDateTime(CivilDate(2026, 9, 26), 11, 30), newYork.time.dateTime)
        assertEquals(-1, newYork.dayDifference)
        assertEquals(-13 * 3600, newYork.offsetDifferenceSeconds)
        assertEquals(-(3 * 3600 + 15 * 60), kathmandu.offsetDifferenceSeconds)
        assertEquals(-1, kathmandu.dayDifference)
        assertEquals(0, utcTime.time.offsetSeconds)
    }

    @Test
    fun stateFollowsClockLocalZoneAndSavedList() = runTest {
        val clock = FakeClockRepository(utc(2026, 9, 26, 15, 30))
        val saved = FakeSavedCitiesRepository(listOf("America/New_York"))
        val state = ObserveWorldClockUseCase(clock, zones, saved)(backgroundScope)

        clock.now.value += 1.seconds
        assertEquals(1, state.first { it.local.time.dateTime.second == 1 }.local.time.dateTime.second)

        zones.localZoneId.value = "America/New_York"
        val inNewYork = state.first { it.local.city.name == "뉴욕" }
        assertEquals(0, inNewYork.cities.single().dayDifference)
        assertEquals(0, inNewYork.cities.single().offsetDifferenceSeconds)

        saved.zoneIds.value = listOf("Asia/Seoul", "America/New_York")
        assertEquals(listOf("서울", "뉴욕"), state.first { it.cities.size == 2 }.cities.map { it.city.name })
    }

    @Test
    fun zonesUnknownToPlatformAreSkipped() = runTest {
        val clock = FakeClockRepository(utc(2026, 9, 26))
        val saved = FakeSavedCitiesRepository(listOf("Europe/London", "Asia/Tokyo"))

        val value = ObserveWorldClockUseCase(clock, zones, saved)(backgroundScope).value

        assertEquals(listOf("Asia/Tokyo"), value.cities.map { it.city.zoneId })
        assertEquals(listOf("Europe/London", "Asia/Tokyo"), value.savedZoneIds)
    }

    @Test
    fun unknownLocalZoneFallsBackToUtc() = runTest {
        val clock = FakeClockRepository(utc(2026, 9, 26, 1))
        val value = ObserveWorldClockUseCase(clock, FakeTimeZoneRepository("Nowhere/Land"), FakeSavedCitiesRepository())(backgroundScope).value

        assertEquals(WorldCities.UtcZoneId, value.local.city.zoneId)
        assertEquals(1, value.local.time.dateTime.hour)
    }

    @Test
    fun addAppendsOnceAndRemoveDeletes() = runTest {
        val saved = FakeSavedCitiesRepository(listOf("Asia/Tokyo"))

        AddCityUseCase(saved)("America/New_York")
        AddCityUseCase(saved)("Asia/Tokyo")
        assertEquals(listOf("Asia/Tokyo", "America/New_York"), saved.zoneIds.value)

        RemoveCityUseCase(saved)("Asia/Tokyo")
        RemoveCityUseCase(saved)("Europe/Paris")
        assertEquals(listOf("America/New_York"), saved.zoneIds.value)
    }

    @Test
    fun searchMatchesKoreanEnglishAndZoneNamesAndSkipsExcludedOrUnsupported() {
        val search = SearchCitiesUseCase(zones)

        assertEquals(listOf("America/New_York"), search("뉴욕").map { it.zoneId })
        assertEquals(listOf("America/New_York"), search("new york").map { it.zoneId })
        assertEquals(listOf("America/New_York"), search("NEW_YORK").map { it.zoneId })
        assertEquals(listOf("Asia/Seoul"), search("asia/seoul").map { it.zoneId })
        assertEquals(emptyList(), search("뉴욕", exclude = listOf("America/New_York")))
        // 가짜 데이터베이스는 런던을 모른다.
        assertEquals(emptyList(), search("런던"))
        assertEquals(listOf("Asia/Seoul", "Asia/Tokyo", "Asia/Kathmandu", "America/New_York", "UTC"), search.all().map { it.zoneId })
    }

    @Test
    fun convertsWallTimeBetweenZones() {
        val conversion = ConvertTimeUseCase(zones)(
            local = CivilDateTime(CivilDate(2026, 9, 27), 9, 0),
            fromZoneId = "Asia/Seoul",
            toZoneIds = listOf("America/New_York", "UTC", "Europe/London"),
        )!!

        assertEquals(LocalTimeKind.UNIQUE, conversion.kind)
        assertEquals(CivilDateTime(CivilDate(2026, 9, 27), 9, 0), conversion.source.dateTime)
        val (newYork, utcTime) = conversion.targets
        assertEquals(CivilDateTime(CivilDate(2026, 9, 26), 20, 0), newYork.time.dateTime)
        assertEquals(-1, newYork.dayDifference)
        assertEquals((-13).hours.inWholeSeconds.toInt(), newYork.offsetDifferenceSeconds)
        assertEquals(CivilDateTime(CivilDate(2026, 9, 27), 0, 0), utcTime.time.dateTime)
        assertEquals(2, conversion.targets.size)
    }

    @Test
    fun conversionIsDstCorrectAndReportsGaps() {
        val convert = ConvertTimeUseCase(zones)

        val winter = convert(CivilDateTime(CivilDate(2026, 1, 15), 9, 0), "America/New_York", listOf("Asia/Seoul"))!!
        assertEquals(CivilDateTime(CivilDate(2026, 1, 15), 23, 0), winter.targets.single().time.dateTime)

        val summer = convert(CivilDateTime(CivilDate(2026, 7, 15), 9, 0), "America/New_York", listOf("Asia/Seoul"))!!
        assertEquals(CivilDateTime(CivilDate(2026, 7, 15), 22, 0), summer.targets.single().time.dateTime)

        val gap = convert(CivilDateTime(CivilDate(2026, 3, 8), 2, 30), "America/New_York", listOf("UTC"))!!
        assertEquals(LocalTimeKind.GAP, gap.kind)
        assertEquals(CivilDateTime(CivilDate(2026, 3, 8), 3, 30), gap.source.dateTime)

        assertNull(convert(CivilDateTime(CivilDate(2026, 1, 1), 0, 0), "Nowhere/Land", listOf("UTC")))
    }

    @Test
    fun cityOfUnknownZoneUsesLastSegmentAndAliasesResolve() {
        assertEquals("Yangon", WorldCities.cityOf("Asia/Yangon").name)
        assertEquals("Port of Spain", WorldCities.cityOf("America/Port_of_Spain").name)
        assertEquals("뭄바이·델리", WorldCities.cityOf("Asia/Calcutta").name)
        assertEquals("협정 세계시", WorldCities.cityOf("Etc/UTC").name)
    }

    @Test
    fun curatedListHasUniqueZonesAndIncludesTheRequestedCities() {
        val zoneIds = WorldCities.all.map { it.zoneId }
        assertEquals(zoneIds.size, zoneIds.toSet().size)

        listOf(
            "Asia/Seoul", "Asia/Tokyo", "Asia/Shanghai", "Asia/Singapore", "Asia/Bangkok", "Asia/Jakarta", "Asia/Kolkata",
            "Asia/Dubai", "Europe/Moscow", "Europe/Istanbul", "Europe/Berlin", "Europe/Paris", "Europe/London",
            "America/New_York", "America/Chicago", "America/Denver", "America/Los_Angeles", "Pacific/Honolulu",
            "Australia/Sydney", "Pacific/Auckland", "America/Sao_Paulo", "America/Mexico_City", "America/Toronto",
            "America/Vancouver", "Africa/Cairo", "Africa/Johannesburg", "UTC",
        ).forEach { assertEquals(true, it in zoneIds, it) }
        WorldCities.defaultZoneIds.forEach { assertEquals(true, it in zoneIds, it) }
    }
}
