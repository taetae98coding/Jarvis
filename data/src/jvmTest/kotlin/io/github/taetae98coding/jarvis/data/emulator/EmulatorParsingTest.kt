package io.github.taetae98coding.jarvis.data.emulator

import io.github.taetae98coding.jarvis.domain.emulator.EmulatorDevice
import io.github.taetae98coding.jarvis.domain.emulator.EmulatorPlatform
import io.github.taetae98coding.jarvis.domain.emulator.EmulatorSummary
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

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
    fun readsAvdNamesOnePerNonBlankLine() {
        val output = """
            Pixel_9_API_37
            Pixel_Tablet_API_36

        """.trimIndent()

        assertEquals(listOf("Pixel_9_API_37", "Pixel_Tablet_API_36"), parseAvdNames(output))
    }

    // `offline` 인 에뮬레이터는 아직 붙는 중이라 화면을 찍을 수 없다.
    @Test
    fun readsOnlyAttachedEmulatorSerials() {
        val output = """
            List of devices attached
            emulator-5554	device
            emulator-5556	offline
            39061FDJH00CNS	device

        """.trimIndent()

        assertEquals(listOf("emulator-5554"), parseEmulatorSerials(output))
    }

    // `adb emu avd name` 은 이름 뒤에 콘솔 응답 `OK` 를 붙인다.
    @Test
    fun readsAvdNameFromTheEmulatorConsole() {
        assertEquals("Pixel_9_API_37", parseAvdName("Pixel_9_API_37\nOK\n"))
    }

    @Test
    fun readsNoAvdNameFromAnEmptyConsoleAnswer() {
        assertNull(parseAvdName("OK\n"))
    }

    @Test
    fun readsSimulatorsWithNamesAndIdentifiers() {
        val output = """
            == Devices ==
            -- iOS 27.0 --
                iPhone 17 (66C9B671-6289-44B1-9788-DF9528508CD7) (Booted)
                iPad mini (A17 Pro) (F22CB96A-6FAF-4E46-A457-01FB604350A7) (Shutdown)
        """.trimIndent()

        assertEquals(
            listOf(
                EmulatorDevice(
                    id = "66C9B671-6289-44B1-9788-DF9528508CD7",
                    name = "iPhone 17",
                    platform = EmulatorPlatform.IOS,
                    isRunning = true,
                ),
                EmulatorDevice(
                    id = "F22CB96A-6FAF-4E46-A457-01FB604350A7",
                    // 괄호를 품은 기기 이름이 UDID 와 섞이면 안 된다.
                    name = "iPad mini (A17 Pro)",
                    platform = EmulatorPlatform.IOS,
                ),
            ),
            parseSimulatorDevices(output),
        )
    }

    // 시뮬레이터는 실행 중이어도 제스처를 받지 못한다. simctl 에 입력 주입 명령이 없다.
    @Test
    fun simulatorsAreNeverControllable() {
        val output = "    iPhone 17 (66C9B671-6289-44B1-9788-DF9528508CD7) (Booted)"

        assertFalse(parseSimulatorDevices(output).single().canControl)
    }
}
