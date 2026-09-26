package io.github.taetae98coding.jarvis.ui.app

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.runComposeUiTest
import io.github.taetae98coding.jarvis.designsystem.component.JarvisTopBarDefaults
import io.github.taetae98coding.jarvis.domain.qrcode.QrCodeInput
import io.github.taetae98coding.jarvis.domain.qrcode.QrContentType
import io.github.taetae98coding.jarvis.domain.qrcode.QrErrorCorrection
import io.github.taetae98coding.jarvis.domain.qrcode.QrField
import io.github.taetae98coding.jarvis.ui.qrcode.QrCodeCardImageTestTag
import io.github.taetae98coding.jarvis.ui.qrcode.QrCodeCopyTestTag
import io.github.taetae98coding.jarvis.ui.qrcode.QrCodePayloadTestTag
import io.github.taetae98coding.jarvis.ui.qrcode.QrCodeScreenTestTag
import io.github.taetae98coding.jarvis.ui.qrcode.QrCodeTestTag
import io.github.taetae98coding.jarvis.ui.qrcode.qrCodeErrorCorrectionTestTag
import io.github.taetae98coding.jarvis.ui.qrcode.qrCodeFieldTestTag
import io.github.taetae98coding.jarvis.ui.qrcode.qrCodeTypeTestTag
import kotlin.test.Test
import kotlin.test.assertEquals

/** docs/common/qr-code.html */
@OptIn(ExperimentalTestApi::class)
class JarvisAppQrCodeTest {
    @Test
    fun cardOpensScreenAndBackReturnsHome() = runComposeUiTest {
        setContent { TestJarvisApp() }

        onNodeWithTag(QrCodeTestTag).performScrollTo().performClick()
        onNodeWithTag(QrCodeScreenTestTag).assertIsDisplayed()

        onNodeWithContentDescription(JarvisTopBarDefaults.BackContentDescription).performClick()
        onNodeWithTag(QrCodeTestTag).assertExists()
    }

    @Test
    fun typedInputIsPersistedAndCopied() = runComposeUiTest {
        val qrCode = FakeQrCodeSettingsRepository()
        val clipboard = RecordingClipboardManager()
        setContent { TestJarvisApp(qrCode = qrCode, clipboard = clipboard) }

        onNodeWithTag(QrCodeTestTag).performScrollTo().performClick()
        onNodeWithTag(qrCodeTypeTestTag(QrContentType.PHONE)).performClick().assertIsSelected()
        onNodeWithTag(qrCodeFieldTestTag(QrField.PHONE_NUMBER)).performTextInput("010 1234 5678")
        onNodeWithTag(qrCodeErrorCorrectionTestTag(QrErrorCorrection.Q)).performClick()

        onNodeWithTag(QrCodePayloadTestTag).assertTextEquals("tel:01012345678")
        onNodeWithTag(QrCodeCopyTestTag).performScrollTo().performClick()

        assertEquals(QrContentType.PHONE, qrCode.input.value.type)
        assertEquals("010 1234 5678", qrCode.input.value[QrField.PHONE_NUMBER])
        assertEquals(QrErrorCorrection.Q, qrCode.input.value.errorCorrection)
        assertEquals(listOf("tel:01012345678"), clipboard.copied)
    }

    @Test
    fun savedInputIsShownOnCardAndRestoredOnScreen() = runComposeUiTest {
        val qrCode = FakeQrCodeSettingsRepository().apply {
            input.value = QrCodeInput(QrContentType.CONTACT, mapOf(QrField.CONTACT_NAME to "홍길동"), QrErrorCorrection.H)
        }
        setContent { TestJarvisApp(qrCode = qrCode) }

        onNodeWithTag(QrCodeTestTag).performScrollTo()
        onNodeWithTag(QrCodeCardImageTestTag, useUnmergedTree = true).assertExists()

        onNodeWithTag(QrCodeTestTag).performClick()
        onNodeWithTag(qrCodeTypeTestTag(QrContentType.CONTACT)).assertIsSelected()
        onNodeWithTag(qrCodeErrorCorrectionTestTag(QrErrorCorrection.H)).assertIsSelected()
        onNodeWithTag(qrCodeFieldTestTag(QrField.CONTACT_NAME)).assertTextContains("홍길동")
        onNodeWithTag(QrCodePayloadTestTag).assertTextEquals("MECARD:N:홍길동;;")
    }
}
