package io.github.taetae98coding.jarvis.data.profiling

import io.github.taetae98coding.jarvis.data.PlatformContext
import io.github.taetae98coding.jarvis.domain.profiling.Reading
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

/** 이 머신의 실제 명령을 부른다. 값은 머신마다 달라 범위만 본다. */
class MacProfilingSourceTest {
    @Test
    fun secondSampleHasEveryMetricOnMacOs() = runBlocking {
        if (!System.getProperty("os.name").startsWith("Mac")) return@runBlocking

        val sampler = createProfilingSource(PlatformContext()).openSampler()
        val first = sampler.sample()
        assertEquals(Reading.Measuring, first.cpu)
        assertEquals(Reading.Measuring, first.network)
        assertEquals(Reading.Measuring, first.diskActivity)

        delay(200)
        val second = sampler.sample()

        assertIs<Reading.Available<*>>(second.cpu)
        assertIs<Reading.Available<*>>(second.memory)
        assertIs<Reading.Available<*>>(second.gpu)
        assertIs<Reading.Available<*>>(second.network)
        assertIs<Reading.Available<*>>(second.diskActivity)
        assertIs<Reading.Available<*>>(second.diskSpace)
    }
}
