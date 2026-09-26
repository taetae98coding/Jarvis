package io.github.taetae98coding.jarvis.domain.qrcode

import kotlin.test.Test
import kotlin.test.assertEquals

/** docs/common/qr-code.html#requirements R3–R6 */
class QrPayloadTest {
    @Test
    fun textIsUsedAsIs() {
        assertEquals(ready("https://example.com/a b"), build(QrContentType.TEXT, QrField.TEXT to "https://example.com/a b"))
        assertEquals(QrPayloadResult.Empty, build(QrContentType.TEXT))
    }

    @Test
    fun wifiEscapesSpecialCharacters() {
        assertEquals("a\\\\b\\;c\\,d\\:e\\\"f", QrPayload.escape("a\\b;c,d:e\"f"))

        assertEquals(
            ready("WIFI:T:WPA;S:My\\;Net;P:p\\:w\\,d\\\\;H:true;;"),
            build(
                QrContentType.WIFI,
                QrField.WIFI_SSID to "My;Net",
                QrField.WIFI_PASSWORD to "p:w,d\\",
                QrField.WIFI_HIDDEN to "true",
            ),
        )
    }

    @Test
    fun wifiSecurityVariants() {
        assertEquals(
            ready("WIFI:T:WEP;S:cafe;P:12345;;"),
            build(QrContentType.WIFI, QrField.WIFI_SSID to "cafe", QrField.WIFI_PASSWORD to "12345", QrField.WIFI_SECURITY to "WEP"),
        )
        // 비밀번호 없는 네트워크는 P 를 빼고, 남아 있는 비밀번호 칸 값도 넣지 않는다.
        assertEquals(
            ready("WIFI:T:nopass;S:guest;;"),
            build(QrContentType.WIFI, QrField.WIFI_SSID to "guest", QrField.WIFI_PASSWORD to "old", QrField.WIFI_SECURITY to "nopass"),
        )
    }

    @Test
    fun wifiReportsMissingFields() {
        assertEquals(QrPayloadResult.Empty, build(QrContentType.WIFI))
        assertEquals(QrPayloadResult.Missing(QrField.WIFI_PASSWORD), build(QrContentType.WIFI, QrField.WIFI_SSID to "home"))
        assertEquals(QrPayloadResult.Missing(QrField.WIFI_SSID), build(QrContentType.WIFI, QrField.WIFI_PASSWORD to "secret"))
    }

    @Test
    fun contactIsMecardWithOnlyFilledFields() {
        assertEquals(
            ready("MECARD:N:홍길동;TEL:010-1234-5678;EMAIL:hong@example.com;;"),
            build(
                QrContentType.CONTACT,
                QrField.CONTACT_NAME to " 홍길동 ",
                QrField.CONTACT_PHONE to "010-1234-5678",
                QrField.CONTACT_EMAIL to "hong@example.com",
            ),
        )
        assertEquals(ready("MECARD:TEL:+821012345678;;"), build(QrContentType.CONTACT, QrField.CONTACT_PHONE to "+82 10 1234 5678"))
        assertEquals(ready("MECARD:N:Doe\\,John;;"), build(QrContentType.CONTACT, QrField.CONTACT_NAME to "Doe,John"))
        assertEquals(QrPayloadResult.Empty, build(QrContentType.CONTACT))
    }

    @Test
    fun phoneSmsAndEmail() {
        assertEquals(ready("tel:+82-101234-5678"), build(QrContentType.PHONE, QrField.PHONE_NUMBER to "+82-10 1234-5678"))
        assertEquals(QrPayloadResult.Empty, build(QrContentType.PHONE))

        assertEquals(ready("SMSTO:01012345678:곧 도착: 5분"), build(QrContentType.SMS, QrField.SMS_NUMBER to "010 1234 5678", QrField.SMS_MESSAGE to "곧 도착: 5분"))
        assertEquals(ready("SMSTO:01012345678"), build(QrContentType.SMS, QrField.SMS_NUMBER to "01012345678"))
        assertEquals(QrPayloadResult.Missing(QrField.SMS_NUMBER), build(QrContentType.SMS, QrField.SMS_MESSAGE to "hi"))

        assertEquals(
            ready("mailto:a@b.com?subject=Hello%20World&body=%EC%95%88%EB%85%95%26%3F"),
            build(QrContentType.EMAIL, QrField.EMAIL_ADDRESS to "a@b.com", QrField.EMAIL_SUBJECT to "Hello World", QrField.EMAIL_BODY to "안녕&?"),
        )
        assertEquals(ready("mailto:a@b.com"), build(QrContentType.EMAIL, QrField.EMAIL_ADDRESS to " a@b.com "))
        assertEquals(QrPayloadResult.Missing(QrField.EMAIL_ADDRESS), build(QrContentType.EMAIL, QrField.EMAIL_BODY to "x"))
    }

    @Test
    fun otherTypesFieldsAreIgnored() {
        assertEquals(QrPayloadResult.Empty, build(QrContentType.TEXT, QrField.WIFI_SSID to "home"))
    }

    private fun ready(text: String) = QrPayloadResult.Ready(text)

    private fun build(type: QrContentType, vararg fields: Pair<QrField, String>): QrPayloadResult =
        QrPayload.build(QrCodeInput(type = type, fields = fields.toMap()))
}
