package io.github.taetae98coding.jarvis.domain.emulator

/**
 * 기기에 보낼 입력 이벤트 하나. 좌표는 지금 보고 있는 프레임의 픽셀이고, 그 프레임의 크기를 함께 싣는다.
 * 기기 쪽 에이전트가 프레임 크기를 디스플레이 크기로 되돌리며, 크기가 지금 영상과 다르면(회전 직후)
 * 이벤트를 버린다. 그래서 디스플레이 해상도는 여기서 알 필요가 없다(docs/common/device-mirroring.html).
 */
sealed interface EmulatorGesture {
    val x: Int
    val y: Int
    val frameWidth: Int
    val frameHeight: Int

    /** 손가락 하나. 누른 순간·끄는 동안·뗀 순간이 각각 하나씩 온다. */
    data class Touch(
        val action: TouchAction,
        override val x: Int,
        override val y: Int,
        override val frameWidth: Int,
        override val frameHeight: Int,
    ) : EmulatorGesture

    /** 마우스가 누르지 않고 지나간다. 기기 안의 UI 가 호버 상태를 그린다. */
    data class Hover(
        override val x: Int,
        override val y: Int,
        override val frameWidth: Int,
        override val frameHeight: Int,
    ) : EmulatorGesture
}

enum class TouchAction {
    DOWN,
    MOVE,
    UP,
    CANCEL,
}
