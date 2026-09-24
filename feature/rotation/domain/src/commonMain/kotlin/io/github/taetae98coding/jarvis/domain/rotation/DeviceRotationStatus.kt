package io.github.taetae98coding.jarvis.domain.rotation

/**
 * 화면 회전 기능의 상태. 도는 것은 앱 창이 아니라 기기 전체다(공통 스펙 R9). 그렇게 할 수 없는
 * 플랫폼은 [supported] 를 false 로 두고 우회하지 않는다.
 */
data class DeviceRotationStatus(
    /** 이 플랫폼에서 화면을 돌릴 수 있는가. */
    val supported: Boolean = false,
    /** 돌릴 권한이 있는가. Android 는 `WRITE_SETTINGS` 다. 권한을 요구하지 않는 플랫폼은 true 로 둔다. */
    val permitted: Boolean = true,
    /** 현재 각도에 고정되어 있는가. false 면 센서를 따라 돈다. */
    val locked: Boolean = false,
    /** 지금 각도. 읽을 수 없으면 null. */
    val angle: RotationAngle? = null,
)
