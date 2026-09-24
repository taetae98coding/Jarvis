package io.github.taetae98coding.jarvis.domain.rotation

/** 화면 회전 알림 위젯의 상태. 기본값은 알림을 만들 수 없는 플랫폼의 값이다. */
data class DeviceRotationNotificationStatus(
    /** 이 플랫폼이 알림 창에 컨트롤을 놓을 수 있는가. false 면 카드에 "알림에 고정" 행이 없다. */
    val supported: Boolean = false,
    /** 알림 게시 권한이 허용됐는가. */
    val permitted: Boolean = false,
    /** 사용자가 "알림에 고정"을 켰는가. 권한과 무관하게 저장된다. */
    val pinned: Boolean = false,
)
