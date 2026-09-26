package io.github.taetae98coding.jarvis.ui.qrcode

import io.github.taetae98coding.jarvis.domain.qrcode.QrCodeResult
import io.github.taetae98coding.jarvis.domain.qrcode.QrContentType
import io.github.taetae98coding.jarvis.domain.qrcode.QrErrorCorrection
import io.github.taetae98coding.jarvis.domain.qrcode.QrField
import io.github.taetae98coding.jarvis.domain.qrcode.QrMode
import io.github.taetae98coding.jarvis.domain.qrcode.WifiSecurity

internal val QrContentType.label: String
    get() = when (this) {
        QrContentType.TEXT -> "텍스트·URL"
        QrContentType.WIFI -> "Wi-Fi"
        QrContentType.CONTACT -> "연락처"
        QrContentType.PHONE -> "전화"
        QrContentType.SMS -> "문자"
        QrContentType.EMAIL -> "이메일"
    }

internal val QrField.label: String
    get() = when (this) {
        QrField.TEXT -> "텍스트 또는 URL"
        QrField.WIFI_SSID -> "네트워크 이름(SSID)"
        QrField.WIFI_PASSWORD -> "비밀번호"
        QrField.WIFI_SECURITY -> "보안"
        QrField.WIFI_HIDDEN -> "숨긴 네트워크"
        QrField.CONTACT_NAME -> "이름"
        QrField.CONTACT_PHONE, QrField.PHONE_NUMBER, QrField.SMS_NUMBER -> "전화번호"
        QrField.CONTACT_EMAIL, QrField.EMAIL_ADDRESS -> "이메일 주소"
        QrField.SMS_MESSAGE -> "내용"
        QrField.EMAIL_SUBJECT -> "제목"
        QrField.EMAIL_BODY -> "본문"
    }

internal val QrField.placeholder: String
    get() = when (this) {
        QrField.TEXT -> "https://example.com"
        QrField.CONTACT_PHONE, QrField.PHONE_NUMBER, QrField.SMS_NUMBER -> "010-1234-5678"
        QrField.CONTACT_EMAIL, QrField.EMAIL_ADDRESS -> "name@example.com"
        else -> ""
    }

internal val WifiSecurity.label: String
    get() = when (this) {
        WifiSecurity.WPA -> "WPA·WPA2·WPA3"
        WifiSecurity.WEP -> "WEP"
        WifiSecurity.NONE -> "없음"
    }

internal val QrErrorCorrection.label: String
    get() = when (this) {
        QrErrorCorrection.L -> "L 7%"
        QrErrorCorrection.M -> "M 15%"
        QrErrorCorrection.Q -> "Q 25%"
        QrErrorCorrection.H -> "H 30%"
    }

private val QrMode.unit: String
    get() = when (this) {
        QrMode.NUMERIC, QrMode.ALPHANUMERIC -> "자"
        QrMode.BYTE -> "바이트"
    }

/** 코드가 없을 때 그 자리에 보일 문구. 코드가 있으면 null. */
internal val QrCodeResult.message: String?
    get() = when (this) {
        QrCodeResult.Empty -> "내용을 넣으면 QR 코드가 나타납니다."
        is QrCodeResult.Missing -> "${field.label} 칸을 채워야 QR 코드가 나타납니다."
        is QrCodeResult.TooLong -> "QR 코드에 담기에 너무 깁니다. 지금 ${length}${mode.unit}, 이 오류 정정 단계의 최대는 ${limit}${mode.unit}입니다."
        is QrCodeResult.Ready -> null
    }
