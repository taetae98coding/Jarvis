package io.github.taetae98coding.jarvis.data.profiling

import io.github.taetae98coding.jarvis.domain.profiling.Profiling
import io.github.taetae98coding.jarvis.domain.profiling.ProfilingMetric
import io.github.taetae98coding.jarvis.domain.profiling.Reading
import io.github.taetae98coding.jarvis.domain.profiling.Usage
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class ProfilingRepositoryTest {
    @Test
    fun readsAgainEveryPollInterval() = runTest {
        val source = CountingSource()
        val repository = DefaultProfilingRepository(source)

        val values = repository.observeProfiling().take(3).toList()

        assertEquals(listOf(1.0, 2.0, 3.0), values.map { (it.cpu as Reading.Available).value.percent })
        assertEquals(2 * ProfilingPollInterval.inWholeMilliseconds, testScheduler.currentTime)
    }

    @Test
    fun opensANewSamplerForEachCollection() = runTest {
        val source = CountingSource()
        val repository = DefaultProfilingRepository(source)

        repository.observeProfiling().take(1).toList()
        val second = repository.observeProfiling().take(1).toList()

        assertEquals(2, source.opened)
        // 표본기마다 제 카운터에서 시작한다. 앞 수집의 표본을 이어받지 않는다.
        assertEquals(1.0, (second.single().cpu as Reading.Available).value.percent)
    }

    @Test
    fun stopsReadingWhenCollectionEnds() = runTest {
        val source = CountingSource()
        val repository = DefaultProfilingRepository(source)

        val job = backgroundScope.launch { repository.observeProfiling().collect {} }
        runCurrent()
        advanceTimeBy(2.seconds + 1.seconds / 2)
        job.cancel()
        val readsWhileCollected = source.samples
        advanceTimeBy(10.seconds)

        assertEquals(3, readsWhileCollected)
        assertEquals(readsWhileCollected, source.samples)
    }

    @Test
    fun supportedMetricsComeFromTheSource() {
        val repository = DefaultProfilingRepository(CountingSource())

        assertEquals(setOf(ProfilingMetric.CPU), repository.supportedMetrics)
    }

    private class CountingSource : ProfilingSource {
        var opened = 0
        var samples = 0

        override val supportedMetrics = setOf(ProfilingMetric.CPU)

        override fun openSampler(): ProfilingSampler {
            opened++
            var count = 0
            return ProfilingSampler {
                samples++
                count++
                Profiling.initial(supportedMetrics).copy(cpu = Reading.Available(Usage(count.toDouble())))
            }
        }
    }
}
