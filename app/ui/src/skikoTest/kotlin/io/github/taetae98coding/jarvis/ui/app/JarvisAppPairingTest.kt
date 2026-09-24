package io.github.taetae98coding.jarvis.ui.app

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.v2.runComposeUiTest
import io.github.taetae98coding.jarvis.domain.emulator.PairingService
import io.github.taetae98coding.jarvis.ui.emulator.EmulatorListTestTag
import io.github.taetae98coding.jarvis.ui.emulator.EmulatorTestTag
import io.github.taetae98coding.jarvis.ui.emulator.WifiPairingCodeTabTestTag
import io.github.taetae98coding.jarvis.ui.emulator.WifiPairingIosTestTag
import io.github.taetae98coding.jarvis.ui.emulator.WifiPairingQrTestTag
import io.github.taetae98coding.jarvis.ui.emulator.WifiPairingTestTag
import io.github.taetae98coding.jarvis.ui.emulator.wifiPairingCodeTestTag
import io.github.taetae98coding.jarvis.ui.emulator.wifiPairingPairTestTag
import io.github.taetae98coding.jarvis.ui.emulator.wifiPairingServiceTestTag
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class JarvisAppPairingTest {
    @Test
    fun deviceListOpensWifiPairing() = runComposeUiTest {
        setContent { TestJarvisApp() }
        onNodeWithTag(EmulatorTestTag).performClick()

        onNodeWithTag(WifiPairingTestTag).performClick()

        // 처음 열리는 탭은 QR 이다.
        onNodeWithTag(WifiPairingQrTestTag).assertIsDisplayed()
        onNodeWithContentDescription("페어링 QR 코드").assertIsDisplayed()
        onNodeWithTag(WifiPairingIosTestTag).assertIsDisplayed()

        onNodeWithContentDescription("뒤로").performClick()
        onNodeWithTag(EmulatorListTestTag).assertIsDisplayed()
    }

    @Test
    fun pairingCodeIsSentForTheChosenDevice() = runComposeUiTest {
        val pairing = FakeDevicePairingRepository(services = listOf(WaitingService, OtherService))
        setContent { TestJarvisApp(pairing = pairing) }
        openPairingCodeTab()

        onNodeWithTag(wifiPairingServiceTestTag(WaitingService.name)).assertIsDisplayed()
        onNodeWithText(WaitingService.address).assertIsDisplayed()

        // 6자리가 되기 전에는 누를 수 없다.
        onNodeWithTag(wifiPairingCodeTestTag(WaitingService.name)).performTextInput("12345")
        onNodeWithTag(wifiPairingPairTestTag(WaitingService.name)).assertIsNotEnabled()
        onNodeWithTag(wifiPairingCodeTestTag(WaitingService.name)).performTextInput("6")
        onNodeWithTag(wifiPairingPairTestTag(WaitingService.name)).assertIsEnabled()

        onNodeWithTag(wifiPairingPairTestTag(WaitingService.name)).performClick()

        waitUntil(timeoutMillis = TimeoutMillis) { pairing.paired.isNotEmpty() }
        assertEquals(listOf(WaitingService to "123456"), pairing.paired.toList())
    }

    // 0개와 구분되어야 데스크톱 앱이 꺼져 있는 것을 알아챈다.
    @Test
    fun pairingSaysWhenNothingCanBeFound() = runComposeUiTest {
        setContent { TestJarvisApp(pairing = FakeDevicePairingRepository(services = null)) }
        openPairingCodeTab()

        onNodeWithText("개발자 머신에서 페어링 대기 기기를 찾을 수 없습니다.").assertIsDisplayed()
    }

    @Test
    fun pairingSaysWhenNoDeviceIsWaiting() = runComposeUiTest {
        setContent { TestJarvisApp() }
        openPairingCodeTab()

        onNodeWithText("페어링을 기다리는 기기가 없습니다.").assertIsDisplayed()
    }

    private fun ComposeUiTest.openPairingCodeTab() {
        onNodeWithTag(EmulatorTestTag).performClick()
        onNodeWithTag(WifiPairingTestTag).performClick()
        onNodeWithTag(WifiPairingCodeTabTestTag).performClick()
    }

    private companion object {
        const val TimeoutMillis = 10_000L

        val WaitingService = PairingService(name = "adb-R54T202XEHN-Y2yH0N", host = "172.30.1.47", port = 37123)

        val OtherService = PairingService(name = "adb-R3CY705Y62R-WbpIT6", host = "172.30.1.87", port = 37099)
    }
}
