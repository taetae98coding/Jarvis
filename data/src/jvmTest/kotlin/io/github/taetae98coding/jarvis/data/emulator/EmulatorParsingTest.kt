package io.github.taetae98coding.jarvis.data.emulator

import io.github.taetae98coding.jarvis.domain.emulator.EmulatorSummary
import kotlin.test.Test
import kotlin.test.assertEquals

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
}
