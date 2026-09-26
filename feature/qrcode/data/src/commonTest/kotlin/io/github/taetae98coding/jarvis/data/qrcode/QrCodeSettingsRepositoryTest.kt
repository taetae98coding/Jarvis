package io.github.taetae98coding.jarvis.data.qrcode

import io.github.taetae98coding.jarvis.data.settings.InMemorySettingsStore
import io.github.taetae98coding.jarvis.domain.qrcode.QrCodeInput
import io.github.taetae98coding.jarvis.domain.qrcode.QrContentType
import io.github.taetae98coding.jarvis.domain.qrcode.QrErrorCorrection
import io.github.taetae98coding.jarvis.domain.qrcode.QrField
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

/** docs/common/qr-code.html R8 */
class QrCodeSettingsRepositoryTest {
    @Test
    fun defaultsToTextAndMedium() {
        assertEquals(QrCodeInput(), DefaultQrCodeSettingsRepository(InMemorySettingsStore()).readInput())
    }

    @Test
    fun typeFieldsAndLevelSurviveNewRepository() {
        val store = InMemorySettingsStore()
        DefaultQrCodeSettingsRepository(store).apply {
            setContentType(QrContentType.CONTACT)
            setField(QrField.CONTACT_NAME, "홍길동")
            setField(QrField.TEXT, "https://example.com")
            setErrorCorrection(QrErrorCorrection.H)
        }

        val reopened = DefaultQrCodeSettingsRepository(store).readInput()
        assertEquals(QrContentType.CONTACT, reopened.type)
        assertEquals("홍길동", reopened[QrField.CONTACT_NAME])
        assertEquals("https://example.com", reopened[QrField.TEXT])
        assertEquals(QrErrorCorrection.H, reopened.errorCorrection)
        assertEquals("contact", store.getString(DefaultQrCodeSettingsRepository.TypeKey, ""))
    }

    @Test
    fun wifiPasswordIsKeptInMemoryOnly() {
        val store = InMemorySettingsStore()
        val repository = DefaultQrCodeSettingsRepository(store)
        repository.setField(QrField.WIFI_SSID, "home")
        repository.setField(QrField.WIFI_PASSWORD, "secret")

        assertEquals("secret", repository.readInput()[QrField.WIFI_PASSWORD])
        assertEquals("", store.getString(DefaultQrCodeSettingsRepository.fieldKey(QrField.WIFI_PASSWORD), ""))

        val reopened = DefaultQrCodeSettingsRepository(store).readInput()
        assertEquals("home", reopened[QrField.WIFI_SSID])
        assertEquals("", reopened[QrField.WIFI_PASSWORD])
    }

    @Test
    fun unknownStoredValuesFallBack() {
        val store = InMemorySettingsStore(
            mutableMapOf(
                DefaultQrCodeSettingsRepository.TypeKey to "barcode",
                DefaultQrCodeSettingsRepository.ErrorCorrectionKey to "X",
            ),
        )

        val input = DefaultQrCodeSettingsRepository(store).readInput()
        assertEquals(QrContentType.TEXT, input.type)
        assertEquals(QrErrorCorrection.M, input.errorCorrection)
    }

    @Test
    fun observeFollowsWritesToBothStores() = runTest {
        val store = InMemorySettingsStore()
        val repository = DefaultQrCodeSettingsRepository(store)

        assertEquals(QrCodeInput(), repository.observeInput().first())

        repository.setContentType(QrContentType.WIFI)
        repository.setField(QrField.WIFI_PASSWORD, "pw")
        val observed = repository.observeInput().first()
        assertEquals(QrContentType.WIFI, observed.type)
        assertEquals("pw", observed[QrField.WIFI_PASSWORD])
        // 수집이 끝나면 리스너가 풀린다.
        assertEquals(0, store.listeners)
    }
}
