package io.github.taetae98coding.jarvis.data.profiling

import io.github.taetae98coding.jarvis.data.state.observeByPolling
import io.github.taetae98coding.jarvis.domain.profiling.Profiling
import io.github.taetae98coding.jarvis.domain.profiling.ProfilingMetric
import io.github.taetae98coding.jarvis.domain.profiling.ProfilingRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow

internal class DefaultProfilingRepository(
    private val source: ProfilingSource,
) : ProfilingRepository {
    override val supportedMetrics: Set<ProfilingMetric>
        get() = source.supportedMetrics

    override fun observeProfiling(): Flow<Profiling> = flow {
        // 표본기를 리포지토리가 한 벌만 들면 구독자 둘이 서로의 직전 표본을 가져가 간격이 어긋난다.
        val sampler = source.openSampler()
        emitAll(observeByPolling(ProfilingPollInterval, sampler::sample))
    }
}
