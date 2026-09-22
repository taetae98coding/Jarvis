package io.github.taetae98coding.jarvis.domain.emulator

enum class EmulatorPlatform {
    ANDROID,
    IOS,
}

/**
 * 개발자 머신의 가상 기기 하나.
 *
 * [id] 는 화면과 제스처 요청에 그대로 쓰는 식별자다. 실행 중인 Android 에뮬레이터는 adb 시리얼,
 * iOS 시뮬레이터는 UDID, 아직 켜지지 않은 AVD 는 `avd:<이름>` 이다. 마지막 것은 켜지는 순간
 * 시리얼로 바뀌므로 목록을 다시 받기 전까지의 식별자로만 쓴다.
 *
 * [canControl] 은 제스처를 받을 수 있는지다. iOS 시뮬레이터에는 입력을 주입하는 공개 도구가 없어서
 * 실행 중이어도 false 다.
 */
data class EmulatorDevice(
    val id: String,
    val name: String,
    val platform: EmulatorPlatform,
    val isRunning: Boolean = false,
    val canControl: Boolean = false,
)
