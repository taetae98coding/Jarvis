package io.github.taetae98coding.jarvis.domain.emulator

enum class EmulatorPlatform {
    ANDROID,
    IOS,
}

/**
 * 개발자 머신이 알고 있는 기기 하나. 가상 기기(AVD·시뮬레이터)와 그 머신에 연결된 실물 기기가
 * 같은 목록에 섞인다.
 *
 * [id] 는 화면·제스처·실행 요청에 그대로 쓰는 식별자다. 종류마다 모양이 다르고, :data 가 그 모양만
 * 보고 어느 도구를 부를지 가른다. 실행 중인 Android 기기는 adb 시리얼, iOS 시뮬레이터는 UDID,
 * 아직 켜지지 않은 AVD 는 `avd:<이름>`, 실물 iOS 기기는 `ios:<UDID>` 다. 꺼진 AVD 의 id 는 켜지는
 * 순간 시리얼로 바뀌므로 목록을 다시 받기 전까지의 식별자로만 쓴다.
 *
 * [isRunning] 은 가상 기기에는 부팅되어 있다는 뜻이고 실물 기기에는 연결되어 있다는 뜻이다.
 * 나머지 플래그는 그것과 별개다. 실물 iOS 기기는 연결되어 있어도 화면을 찍을 수 없고, 꺼진
 * AVD 는 켤 수는 있어도 찍을 수 없다.
 *
 * [isAsleep] 은 연결은 됐는데 화면이 꺼져 있다는 뜻이다. 그 상태로도 화면은 찍히지만 검은 그림만
 * 나온다. 깨울 수 있는지는 따로 두지 않는다 — 깨우기와 제스처가 같은 명령(`input`)이라 [canControl]
 * 이 그대로 답이 된다.
 *
 * [connection] 은 실물 기기가 유선(USB)인지 무선(네트워크)인지다. 가상 기기와, 알아낼 수 없는 경우는
 * null 이다(docs/common/emulator-control.html#connection).
 */
data class EmulatorDevice(
    val id: String,
    val name: String,
    val platform: EmulatorPlatform,
    val isPhysical: Boolean = false,
    val isRunning: Boolean = false,
    val isAsleep: Boolean = false,
    val canStream: Boolean = false,
    val canControl: Boolean = false,
    val canLaunch: Boolean = false,
    val connection: DeviceConnection? = null,
)
