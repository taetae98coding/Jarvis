package io.github.taetae98coding.jarvis.data.emulator

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DeviceLogCommandTest {
    @Test
    fun firstLogcatStartsFromTheRecentBacklog() {
        assertEquals(
            listOf("/sdk/adb", "-s", "emulator-5554", "logcat", "-v", "threadtime", "-T", "1000"),
            logcatCommand("/sdk/adb", "emulator-5554", since = null),
        )
    }

    // 다시 띄울 때 최근 1000줄을 또 받으면 끊기기 전 줄이 겹친다.
    @Test
    fun restartedLogcatStartsFromTheLastLineTime() {
        val serial = "adb-R3CY705Y62R-WbpIT6._adb-tls-connect._tcp"

        assertEquals(
            listOf("/sdk/adb", "-s", serial, "logcat", "-v", "threadtime", "-T", "09-25 17:18:54.505"),
            logcatCommand("/sdk/adb", serial, since = "09-25 17:18:54.505"),
        )
    }

    @Test
    fun readsTheThreadtimeTimestamp() {
        assertEquals("09-25 17:18:54.505", logcatTimestamp("09-25 17:18:54.505  1803 26154 I sensors-hal: handle"))
        assertNull(logcatTimestamp("--------- beginning of main"))
    }

    @Test
    fun simulatorLogsComeFromTheRuntimeLogTool() {
        assertEquals(
            listOf("xcrun", "simctl", "spawn", "66C9B671-0000-0000-0000-DF9528508CD7", "log", "stream", "--style", "compact"),
            simulatorLogCommand(listOf("xcrun", "simctl"), "66C9B671-0000-0000-0000-DF9528508CD7"),
        )
    }
}
