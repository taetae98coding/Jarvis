package io.github.taetae98coding.jarvis.shared.platform

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class HostAgentTest {
    @Test
    fun countsSurviveTheWireFormat() {
        val status = EmulatorStatus(
            android = EmulatorSummary(total = 5, running = 2),
            // 셀 수 없는 종류는 0개가 아니라 없음으로 전달되어야 한다.
            ios = null,
        )

        assertEquals(status, decodeEmulatorStatus(encodeEmulatorStatus(status)))
    }

    @Test
    fun brokenResponsesAreNotCounts() {
        assertNull(decodeEmulatorStatus("<html>Proxy Error</html>"))
    }

    @Test
    fun newerAgentFieldsAreIgnored() {
        val body = """{"android":{"total":1,"running":1},"web":{"total":9}}"""

        assertEquals(EmulatorStatus(android = EmulatorSummary(total = 1, running = 1)), decodeEmulatorStatus(body))
    }

    @Test
    fun missingAgentMeansUncountable() = runTest {
        val statuses = hostAgentEmulatorProbe { null }.observe().take(1).toList()

        assertEquals(listOf(EmulatorStatus()), statuses)
    }

    @Test
    fun probeFollowsAgentUpdates() = runTest {
        var answer: EmulatorStatus? = null
        val statuses = mutableListOf<EmulatorStatus>()
        backgroundScope.launch { hostAgentEmulatorProbe { answer }.observe().toList(statuses) }
        runCurrent()

        answer = EmulatorStatus(android = EmulatorSummary(total = 3, running = 1))
        advanceTimeBy(6.seconds)

        assertEquals(
            listOf(EmulatorStatus(), EmulatorStatus(android = EmulatorSummary(total = 3, running = 1))),
            statuses,
        )
    }
}
