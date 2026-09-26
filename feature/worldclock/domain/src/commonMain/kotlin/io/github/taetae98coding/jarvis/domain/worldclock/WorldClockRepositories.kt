package io.github.taetae98coding.jarvis.domain.worldclock

import kotlinx.coroutines.flow.Flow
import kotlin.time.Instant

/** 벽시계. 테스트가 시간을 갈아끼울 수 있게 도메인이 직접 `Clock.System` 을 부르지 않는다. */
interface ClockRepository {
    /** 초가 바뀔 때마다 초 단위로 자른 지금 시각을 흘린다. cold 다. */
    fun observeNow(): Flow<Instant>

    fun readNow(): Instant
}

/**
 * 플랫폼의 시간대 데이터베이스.
 *
 * [offsetSecondsAt] 은 상태가 아니다. 같은 시간대·같은 순간이면 늘 같은 값이라(데이터베이스는 앱이 도는 동안
 * 바뀌지 않는다) Flow 가 아닌 함수다.
 */
interface TimeZoneRepository {
    /** [instant] 에 [zoneId] 가 UTC 보다 몇 초 앞서는지. 서머타임을 반영한다. 이 플랫폼이 모르는 시간대면 null. */
    fun offsetSecondsAt(zoneId: String, instant: Instant): Int?

    /** 기기 시간대의 IANA 이름. 사용자가 시스템 설정에서 바꾸면 따라간다. cold 다. */
    fun observeLocalZoneId(): Flow<String>

    fun readLocalZoneId(): String
}

/** 세계 시계에 놓은 도시의 시간대 이름. 놓은 순서대로다. */
interface SavedCitiesRepository {
    fun observeSavedZoneIds(): Flow<List<String>>

    fun readSavedZoneIds(): List<String>

    fun setSavedZoneIds(zoneIds: List<String>)
}
