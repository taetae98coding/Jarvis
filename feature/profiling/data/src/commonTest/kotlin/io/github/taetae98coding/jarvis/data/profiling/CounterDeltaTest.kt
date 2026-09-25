package io.github.taetae98coding.jarvis.data.profiling

import io.github.taetae98coding.jarvis.domain.profiling.Reading
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TestTimeSource

class CounterDeltaTest {
    private val time = TestTimeSource()
    private val delta = CounterDelta(time)

    @Test
    fun firstSampleHasNothingToCompare() {
        assertNull(delta.next(longArrayOf(100)))
    }

    @Test
    fun laterSamplesGiveDeltaAndElapsedTime() {
        delta.next(longArrayOf(1_000, 50))
        time += 2.seconds

        val sample = delta.next(longArrayOf(5_000, 250))!!

        assertEquals(listOf(4_000L, 200L), sample.deltas.toList())
        assertEquals(2.seconds, sample.elapsed)
        assertEquals(2_000, sample.perSecond(0))
        assertEquals(100, sample.perSecond(1))
    }

    @Test
    fun counterThatWentBackwardsCountsAsZero() {
        delta.next(longArrayOf(4_000_000_000))
        time += 1.seconds

        assertEquals(0, delta.next(longArrayOf(10))!!.deltas.single())
    }

    @Test
    fun percentOfElapsedIsClampedToOneHundred() {
        delta.next(longArrayOf(0, 0))
        time += 1.seconds

        val sample = delta.next(longArrayOf(250.milliseconds.inWholeNanoseconds, 3.seconds.inWholeNanoseconds))!!

        assertEquals(25.0, sample.percentOfElapsed(0))
        assertEquals(100.0, sample.percentOfElapsed(1))
    }

    @Test
    fun percentOfDividesCountersByEachOther() {
        delta.next(longArrayOf(0, 0, 0))
        time += 1.seconds

        val sample = delta.next(longArrayOf(10, 30, 60))!!

        assertEquals(40.0, sample.percentOf(part = intArrayOf(0, 1), whole = intArrayOf(0, 1, 2)))
    }

    @Test
    fun readSeparatesUnavailableMeasuringAndAvailable() {
        assertEquals(Reading.Unavailable, delta.read(null) { it.deltas[0] })
        assertEquals(Reading.Measuring, delta.read(longArrayOf(1)) { it.deltas[0] })
        time += 1.seconds
        assertEquals(Reading.Available(4L), delta.read(longArrayOf(5)) { it.deltas[0] })
    }
}
