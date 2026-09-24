package io.github.taetae98coding.jarvis.domain.emulator

/** 실물 기기가 개발자 머신에 붙은 방식. 가상 기기와, 알아낼 수 없는 경우는 null 로 둔다. */
enum class DeviceConnection {
    /** 케이블(USB). */
    WIRED,

    /** 네트워크(Wi-Fi·adb connect·무선 디버깅). */
    WIRELESS,
}
