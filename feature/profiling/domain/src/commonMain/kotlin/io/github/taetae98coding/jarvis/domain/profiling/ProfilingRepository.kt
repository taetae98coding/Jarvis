package io.github.taetae98coding.jarvis.domain.profiling

import kotlinx.coroutines.flow.Flow

/**
 * `observeProfiling` 은 cold 라서 수집하는 동안에만 표본을 읽는다. 표본 사이의 상태(직전 카운터)는
 * 수집마다 따로 가진다.
 */
interface ProfilingRepository {
    /** 이 플랫폼이 줄 수 있는 지표. 실행 중에 바뀌지 않는다. */
    val supportedMetrics: Set<ProfilingMetric>

    fun observeProfiling(): Flow<Profiling>
}
