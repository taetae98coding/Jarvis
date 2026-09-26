package io.github.taetae98coding.jarvis.domain.worldclock

import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.time.Instant

internal fun utc(year: Int, month: Int, day: Int, hour: Int = 0, minute: Int = 0): Instant =
    Instant.fromEpochSeconds(CivilDateTime(CivilDate(year, month, day), hour, minute).localEpochSeconds)

/**
 * 몇 개 시간대만 아는 고정 데이터베이스. 뉴욕은 2025·2026년 서머타임 전환 시각만 넣었다
 * (3월 둘째 일요일 02:00 EST = 07:00Z, 11월 첫째 일요일 02:00 EDT = 06:00Z).
 */
internal class FakeTimeZoneRepository(
    localZoneId: String = "Asia/Seoul",
) : TimeZoneRepository {
    val localZoneId = MutableStateFlow(localZoneId)

    private val newYorkSummer = listOf(
        utc(2025, 3, 9, 7)..utc(2025, 11, 2, 6),
        utc(2026, 3, 8, 7)..utc(2026, 11, 1, 6),
    )

    override fun offsetSecondsAt(zoneId: String, instant: Instant): Int? = when (zoneId) {
        "UTC" -> 0
        "Asia/Seoul", "Asia/Tokyo" -> 9 * 3600
        "Asia/Kathmandu" -> 5 * 3600 + 45 * 60
        "America/New_York" -> if (newYorkSummer.any { instant in it && instant != it.endInclusive }) -4 * 3600 else -5 * 3600
        else -> null
    }

    override fun observeLocalZoneId() = localZoneId

    override fun readLocalZoneId() = localZoneId.value
}

internal class FakeClockRepository(start: Instant) : ClockRepository {
    val now = MutableStateFlow(start)

    override fun observeNow() = now

    override fun readNow() = now.value
}

internal class FakeSavedCitiesRepository(initial: List<String> = emptyList()) : SavedCitiesRepository {
    val zoneIds = MutableStateFlow(initial)

    override fun observeSavedZoneIds() = zoneIds

    override fun readSavedZoneIds() = zoneIds.value

    override fun setSavedZoneIds(zoneIds: List<String>) {
        this.zoneIds.value = zoneIds
    }
}
