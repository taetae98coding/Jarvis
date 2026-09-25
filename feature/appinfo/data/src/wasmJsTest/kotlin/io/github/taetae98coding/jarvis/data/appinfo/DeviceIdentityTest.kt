package io.github.taetae98coding.jarvis.data.appinfo

import io.github.taetae98coding.jarvis.data.PlatformContext
import kotlin.test.Test
import kotlin.test.assertTrue

class DeviceIdentityTest {
    @Test
    fun deviceIdentityIsNotBlank() {
        val device = readDeviceIdentity(PlatformContext())

        assertTrue(device.name.isNotBlank(), "기기 이름이 비어 있다")
        assertTrue(device.id.isNotBlank(), "기기 식별자가 비어 있다")
    }
}
