package io.github.taetae98coding.jarvis.ui.qrcode

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.text.AnnotatedString
import io.github.taetae98coding.jarvis.designsystem.theme.JarvisTheme
import io.github.taetae98coding.jarvis.domain.qrcode.GenerateQrCodeUseCase
import io.github.taetae98coding.jarvis.domain.qrcode.QrCodeInput
import io.github.taetae98coding.jarvis.domain.qrcode.QrContentType
import io.github.taetae98coding.jarvis.domain.qrcode.QrErrorCorrection
import io.github.taetae98coding.jarvis.domain.qrcode.QrField
import io.github.taetae98coding.jarvis.domain.qrcode.WifiSecurity
import kotlin.test.Test
import kotlin.test.assertEquals

/** docs/common/qr-code.html#verification */
@OptIn(ExperimentalTestApi::class)
class QrCodeScreenTest {
    private val generate = GenerateQrCodeUseCase()

    @Test
    fun emptyInputShowsHintInsteadOfCode() = runComposeUiTest {
        setScreen(QrCodeInput())

        onNodeWithTag(QrCodeMessageTestTag).assertTextEquals("내용을 넣으면 QR 코드가 나타납니다.")
        onNodeWithTag(QrCodeImageTestTag).assertDoesNotExist()
    }

    @Test
    fun typedTextDrawsCodeAndCopiesPayload() = runComposeUiTest {
        val clipboard = RecordingClipboardManager()
        setScreen(QrCodeInput(), clipboard = clipboard)

        onNodeWithTag(qrCodeFieldTestTag(QrField.TEXT)).performTextInput("https://example.com")

        onNodeWithTag(QrCodeImageTestTag).assertIsDisplayed()
        onNodeWithContentDescription(QrCodeImageDefaults.ContentDescription).assertExists()
        onNodeWithTag(QrCodePayloadTestTag).assertTextEquals("https://example.com")
        onNodeWithTag(QrCodeCopyTestTag).performScrollTo().performClick()
        assertEquals(listOf("https://example.com"), clipboard.copied)
    }

    @Test
    fun typeAndErrorCorrectionChipsSelect() = runComposeUiTest {
        val state = setScreen(QrCodeInput())

        onNodeWithTag(qrCodeTypeTestTag(QrContentType.TEXT)).assertIsSelected()
        onNodeWithTag(qrCodeTypeTestTag(QrContentType.CONTACT)).performClick().assertIsSelected()
        onNodeWithTag(qrCodeTypeTestTag(QrContentType.TEXT)).assertIsNotSelected()
        onNodeWithTag(qrCodeFieldTestTag(QrField.CONTACT_NAME)).assertExists()
        onNodeWithTag(qrCodeFieldTestTag(QrField.TEXT)).assertDoesNotExist()

        onNodeWithTag(qrCodeErrorCorrectionTestTag(QrErrorCorrection.M)).assertIsSelected()
        onNodeWithTag(qrCodeErrorCorrectionTestTag(QrErrorCorrection.H)).performClick().assertIsSelected()
        assertEquals(QrErrorCorrection.H, state.input.errorCorrection)
    }

    @Test
    fun wifiFormBuildsEscapedPayload() = runComposeUiTest {
        setScreen(QrCodeInput(type = QrContentType.WIFI))

        onNodeWithTag(qrCodeFieldTestTag(QrField.WIFI_SSID)).performTextInput("Cafe;2F")
        onNodeWithTag(QrCodeMessageTestTag).assertTextEquals("비밀번호 칸을 채워야 QR 코드가 나타납니다.")
        onNodeWithTag(qrCodeFieldTestTag(QrField.WIFI_PASSWORD)).performTextInput("a:b")
        onNodeWithTag(QrCodeHiddenTestTag).performClick()

        onNodeWithTag(QrCodePayloadTestTag).assertTextEquals("WIFI:T:WPA;S:Cafe\\;2F;P:a\\:b;H:true;;")

        // 보안 없음이면 비밀번호 칸이 사라지고 P 도 빠진다.
        onNodeWithTag(qrCodeSecurityTestTag(WifiSecurity.NONE)).performClick()
        onNodeWithTag(qrCodeFieldTestTag(QrField.WIFI_PASSWORD)).assertDoesNotExist()
        onNodeWithTag(QrCodePayloadTestTag).assertTextEquals("WIFI:T:nopass;S:Cafe\\;2F;H:true;;")
    }

    @Test
    fun tooLongInputShowsLimit() = runComposeUiTest {
        setScreen(QrCodeInput(fields = mapOf(QrField.TEXT to "a".repeat(1300)), errorCorrection = QrErrorCorrection.H))

        onNodeWithTag(QrCodeMessageTestTag).assertTextContains("최대는 1273바이트", substring = true)
        onNodeWithTag(QrCodeImageTestTag).assertDoesNotExist()
    }

    @Test
    fun cardShowsSmallCodeOnlyWhenReady() = runComposeUiTest {
        var result by mutableStateOf(generate(QrCodeInput()))
        setContent { JarvisTheme { QrCodeCard(result = result, onClick = {}) } }

        onNodeWithTag(QrCodeCardImageTestTag, useUnmergedTree = true).assertDoesNotExist()
        result = generate(QrCodeInput(fields = mapOf(QrField.TEXT to "hello")))
        onNodeWithTag(QrCodeCardImageTestTag, useUnmergedTree = true).assertIsDisplayed()
    }

    private class ScreenState(initial: QrCodeInput) {
        var input by mutableStateOf(initial)
    }

    private fun ComposeUiTest.setScreen(
        initial: QrCodeInput,
        clipboard: RecordingClipboardManager = RecordingClipboardManager(),
    ): ScreenState {
        val state = ScreenState(initial)

        setContent {
            CompositionLocalProvider(LocalClipboardManager provides clipboard) {
                JarvisTheme {
                    QrCodeScreen(
                        input = state.input,
                        result = generate(state.input),
                        initialFieldValue = { state.input[it] },
                        onSelectType = { state.input = state.input.copy(type = it) },
                        onFieldChange = { field, value -> state.input = state.input.copy(fields = state.input.fields + (field to value)) },
                        onErrorCorrectionChange = { state.input = state.input.copy(errorCorrection = it) },
                        onBack = {},
                    )
                }
            }
        }
        return state
    }
}

// 기본 관리자(AwtClipboardManager 등)는 테스트 중에 실제 시스템 클립보드를 덮어쓴다. 기록만 하는 것으로 바꾼다.
@Suppress("DEPRECATION")
private class RecordingClipboardManager : ClipboardManager {
    val copied = mutableListOf<String>()

    override fun setText(annotatedString: AnnotatedString) {
        copied += annotatedString.text
    }

    override fun getText(): AnnotatedString? = copied.lastOrNull()?.let(::AnnotatedString)
}
