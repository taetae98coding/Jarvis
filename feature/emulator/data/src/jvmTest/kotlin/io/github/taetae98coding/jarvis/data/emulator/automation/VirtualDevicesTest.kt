package io.github.taetae98coding.jarvis.data.emulator.automation

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class VirtualDevicesTest {
    @Test
    fun nextNameTakesTheFirstUnusedNumber() {
        assertEquals("Jarvis_1", nextName(listOf("Pixel_9"), AvdNamePrefix))
        assertEquals("Jarvis 3", nextName(listOf("Jarvis 1", "Jarvis 2", "Jarvis 4"), SimulatorNamePrefix))
    }

    @Test
    fun systemImageIsTheNewestPhoneImageForTheHost() {
        val images = listOf(
            SystemImage("android-36", "android-tv", "arm64-v8a"),
            SystemImage("android-35", "google_apis_playstore", "arm64-v8a"),
            SystemImage("android-35", "google_apis", "arm64-v8a"),
            SystemImage("android-34", "google_apis", "arm64-v8a"),
            SystemImage("android-36", "google_apis", "x86_64"),
            SystemImage("android-Baklava", "google_apis", "arm64-v8a"),
        )

        assertEquals(SystemImage("android-35", "google_apis_playstore", "arm64-v8a"), pickSystemImage(images, hostAbi("aarch64")))
        assertEquals(SystemImage("android-36", "google_apis", "x86_64"), pickSystemImage(images, hostAbi("x86_64")))
        assertNull(pickSystemImage(images.take(1), "arm64-v8a"))
    }

    @Test
    fun avdFilesPointAtTheImage() {
        val image = SystemImage("android-35", "google_apis", "arm64-v8a")

        val config = avdConfig("Jarvis_1", image).lines()
        assertTrue("image.sysdir.1=system-images/android-35/google_apis/arm64-v8a/" in config)
        assertTrue("hw.cpu.arch=arm64" in config)
        assertTrue("PlayStore.enabled=false" in config)
        assertTrue("AvdId=Jarvis_1" in config)

        val ini = avdIni(File("/home/me/.android/avd/Jarvis_1.avd"), image).lines()
        assertTrue("path=/home/me/.android/avd/Jarvis_1.avd" in ini)
        assertTrue("path.rel=avd/Jarvis_1.avd" in ini)
        assertTrue("target=android-35" in ini)
    }

    @Test
    fun simulatorRuntimeIsTheNewestAvailableIosWithItsFirstIphone() {
        val json = """
            {"runtimes":[
              {"identifier":"com.apple.CoreSimulator.SimRuntime.iOS-26-4","version":"26.4","platform":"iOS","isAvailable":true,
               "supportedDeviceTypes":[{"identifier":"t.iPhone-17","productFamily":"iPhone"}]},
              {"identifier":"com.apple.CoreSimulator.SimRuntime.iOS-28-0","version":"28.0","platform":"iOS","isAvailable":false,
               "supportedDeviceTypes":[{"identifier":"t.iPhone-19","productFamily":"iPhone"}]},
              {"identifier":"com.apple.CoreSimulator.SimRuntime.watchOS-12-0","version":"30.0","platform":"watchOS","isAvailable":true,
               "supportedDeviceTypes":[{"identifier":"t.Watch","productFamily":"Apple Watch"}]},
              {"identifier":"com.apple.CoreSimulator.SimRuntime.iOS-27-0","version":"27.0","platform":"iOS","isAvailable":true,
               "supportedDeviceTypes":[{"identifier":"t.iPad","productFamily":"iPad"},{"identifier":"t.iPhone-18-Pro","productFamily":"iPhone"}]}
            ]}
        """.trimIndent()

        assertEquals("com.apple.CoreSimulator.SimRuntime.iOS-27-0" to "t.iPhone-18-Pro", pickSimulatorRuntime(json))
        assertNull(pickSimulatorRuntime("""{"runtimes":[]}"""))
    }
}
