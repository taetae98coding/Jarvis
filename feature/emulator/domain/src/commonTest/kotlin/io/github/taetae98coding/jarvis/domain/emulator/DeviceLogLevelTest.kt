package io.github.taetae98coding.jarvis.domain.emulator

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DeviceLogLevelTest {
    @Test
    fun readsTheLogcatThreadtimeLevel() {
        assertEquals(DeviceLogLevel.Info, parseDeviceLogLevel("09-25 17:18:51.832  1850  2652 I thermal_core: pivot(AP) : 326"))
        assertEquals(DeviceLogLevel.Debug, parseDeviceLogLevel("09-25 17:18:51.468  1129  1129 D io_stats: !@ Write_top(KB)"))
        assertEquals(DeviceLogLevel.Warn, parseDeviceLogLevel("09-25 17:18:51.468 11129 21129 W ActivityManager: slow"))
        assertEquals(DeviceLogLevel.Error, parseDeviceLogLevel("09-25 17:18:51.468   129   129 E AndroidRuntime: FATAL EXCEPTION"))
        assertEquals(DeviceLogLevel.Fatal, parseDeviceLogLevel("09-25 17:18:51.468   129   129 A libc: abort"))
        assertEquals(DeviceLogLevel.Verbose, parseDeviceLogLevel("09-25 17:18:51.468   129   129 V Tag: v"))
    }

    @Test
    fun readsTheIosCompactLevel() {
        assertEquals(DeviceLogLevel.Info, parseDeviceLogLevel("2026-09-25 17:18:51.832 Df SpringBoard[53:1a2] message"))
        assertEquals(DeviceLogLevel.Debug, parseDeviceLogLevel("2026-09-25 17:18:51.832 Db backboardd[60:2b1] message"))
        assertEquals(DeviceLogLevel.Error, parseDeviceLogLevel("2026-09-25 17:18:51.832 E  locationd[70:3c] message"))
        assertEquals(DeviceLogLevel.Fatal, parseDeviceLogLevel("2026-09-25 17:18:51.832 Fa runningboardd[33:1] fault"))
    }

    @Test
    fun headersHaveNoLevel() {
        assertEquals(DeviceLogLevel.Unknown, parseDeviceLogLevel("--------- beginning of main"))
        assertEquals(DeviceLogLevel.Unknown, parseDeviceLogLevel("Filtering the log data using \"composedMessage CONTAINS x\""))
        // 메시지 안의 " E " 를 수준으로 읽지 않는다.
        assertEquals(DeviceLogLevel.Unknown, parseDeviceLogLevel("at com.example E Foo"))
    }

    @Test
    fun onlyPhysicalIosCannotReadLogs() {
        assertFalse(EmulatorDevice("ios:1", "iPhone", EmulatorPlatform.IOS, isPhysical = true).canReadLog)
        assertTrue(EmulatorDevice("A-B", "iPhone 17", EmulatorPlatform.IOS).canReadLog)
        assertTrue(EmulatorDevice("R3C", "Galaxy", EmulatorPlatform.ANDROID, isPhysical = true).canReadLog)
    }
}
