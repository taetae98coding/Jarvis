package io.github.taetae98coding.jarvis.domain.qrcode

enum class QrContentType {
    TEXT,
    WIFI,
    CONTACT,
    PHONE,
    SMS,
    EMAIL,
    ;

    val storedValue: String get() = name.lowercase()

    val fields: List<QrField> get() = QrField.entries.filter { it.type == this }

    companion object {
        fun fromStored(value: String?): QrContentType = entries.firstOrNull { it.storedValue == value } ?: TEXT
    }
}

/**
 * 입력 칸 하나. 값은 모두 문자열로 저장한다(보안 방식은 [WifiSecurity.storedValue], 숨긴 네트워크는 `"true"`/`""`).
 *
 * [persisted] 가 false 인 칸은 앱이 떠 있는 동안 메모리에만 둔다(docs/common/qr-code.html#decision-password).
 */
enum class QrField(val type: QrContentType, val persisted: Boolean = true) {
    TEXT(QrContentType.TEXT),
    WIFI_SSID(QrContentType.WIFI),
    WIFI_PASSWORD(QrContentType.WIFI, persisted = false),
    WIFI_SECURITY(QrContentType.WIFI),
    WIFI_HIDDEN(QrContentType.WIFI),
    CONTACT_NAME(QrContentType.CONTACT),
    CONTACT_PHONE(QrContentType.CONTACT),
    CONTACT_EMAIL(QrContentType.CONTACT),
    PHONE_NUMBER(QrContentType.PHONE),
    SMS_NUMBER(QrContentType.SMS),
    SMS_MESSAGE(QrContentType.SMS),
    EMAIL_ADDRESS(QrContentType.EMAIL),
    EMAIL_SUBJECT(QrContentType.EMAIL),
    EMAIL_BODY(QrContentType.EMAIL),
    ;

    val storedValue: String get() = name.lowercase()
}

enum class WifiSecurity(val storedValue: String) {
    WPA("WPA"),
    WEP("WEP"),
    NONE("nopass"),
    ;

    companion object {
        fun fromStored(value: String?): WifiSecurity = entries.firstOrNull { it.storedValue == value } ?: WPA
    }
}

/** 고른 종류, 칸마다의 값, 오류 정정 단계. 다른 종류의 칸 값도 함께 들고 있어 종류를 오가도 남는다. */
data class QrCodeInput(
    val type: QrContentType = QrContentType.TEXT,
    val fields: Map<QrField, String> = emptyMap(),
    val errorCorrection: QrErrorCorrection = QrErrorCorrection.Default,
) {
    operator fun get(field: QrField): String = fields[field].orEmpty()
}
