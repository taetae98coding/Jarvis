package io.github.taetae98coding.jarvis.domain.emulator

/**
 * 개발자 머신이 mDNS(`_adb-tls-pairing._tcp`)로 찾은, 페어링을 기다리는 Android 기기 하나.
 *
 * [name] 은 mDNS 인스턴스 이름이다. 페어링 코드 창을 연 기기는 `adb-<시리얼>-<임의>` 이고, QR 을
 * 스캔한 기기는 QR 에 담긴 [PairingQrCode.serviceName] 그대로다.
 */
data class PairingService(
    val name: String,
    val host: String,
    val port: Int,
) {
    val address: String
        get() = "$host:$port"
}

/**
 * 기기의 "QR 코드로 기기 페어링" 이 읽는 값. 기기는 스캔한 뒤 [serviceName] 으로 페어링을 알리고,
 * [password] 가 곧 `adb pair` 에 넘길 페어링 코드다.
 */
data class PairingQrCode(
    val serviceName: String,
    val password: String,
) {
    // Android Settings 의 AdbQrCode 가 읽는 형식이다. 두 값이 영숫자뿐이라 이스케이프할 것이 없다.
    val payload: String
        get() = "WIFI:T:ADB;S:$serviceName;P:$password;;"
}

sealed interface PairingResult {
    /** [isConnected] 가 false 면 페어링만 됐고 `adb` 가 아직 붙지 않았다. 붙으면 기기 목록에 나타난다. */
    data class Paired(val isConnected: Boolean) : PairingResult

    /** [reason] 은 사람이 읽을 한 줄이다. `adb` 가 준 문구면 번역하지 않고 그대로 둔다. */
    data class Failed(val reason: String) : PairingResult
}

sealed interface QrPairingState {
    /** 개발자 머신이 페어링 대기 기기를 찾을 수 없다. 0개와 다르다. */
    data object Unavailable : QrPairingState

    data object Waiting : QrPairingState

    data class Pairing(val service: PairingService) : QrPairingState

    data class Finished(val result: PairingResult) : QrPairingState
}
