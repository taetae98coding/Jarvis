package io.github.taetae98coding.jarvis.data.emulator.agent

import io.github.taetae98coding.jarvis.data.emulator.DeviceLogDataSource
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class DeviceLogSessionsTest {
    private class FakeLogs : DeviceLogDataSource {
        val flows = HashMap<String, MutableSharedFlow<List<String>>>()

        override fun observeLog(deviceId: String): Flow<List<String>> = flows.getOrPut(deviceId) { MutableSharedFlow() }

        fun subscribers(deviceId: String): Int = flows[deviceId]?.subscriptionCount?.value ?: 0
    }

    @Test
    fun givesOnlyLinesAfterTheCursor() = runTest {
        val logs = FakeLogs()
        val sessions = DeviceLogSessions(logs, backgroundScope)

        assertEquals(DeviceLogChunk(next = -1, lines = emptyList()), sessions.read("a", after = -1))
        runCurrent()
        logs.flows.getValue("a").emit(listOf("1", "2"))

        val first = sessions.read("a", after = -1)
        assertEquals(listOf("1", "2"), first.lines)

        logs.flows.getValue("a").emit(listOf("3"))
        assertEquals(listOf("3"), sessions.read("a", after = first.next).lines)
    }

    @Test
    fun keepsOnlyTheNewestLines() = runTest {
        val logs = FakeLogs()
        val sessions = DeviceLogSessions(logs, backgroundScope, capacity = 3)

        sessions.read("a", after = -1)
        runCurrent()
        logs.flows.getValue("a").emit(listOf("1", "2", "3", "4", "5"))

        assertEquals(listOf("3", "4", "5"), sessions.read("a", after = -1).lines)
    }

    @Test
    fun stopsReadingWhenNobodyAsks() = runTest {
        var now = 0L
        val logs = FakeLogs()
        val sessions = DeviceLogSessions(logs, backgroundScope, idleTimeout = 10.seconds, clock = { now })

        sessions.read("a", after = -1)
        runCurrent()
        logs.flows.getValue("a").emit(listOf("1"))
        val cursor = sessions.read("a", after = -1).next

        now = 5_000
        sessions.sweep()
        runCurrent()
        assertEquals(1, logs.subscribers("a"))

        now = 16_000
        sessions.sweep()
        runCurrent()
        assertEquals(0, logs.subscribers("a"))

        // 다시 물으면 새로 읽는다. 순번은 이어져서 옛 커서가 새 줄을 가리지 않는다.
        sessions.read("a", after = cursor)
        runCurrent()
        logs.flows.getValue("a").emit(listOf("2"))
        assertEquals(listOf("2"), sessions.read("a", after = cursor).lines)
    }
}
