package io.github.taetae98coding.jarvis.domain.focus

import kotlinx.coroutines.flow.Flow
import kotlin.time.Instant

/** 벽시계. 테스트가 시간을 갈아끼울 수 있게 도메인이 직접 `Clock.System` 을 부르지 않는다. */
interface FocusClock {
    fun now(): Instant

    /** 남은 시간을 다시 그릴 만큼 촘촘히 지금 시각을 흘린다. cold 다. */
    fun observeNow(): Flow<Instant>

    /** 기기 시간대에서 [instant] 가 속한 날(1970-01-01 부터 센 날 수). "오늘" 의 경계다. */
    fun localEpochDay(instant: Instant): Long
}
