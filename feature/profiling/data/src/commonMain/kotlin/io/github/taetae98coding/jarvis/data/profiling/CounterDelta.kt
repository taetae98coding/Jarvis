package io.github.taetae98coding.jarvis.data.profiling

import io.github.taetae98coding.jarvis.domain.profiling.Reading
import kotlin.time.ComparableTimeMark
import kotlin.time.Duration
import kotlin.time.TimeSource

/** 누적 카운터(바이트, CPU 틱, 디스크 사용 시간)를 직전 표본과의 차이로 바꾼다. */
internal class CounterDelta(
    private val timeSource: TimeSource.WithComparableMarks = TimeSource.Monotonic,
) {
    private var previous: LongArray? = null
    private var previousMark: ComparableTimeMark? = null

    /** 첫 표본은 비교할 것이 없어 null 이다. */
    fun next(counters: LongArray): Sample? {
        val mark = timeSource.markNow()
        val last = previous
        val lastMark = previousMark
        previous = counters
        previousMark = mark

        if (last == null || lastMark == null || last.size != counters.size) return null

        // 카운터가 줄어드는 것은 재부팅·인터페이스 교체·32비트 넘침(iOS if_data)이다. 그 표본은 0 으로 둔다.
        return Sample(LongArray(counters.size) { (counters[it] - last[it]).coerceAtLeast(0) }, mark - lastMark)
    }

    class Sample(
        val deltas: LongArray,
        val elapsed: Duration,
    ) {
        fun perSecond(index: Int): Long {
            val nanos = elapsed.inWholeNanoseconds
            return if (nanos <= 0) 0 else (deltas[index].toDouble() * NanosPerSecond / nanos).toLong()
        }

        /** 카운터가 나노초일 때, 경과 시간 중 그 카운터가 차지한 비율. */
        fun percentOfElapsed(index: Int): Double = percent(deltas[index].toDouble(), elapsed.inWholeNanoseconds.toDouble())

        /** 카운터 [part] 가 카운터들 [whole] 의 합에서 차지한 비율. CPU 틱처럼 시간 대신 카운터끼리 나눌 때 쓴다. */
        fun percentOf(part: IntArray, whole: IntArray): Double =
            percent(part.sumOf { deltas[it].toDouble() }, whole.sumOf { deltas[it].toDouble() })
    }

    private companion object {
        const val NanosPerSecond = 1_000_000_000.0
    }
}

/** [counters] 를 읽지 못했으면 측정 불가, 첫 표본이면 측정 중이다. */
internal inline fun <T> CounterDelta.read(counters: LongArray?, value: (CounterDelta.Sample) -> T): Reading<T> {
    counters ?: return Reading.Unavailable
    val sample = next(counters) ?: return Reading.Measuring
    return Reading.Available(value(sample))
}

internal fun <T : Any> T?.toReading(): Reading<T> = if (this == null) Reading.Unavailable else Reading.Available(this)

// 요청이 겹치거나 디스크가 여럿이면 100 을 넘을 수 있어 자른다(docs/common/profiling.html#limits).
internal fun percent(part: Double, whole: Double): Double =
    if (whole <= 0.0) 0.0 else (part / whole * 100).coerceIn(0.0, 100.0)
