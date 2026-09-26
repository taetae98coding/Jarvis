package io.github.taetae98coding.jarvis.domain.worldclock

import kotlin.time.Instant

/** 한 도시의 지금 시각과, 기기 시간대와 비교한 날·오프셋 차이. */
data class CityTime(
    val city: City,
    val time: ZonedTime,
    /** 이 도시 날짜 − 기준 날짜. 내일이면 1, 어제면 −1. */
    val dayDifference: Int,
    /** 이 도시 오프셋 − 기준 오프셋. 서울 기준 뉴욕(EDT)은 −13시간이다. */
    val offsetDifferenceSeconds: Int,
)

data class WorldClockState(
    val now: Instant,
    val local: CityTime,
    /** 놓은 순서대로다. 이 플랫폼이 모르는 시간대는 빠진다. */
    val cities: List<CityTime>,
    /** 저장된 그대로의 목록. 추가 후보에서 이미 놓은 도시를 뺄 때 쓴다. */
    val savedZoneIds: List<String>,
)

internal fun ZoneResolver.worldClockState(now: Instant, localZoneId: String, savedZoneIds: List<String>): WorldClockState {
    val localTime = toZoned(now, localZoneId) ?: toZoned(now, WorldCities.UtcZoneId)
        // UTC 도 모르는 시간대 데이터베이스는 없지만, 가짜 오프셋이 그럴 수 있어 0 으로 둔다.
        ?: ZonedTime(WorldCities.UtcZoneId, CivilDateTime.fromLocalEpochSeconds(now.epochSeconds), 0)
    val local = CityTime(WorldCities.cityOf(localTime.zoneId), localTime, dayDifference = 0, offsetDifferenceSeconds = 0)

    return WorldClockState(
        now = now,
        local = local,
        cities = savedZoneIds.mapNotNull { zoneId -> toZoned(now, zoneId)?.let { cityTime(it, localTime) } },
        savedZoneIds = savedZoneIds,
    )
}

internal fun cityTime(time: ZonedTime, reference: ZonedTime): CityTime =
    CityTime(
        city = WorldCities.cityOf(time.zoneId),
        time = time,
        dayDifference = (time.dateTime.date.epochDay - reference.dateTime.date.epochDay).toInt(),
        offsetDifferenceSeconds = time.offsetSeconds - reference.offsetSeconds,
    )
