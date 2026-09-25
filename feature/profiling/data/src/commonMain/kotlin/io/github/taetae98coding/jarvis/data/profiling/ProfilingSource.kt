package io.github.taetae98coding.jarvis.data.profiling

import io.github.taetae98coding.jarvis.data.PlatformContext
import io.github.taetae98coding.jarvis.domain.profiling.Profiling
import io.github.taetae98coding.jarvis.domain.profiling.ProfilingMetric
import kotlin.time.Duration.Companion.seconds

/** 무엇을 어떤 API 로 읽는지는 각 플랫폼 스펙의 profiling 절에 있다. */
internal interface ProfilingSource {
    val supportedMetrics: Set<ProfilingMetric>

    /** 수집 하나마다 새로 연다. 속도 지표는 직전 표본과의 차이라 표본기가 그 상태를 들고 있다. */
    fun openSampler(): ProfilingSampler
}

internal fun interface ProfilingSampler {
    suspend fun sample(): Profiling
}

internal expect fun createProfilingSource(context: PlatformContext): ProfilingSource

// 지표 어느 것도 변경 알림이 없는 누적값이라 폴링한다(docs/common/profiling.html R3).
internal val ProfilingPollInterval = 1.seconds
