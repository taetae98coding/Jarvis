package io.github.taetae98coding.jarvis.data.state

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class ObserveSystemStateTest {
    @Test
    fun pollingReadsAgainAfterEachInterval() = runTest {
        var reads = 0

        val values = observeByPolling(interval = 5.seconds) { ++reads }.take(3).toList()

        assertEquals(listOf(1, 2, 3), values)
        assertEquals(10_000L, testScheduler.currentTime)
    }

    @Test
    fun pollingSkipsUnchangedValues() = runTest {
        val values = mutableListOf<Int>()
        backgroundScope.launch { observeByPolling(interval = 5.seconds) { 7 }.toList(values) }

        advanceTimeBy(1.minutes)

        assertEquals(listOf(7), values)
    }

    @Test
    fun signalsDriveRereads() = runTest {
        val signals = signalFlow()
        var value = 1
        val values = mutableListOf<Int>()
        backgroundScope.launch { observeOnSignals(signals) { value }.toList(values) }
        runCurrent()

        value = 2
        signals.emit(Unit)
        runCurrent()

        assertEquals(listOf(1, 2), values)
    }

    @Test
    fun signalsReplacePolling() = runTest {
        val values = mutableListOf<Int>()
        var value = 1
        backgroundScope.launch { observeOnSignals(signalFlow()) { value }.toList(values) }
        runCurrent()

        // 신호 기반 조회는 시간이 지나도 다시 읽지 않는다. 폴링이 함께 돌면 콜백을 주는 플랫폼에서
        // 불필요한 조회가 생긴다.
        value = 2
        advanceTimeBy(1.minutes)

        assertEquals(listOf(1), values)
    }

    @Test
    fun systemStateFollowsSignalsWhenTheyExist() = runTest {
        val signals = signalFlow()
        var value = 1
        val values = mutableListOf<Int>()
        backgroundScope.launch {
            observeSystemState(signals = signals, interval = 5.seconds) { value }.toList(values)
        }
        runCurrent()

        value = 2
        signals.emit(Unit)
        runCurrent()

        assertEquals(listOf(1, 2), values)
    }

    @Test
    fun systemStateFallsBackToPollingWithoutSignals() = runTest {
        var value = 1
        val values = mutableListOf<Int>()
        backgroundScope.launch {
            observeSystemState(signals = null, interval = 5.seconds) { value }.toList(values)
        }
        runCurrent()

        value = 2
        advanceTimeBy(6.seconds)

        assertEquals(listOf(1, 2), values)
    }

    private fun signalFlow() =
        MutableSharedFlow<Unit>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
}
