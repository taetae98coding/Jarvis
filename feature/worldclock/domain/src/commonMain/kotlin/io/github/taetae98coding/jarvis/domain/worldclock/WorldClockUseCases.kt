package io.github.taetae98coding.jarvis.domain.worldclock

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlin.time.Instant

class ObserveWorldClockUseCase(
    private val clock: ClockRepository,
    private val zones: TimeZoneRepository,
    private val saved: SavedCitiesRepository,
) {
    private val resolver = ZoneResolver(zones::offsetSecondsAt)

    operator fun invoke(scope: CoroutineScope): StateFlow<WorldClockState> =
        combine(clock.observeNow(), zones.observeLocalZoneId(), saved.observeSavedZoneIds(), resolver::worldClockState)
            .stateIn(
                scope,
                SharingStarted.WhileSubscribed(),
                resolver.worldClockState(clock.readNow(), zones.readLocalZoneId(), saved.readSavedZoneIds()),
            )
}

class AddCityUseCase(
    private val saved: SavedCitiesRepository,
) {
    /** 이미 있으면 그대로 둔다. 새 도시는 맨 뒤에 붙는다. */
    suspend operator fun invoke(zoneId: String) {
        val current = saved.observeSavedZoneIds().first()
        if (zoneId in current) return

        saved.setSavedZoneIds(current + zoneId)
    }
}

class RemoveCityUseCase(
    private val saved: SavedCitiesRepository,
) {
    suspend operator fun invoke(zoneId: String) {
        val current = saved.observeSavedZoneIds().first()
        if (zoneId !in current) return

        saved.setSavedZoneIds(current - zoneId)
    }
}

class SearchCitiesUseCase(
    private val zones: TimeZoneRepository,
) {
    /** [exclude] 에 있는 도시와 이 플랫폼의 시간대 데이터베이스가 모르는 도시는 뺀다. */
    operator fun invoke(query: String, exclude: Collection<String> = emptyList()): List<City> =
        WorldCities.search(query).filter { it.zoneId !in exclude && isSupported(it.zoneId) }

    /** 고를 수 있는 모든 도시. 시간대 변환의 두 목록이다. */
    fun all(): List<City> = WorldCities.all.filter { isSupported(it.zoneId) }

    // 시간대를 아는지는 순간과 무관하다. 아무 순간이나 물어도 된다.
    private fun isSupported(zoneId: String): Boolean = zones.offsetSecondsAt(zoneId, Instant.fromEpochSeconds(0)) != null
}

data class ConvertedTime(
    val city: City,
    val time: ZonedTime,
    /** 이 도시 날짜 − 원래 시간대의 날짜. */
    val dayDifference: Int,
    val offsetDifferenceSeconds: Int,
)

data class TimeConversion(
    /** 원래 시간대에서 읽은 시각. [kind] 가 [LocalTimeKind.GAP] 이면 넣은 시각보다 서머타임 폭만큼 뒤다. */
    val source: ZonedTime,
    val kind: LocalTimeKind,
    val targets: List<ConvertedTime>,
)

class ConvertTimeUseCase(
    zones: TimeZoneRepository,
) {
    private val resolver = ZoneResolver(zones::offsetSecondsAt)

    /** [instant] 를 [zoneId] 에서 본 시각. "지금" 버튼이 입력 칸을 채울 때 쓴다. */
    fun at(instant: Instant, zoneId: String): ZonedTime? = resolver.toZoned(instant, zoneId)

    /** [fromZoneId] 의 벽시계 [local] 을 [toZoneIds] 각각에서 본다. 모르는 시간대는 빠지고, 원래 시간대를 모르면 null. */
    operator fun invoke(local: CivilDateTime, fromZoneId: String, toZoneIds: List<String>): TimeConversion? {
        val resolved = resolver.resolve(local, fromZoneId) ?: return null
        val source = resolver.toZoned(resolved.instant, fromZoneId) ?: return null

        return TimeConversion(
            source = source,
            kind = resolved.kind,
            targets = toZoneIds.mapNotNull { zoneId ->
                resolver.toZoned(resolved.instant, zoneId)?.let { target ->
                    val city = cityTime(target, source)
                    ConvertedTime(city.city, target, city.dayDifference, city.offsetDifferenceSeconds)
                }
            },
        )
    }
}

class CalculateDatesUseCase {
    fun difference(from: CivilDate, to: CivilDate): DateDifference = DateCalculator.difference(from, to)

    fun addDays(date: CivilDate, days: Long): CivilDate = DateCalculator.addDays(date, days)

    fun internationalAge(birth: CivilDate, today: CivilDate): InternationalAge? = DateCalculator.internationalAge(birth, today)

    fun daysUntil(target: CivilDate, today: CivilDate): Long = DateCalculator.daysUntil(target, today)
}
