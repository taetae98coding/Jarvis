package io.github.taetae98coding.jarvis.domain.qrcode

sealed interface QrPayloadResult {
    data class Ready(val text: String) : QrPayloadResult

    /** 아무 칸도 채우지 않았다. */
    data object Empty : QrPayloadResult

    /** 이 칸이 비어 있으면 읽는 쪽이 쓸 수 없는 코드가 된다. */
    data class Missing(val field: QrField) : QrPayloadResult
}

/**
 * 종류마다 카메라 앱이 알아보는 문자열을 만든다.
 *
 * - Wi-Fi: ZXing 이 정하고 Android 10+ 카메라·iOS 11+ 카메라가 읽는 `WIFI:T:…;S:…;P:…;H:true;;`.
 * - 연락처: MECARD. vCard 3.0 보다 짧아 같은 내용이 한두 버전 작은 코드가 되고, iOS·Android 카메라가 모두 연락처로 연다.
 * - 전화 `tel:`(RFC 3966), 문자 `SMSTO:번호:본문`, 이메일 `mailto:`(RFC 6068).
 */
object QrPayload {
    fun build(input: QrCodeInput): QrPayloadResult = when (input.type) {
        QrContentType.TEXT -> input[QrField.TEXT].let { if (it.isEmpty()) QrPayloadResult.Empty else QrPayloadResult.Ready(it) }
        QrContentType.WIFI -> wifi(input)
        QrContentType.CONTACT -> contact(input)
        QrContentType.PHONE -> required(input, QrField.PHONE_NUMBER) { "tel:${compactNumber(it)}" }
        QrContentType.SMS -> sms(input)
        QrContentType.EMAIL -> email(input)
    }

    /** Wi-Fi 와 MECARD 가 함께 쓰는 규칙: `\ ; , : "` 앞에 역슬래시. */
    fun escape(value: String): String = buildString {
        for (c in value) {
            if (c in "\\;,:\"") append('\\')
            append(c)
        }
    }

    private fun wifi(input: QrCodeInput): QrPayloadResult {
        val ssid = input[QrField.WIFI_SSID]
        val password = input[QrField.WIFI_PASSWORD]
        val security = WifiSecurity.fromStored(input[QrField.WIFI_SECURITY])
        if (ssid.isEmpty()) return if (password.isEmpty()) QrPayloadResult.Empty else QrPayloadResult.Missing(QrField.WIFI_SSID)
        if (security != WifiSecurity.NONE && password.isEmpty()) return QrPayloadResult.Missing(QrField.WIFI_PASSWORD)

        return QrPayloadResult.Ready(
            buildString {
                append("WIFI:T:").append(security.storedValue)
                append(";S:").append(escape(ssid))
                if (security != WifiSecurity.NONE) append(";P:").append(escape(password))
                if (input[QrField.WIFI_HIDDEN] == "true") append(";H:true")
                append(";;")
            },
        )
    }

    private fun contact(input: QrCodeInput): QrPayloadResult {
        val parts = listOf(
            "N" to input[QrField.CONTACT_NAME].trim(),
            "TEL" to compactNumber(input[QrField.CONTACT_PHONE]),
            "EMAIL" to input[QrField.CONTACT_EMAIL].trim(),
        ).filter { it.second.isNotEmpty() }
        if (parts.isEmpty()) return QrPayloadResult.Empty

        return QrPayloadResult.Ready(parts.joinToString(separator = "", prefix = "MECARD:", postfix = ";") { (key, value) -> "$key:${escape(value)};" })
    }

    private fun sms(input: QrCodeInput): QrPayloadResult {
        val number = compactNumber(input[QrField.SMS_NUMBER])
        val message = input[QrField.SMS_MESSAGE]
        if (number.isEmpty()) return if (message.isEmpty()) QrPayloadResult.Empty else QrPayloadResult.Missing(QrField.SMS_NUMBER)
        return QrPayloadResult.Ready(if (message.isEmpty()) "SMSTO:$number" else "SMSTO:$number:$message")
    }

    private fun email(input: QrCodeInput): QrPayloadResult {
        val address = input[QrField.EMAIL_ADDRESS].trim()
        val query = listOf("subject" to input[QrField.EMAIL_SUBJECT], "body" to input[QrField.EMAIL_BODY]).filter { it.second.isNotEmpty() }
        if (address.isEmpty()) return if (query.isEmpty()) QrPayloadResult.Empty else QrPayloadResult.Missing(QrField.EMAIL_ADDRESS)

        val suffix = if (query.isEmpty()) "" else query.joinToString("&", prefix = "?") { (key, value) -> "$key=${percentEncode(value)}" }
        return QrPayloadResult.Ready("mailto:$address$suffix")
    }

    private inline fun required(input: QrCodeInput, field: QrField, build: (String) -> String): QrPayloadResult {
        val value = compactNumber(input[field])
        return if (value.isEmpty()) QrPayloadResult.Empty else QrPayloadResult.Ready(build(value))
    }

    /** 번호 안의 공백은 사람이 읽기 좋으려고 친 것이라 뺀다. `-`·`+` 는 tel URI 가 받으므로 둔다. */
    private fun compactNumber(value: String): String = value.filterNot { it.isWhitespace() }

    /** RFC 3986 비예약 문자만 두고 UTF-8 바이트를 `%XX` 로. 공백은 `+` 가 아니라 `%20` 이다(RFC 6068 5절). */
    internal fun percentEncode(value: String): String = buildString {
        for (b in value.encodeToByteArray()) {
            val c = (b.toInt() and 0xFF)
            val ch = c.toChar()
            if (ch in 'A'..'Z' || ch in 'a'..'z' || ch in '0'..'9' || ch in "-_.~") {
                append(ch)
            } else {
                append('%').append(HexDigits[c ushr 4]).append(HexDigits[c and 0xF])
            }
        }
    }

    private const val HexDigits = "0123456789ABCDEF"
}
