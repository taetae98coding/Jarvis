package io.github.taetae98coding.jarvis.data.appinfo

import kotlin.test.Test
import kotlin.test.assertEquals

class DeviceIdentityJvmTest {
    @Test
    fun platformUuidReadsIoregLine() {
        val output = """
            +-o J316cAP  <class IOPlatformExpertDevice, id 0x100000117, registered, matched, active, busy 0 (12 ms), retain 40>
                {
                  "IOPlatformSerialNumber" = "C02ABCDEF"
                  "IOPlatformUUID" = "5C961F1B-D09E-502B-8FA3-0123456789AB"
                  "model" = <"Mac13,1">
                }
        """.trimIndent()

        assertEquals("5C961F1B-D09E-502B-8FA3-0123456789AB", platformUuid(output))
    }

    @Test
    fun platformUuidIsEmptyWithoutLine() {
        assertEquals("", platformUuid(""))
    }
}
