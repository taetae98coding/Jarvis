package io.github.taetae98coding.jarvis.domain.qrcode

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class QrCodeUseCasesTest {
    private val generate = GenerateQrCodeUseCase()

    @Test
    fun generateUsesSelectedErrorCorrection() {
        val input = QrCodeInput(QrContentType.TEXT, mapOf(QrField.TEXT to "https://example.com"), QrErrorCorrection.H)

        val result = assertIs<QrCodeResult.Ready>(generate(input))

        assertEquals("https://example.com", result.payload)
        assertEquals(QrErrorCorrection.H, result.code.errorCorrection)
    }

    @Test
    fun generatePassesThroughEmptyMissingAndTooLong() {
        assertEquals(QrCodeResult.Empty, generate(QrCodeInput()))
        assertEquals(QrCodeResult.Missing(QrField.WIFI_PASSWORD), generate(QrCodeInput(QrContentType.WIFI, mapOf(QrField.WIFI_SSID to "home"))))

        val long = "가".repeat(1000)
        assertEquals(
            QrCodeResult.TooLong(long, QrMode.BYTE, limit = 2331, length = 3000),
            generate(QrCodeInput(QrContentType.TEXT, mapOf(QrField.TEXT to long), QrErrorCorrection.M)),
        )
    }

    @Test
    fun commandsWriteToRepository() = runTest {
        val repository = FakeRepository()

        SelectQrContentTypeUseCase(repository)(QrContentType.WIFI)
        SetQrFieldUseCase(repository)(QrField.WIFI_SSID, "home")
        SetQrErrorCorrectionUseCase(repository)(QrErrorCorrection.Q)

        val expected = QrCodeInput(QrContentType.WIFI, mapOf(QrField.WIFI_SSID to "home"), QrErrorCorrection.Q)
        assertEquals(expected, ObserveQrCodeInputUseCase(repository)().first())
        assertEquals(expected, ObserveQrCodeInputUseCase(repository).initial())
    }

    private class FakeRepository : QrCodeSettingsRepository {
        val state = MutableStateFlow(QrCodeInput())

        override fun observeInput(): Flow<QrCodeInput> = state

        override fun readInput(): QrCodeInput = state.value

        override fun setContentType(type: QrContentType) {
            state.value = state.value.copy(type = type)
        }

        override fun setField(field: QrField, value: String) {
            state.value = state.value.copy(fields = state.value.fields + (field to value))
        }

        override fun setErrorCorrection(errorCorrection: QrErrorCorrection) {
            state.value = state.value.copy(errorCorrection = errorCorrection)
        }
    }
}
