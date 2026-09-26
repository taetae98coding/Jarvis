package io.github.taetae98coding.jarvis.domain.worldclock

import kotlin.time.Instant

/** 한 시간대에서 본 한 순간. */
data class ZonedTime(
    val zoneId: String,
    val dateTime: CivilDateTime,
    val offsetSeconds: Int,
)

/** 벽시계 시각이 그 시간대에서 몇 번 있는지. */
enum class LocalTimeKind {
    /** 한 번. */
    UNIQUE,

    /** 서머타임이 시작돼 건너뛴 시각. 옮기기 전 오프셋으로 읽어 그만큼 뒤의 시각이 된다. */
    GAP,

    /** 서머타임이 끝나 두 번 있는 시각. 앞의 것(옮기기 전 오프셋)을 고른다. */
    OVERLAP,
}

data class ResolvedTime(
    val instant: Instant,
    val kind: LocalTimeKind,
)

/**
 * 시간대 오프셋 함수 위에서 순간 ↔ 벽시계 시각을 바꾼다. 오프셋만 알면 되므로 가짜 오프셋으로 시험할 수 있다.
 *
 * 벽시계 → 순간은 `java.time.ZonedDateTime.ofLocal` 과 같은 답을 낸다: 두 번 있으면 앞의 것, 없으면 옮기기 전
 * 오프셋으로 읽는다.
 */
class ZoneResolver(
    private val offsetSecondsAt: (zoneId: String, instant: Instant) -> Int?,
) {
    fun toZoned(instant: Instant, zoneId: String): ZonedTime? {
        val offset = offsetSecondsAt(zoneId, instant) ?: return null
        return ZonedTime(zoneId, CivilDateTime.fromLocalEpochSeconds(instant.epochSeconds + offset), offset)
    }

    fun resolve(local: CivilDateTime, zoneId: String): ResolvedTime? {
        val localSeconds = local.localEpochSeconds

        // 하루 안에 오프셋이 두 번 바뀌는 시간대는 없다. 앞뒤 하루의 오프셋이 이 시각 근처의 두 후보다.
        val before = offsetSecondsAt(zoneId, Instant.fromEpochSeconds(localSeconds - SecondsPerDay)) ?: return null
        val after = offsetSecondsAt(zoneId, Instant.fromEpochSeconds(localSeconds + SecondsPerDay)) ?: return null

        val valid = listOf(before, after).distinct().filter { offset ->
            offsetSecondsAt(zoneId, Instant.fromEpochSeconds(localSeconds - offset)) == offset
        }

        return when {
            valid.size == 2 -> ResolvedTime(Instant.fromEpochSeconds(localSeconds - before), LocalTimeKind.OVERLAP)
            valid.size == 1 -> ResolvedTime(Instant.fromEpochSeconds(localSeconds - valid.single()), LocalTimeKind.UNIQUE)
            else -> ResolvedTime(Instant.fromEpochSeconds(localSeconds - before), LocalTimeKind.GAP)
        }
    }
}
