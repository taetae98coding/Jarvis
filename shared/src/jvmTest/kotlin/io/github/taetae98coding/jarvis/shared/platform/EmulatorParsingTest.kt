package io.github.taetae98coding.jarvis.shared.platform

import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds

class EmulatorParsingTest {
    @Test
    fun countsOneAvdPerNonBlankLine() {
        val output = """
            Pixel_9_API_37
            Pixel_Tablet_API_36

        """.trimIndent()

        assertEquals(2, parseAvdCount(output))
    }

    @Test
    fun countsNoAvdsWhenNothingIsInstalled() {
        assertEquals(0, parseAvdCount(""))
    }

    @Test
    fun countsOnlyEmulatorSerialsAsRunning() {
        val output = """
            List of devices attached
            emulator-5554	device
            emulator-5556	offline
            39061FDJH00CNS	device
            192.168.0.10:5555	device

        """.trimIndent()

        assertEquals(2, parseRunningEmulatorCount(output))
    }

    @Test
    fun readsSimulatorStatesFromSimctlListing() {
        val output = """
            == Devices ==
            -- iOS 27.0 --
                iPhone 18 Pro (6B65E47D-5249-4826-A1FF-58A3B10A889E) (Shutdown)
                iPhone 17 (66C9B671-6289-44B1-9788-DF9528508CD7) (Booted)
                iPad mini (A17 Pro) (F22CB96A-6FAF-4E46-A457-01FB604350A7) (Shutdown)
        """.trimIndent()

        assertEquals(EmulatorSummary(total = 3, running = 1), parseSimulatorSummary(output))
    }

    @Test
    fun readsNoSimulatorsFromAnEmptyListing() {
        assertEquals(EmulatorSummary(), parseSimulatorSummary("== Devices ==\n"))
    }

    @Test
    fun pollingProbeCountsAgainAfterEachInterval() = runTest {
        var calls = 0
        val probe = pollingEmulatorProbe(interval = 5.seconds) {
            calls += 1
            EmulatorStatus(android = EmulatorSummary(total = calls))
        }

        val totals = probe.observe().take(3).toList().map { it.android.total }

        assertEquals(listOf(1, 2, 3), totals)
        assertEquals(10_000L, testScheduler.currentTime)
    }

    @Test
    fun pollingProbeSkipsUnchangedCounts() = runTest {
        val probe = pollingEmulatorProbe(interval = 5.seconds) { EmulatorStatus() }
        val statuses = mutableListOf<EmulatorStatus>()
        backgroundScope.launch { probe.observe().toList(statuses) }

        advanceTimeBy(60.seconds)

        assertEquals(listOf(EmulatorStatus()), statuses)
    }
}
