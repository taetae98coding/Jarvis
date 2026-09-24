package io.github.taetae98coding.jarvis.data.emulator

import io.github.taetae98coding.jarvis.domain.emulator.DeviceConnection
import io.github.taetae98coding.jarvis.domain.emulator.EmulatorDevice
import io.github.taetae98coding.jarvis.domain.emulator.EmulatorPlatform
import io.github.taetae98coding.jarvis.domain.emulator.EmulatorSummary
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

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
                    canStream = true,
                ),
                EmulatorDevice(
                    id = "F22CB96A-6FAF-4E46-A457-01FB604350A7",
                    // 괄호를 품은 기기 이름이 UDID 와 섞이면 안 된다.
                    name = "iPad mini (A17 Pro)",
                    platform = EmulatorPlatform.IOS,
                    canLaunch = true,
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

    // 꺼진 시뮬레이터는 켤 수 있고, 켜진 시뮬레이터는 화면을 볼 수 있다.
    @Test
    fun simulatorsCanBeLaunchedOnlyWhileTheyAreOff() {
        val output = """
                iPhone 17 (66C9B671-6289-44B1-9788-DF9528508CD7) (Booted)
                iPhone 18 Pro (6B65E47D-5249-4826-A1FF-58A3B10A889E) (Shutdown)
        """.trimIndent()

        val (booted, shutdown) = parseSimulatorDevices(output)

        assertTrue(booted.canStream)
        assertFalse(booted.canLaunch)
        assertFalse(shutdown.canStream)
        assertTrue(shutdown.canLaunch)
    }

    // 에뮬레이터가 아닌 시리얼은 전부 실물 기기다. `adb connect` 로 붙은 것도 포함한다.
    @Test
    fun readsPhysicalSerialsFromAdb() {
        val output = """
            List of devices attached
            emulator-5554	device
            39061FDJH00CNS	device
            192.168.0.10:5555	device

        """.trimIndent()

        assertEquals(listOf("39061FDJH00CNS", "192.168.0.10:5555"), parsePhysicalSerials(output))
    }

    // 무선 디버깅으로 붙은 기기의 시리얼은 mDNS 이름이라 공백이 들어갈 수 있다. 공백으로 줄을
    // 자르면 상태 칸을 잘못 짚어 그 기기가 통째로 사라진다.
    @Test
    fun readsSerialsThatContainSpaces() {
        val output = """
            List of devices attached
            adb-R3CN80M6LJB-uagcG5._adb-tls-connect._tcp	device
            adb-R54T202XEHN-Y2yH0N (2)._adb-tls-connect._tcp	device

        """.trimIndent()

        assertEquals(
            listOf(
                "adb-R3CN80M6LJB-uagcG5._adb-tls-connect._tcp",
                "adb-R54T202XEHN-Y2yH0N (2)._adb-tls-connect._tcp",
            ),
            parsePhysicalSerials(output),
        )
    }

    // USB 디버깅을 허용하기 전의 기기는 화면을 찍을 수 없다.
    @Test
    fun skipsDevicesThatAreNotReady() {
        val output = """
            List of devices attached
            39061FDJH00CNS	unauthorized
            HT7A1A002042	offline

        """.trimIndent()

        assertEquals(emptyList(), parsePhysicalSerials(output))
    }

    @Test
    fun readsTheDeviceModelFromGetprop() {
        assertEquals("Pixel 9 Pro", parseDeviceModel("Pixel 9 Pro\n"))
    }

    // 속성이 없는 기기에서는 빈 줄이 온다. 이름이 비면 시리얼로 되돌려야 한다.
    @Test
    fun readsNoDeviceModelFromAnEmptyAnswer() {
        assertNull(parseDeviceModel("\n"))
    }

    @Test
    fun readsTheScreenState() {
        assertEquals(true, parseScreenOn("true\n"))
        assertEquals(false, parseScreenOn("false\n"))
    }

    // 모르는 것을 "꺼짐" 으로 표시하면 깨울 수도 없는 기기에 버튼이 붙는다.
    @Test
    fun readsNoScreenStateFromAnUnexpectedAnswer() {
        assertNull(parseScreenOn("Unknown command: get"))
    }

    // 괄호 묶음이 하나뿐인 첫 줄은 이 Mac 자신이고, `== Simulators ==` 아래는 시뮬레이터다.
    @Test
    fun readsPhysicalIosDevicesFromXctrace() {
        val output = """
            == Devices ==
            My Mac (00006000-000C4D8A0288401E)
            Jarvis의 iPhone (18.5) (00008130-000A1C2E0298001C)
            == Simulators ==
            iPhone 17 Simulator (26.0) (66C9B671-6289-44B1-9788-DF9528508CD7)
        """.trimIndent()

        assertEquals(
            listOf(
                EmulatorDevice(
                    id = "ios:00008130-000A1C2E0298001C",
                    name = "Jarvis의 iPhone",
                    platform = EmulatorPlatform.IOS,
                    isPhysical = true,
                    isRunning = true,
                ),
            ),
            parsePhysicalIosDevices(output),
        )
    }

    // 실물 iOS 기기는 연결되어 있어도 화면을 찍는 공개 도구가 없다.
    @Test
    fun physicalIosDevicesHaveNoScreen() {
        val output = "== Devices ==\nJarvis의 iPhone (18.5) (00008130-000A1C2E0298001C)\n"

        assertFalse(parsePhysicalIosDevices(output).single().canStream)
    }

    @Test
    fun readsNoPhysicalIosDevicesWhenNothingIsConnected() {
        val output = """
            == Devices ==
            My Mac (00006000-000C4D8A0288401E)
            == Simulators ==
        """.trimIndent()

        assertEquals(emptyList(), parsePhysicalIosDevices(output))
    }

    // Xcode 27 에서 페어링만 되어 있고 연결이 끊긴 기기는 `== Devices Offline ==` 로 옮겨 간다.
    @Test
    fun skipsPairedButOfflineIosDevices() {
        val output = """
            == Devices ==
            My Mac Studio (5C961F1B-D09E-502B-8FA3-92D28BCDE120)

            == Devices Offline ==
            Jarvis의 iPad (26.6.1) (00008142-000630323422401C)
            Jarvis의 iPhone (26.6.1) (00008140-000161CA0C98801C)

            == Simulators ==
            iPad (A16) Simulator (27.0) (11DB025F-4D05-430A-A304-C4EF718E72A5)
        """.trimIndent()

        assertEquals(emptyList(), parsePhysicalIosDevices(output))
    }

    @Test
    fun usbSerialsAreWired() {
        assertEquals(DeviceConnection.WIRED, physicalConnection("39061FDJH00CNS"))
        assertEquals(DeviceConnection.WIRED, physicalConnection("R3CY705Y62R"))
    }

    @Test
    fun networkSerialsAreWireless() {
        // adb connect 로 붙은 기기.
        assertEquals(DeviceConnection.WIRELESS, physicalConnection("192.168.0.10:5555"))
        // 무선 디버깅으로 붙은 기기의 mDNS 시리얼(공백을 품는다).
        assertEquals(DeviceConnection.WIRELESS, physicalConnection("adb-R54T202XEHN-Y2yH0N (2)._adb-tls-connect._tcp"))
    }

    @Test
    fun devicectlTransportTypeBecomesConnection() {
        val json = """
            {"result":{"devices":[
              {"connectionProperties":{"transportType":"wired","tunnelState":"connected"},
               "hardwareProperties":{"udid":"00008130-000A1C2E0298001C","platform":"iOS"}},
              {"connectionProperties":{"transportType":"localNetwork"},
               "hardwareProperties":{"udid":"00008120-001122334455001E"}}
            ]}}
        """.trimIndent()

        assertEquals(
            mapOf(
                "00008130-000A1C2E0298001C" to DeviceConnection.WIRED,
                "00008120-001122334455001E" to DeviceConnection.WIRELESS,
            ),
            parseIosConnections(json),
        )
    }

    @Test
    fun brokenDevicectlJsonIsNoConnections() {
        assertEquals(emptyMap(), parseIosConnections("not json"))
        assertEquals(emptyMap(), parseIosConnections("""{"result":{}}"""))
    }

}
