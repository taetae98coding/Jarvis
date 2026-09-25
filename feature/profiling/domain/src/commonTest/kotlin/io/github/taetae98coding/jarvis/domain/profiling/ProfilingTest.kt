package io.github.taetae98coding.jarvis.domain.profiling

import kotlin.test.Test
import kotlin.test.assertEquals

class ProfilingTest {
    @Test
    fun initialMeasuresSupportedMetricsAndMarksTheRestUnavailable() {
        val initial = Profiling.initial(setOf(ProfilingMetric.CPU, ProfilingMetric.DISK_SPACE))

        assertEquals(Reading.Measuring, initial.cpu)
        assertEquals(Reading.Unavailable, initial.memory)
        assertEquals(Reading.Unavailable, initial.gpu)
        assertEquals(Reading.Unavailable, initial.network)
        assertEquals(Reading.Unavailable, initial.diskActivity)
        assertEquals(Reading.Measuring, initial.diskSpace)
    }

    @Test
    fun initialWithEveryMetricMeasuresEverything() {
        val initial = Profiling.initial(ProfilingMetric.entries.toSet())

        assertEquals(
            List(6) { Reading.Measuring },
            listOf(initial.cpu, initial.memory, initial.gpu, initial.network, initial.diskActivity, initial.diskSpace),
        )
    }
}
