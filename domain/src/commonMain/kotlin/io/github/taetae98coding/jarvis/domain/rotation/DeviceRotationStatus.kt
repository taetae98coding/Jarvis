package io.github.taetae98coding.jarvis.domain.rotation

/**
 * 화면 회전 기능의 상태.
 *
 * 무엇이 돌아가는지는 플랫폼마다 다르다. Android 는 기기 전체, iOS 는 앱 창, Web 은 브라우저
 * 문서다. 화면은 그 차이를 모르고 이 상태만 본다.
 */
data class DeviceRotationStatus(
    /** 이 플랫폼에서 화면을 돌릴 수 있는가. */
    val supported: Boolean = false,
    /**
     * 돌릴 권한이 있는가. Android 는 `WRITE_SETTINGS`, Web 은 브라우저가 잠금 요청을 받아 주는지다.
     * 권한을 요구하지 않는 플랫폼은 true 로 둔다.
     */
    val permitted: Boolean = true,
    /** 현재 각도에 고정되어 있는가. false 면 센서를 따라 돈다. */
    val locked: Boolean = false,
    /** 지금 각도. 읽을 수 없으면 null. */
    val angle: RotationAngle? = null,
)
