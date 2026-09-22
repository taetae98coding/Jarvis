package io.github.taetae98coding.jarvis.domain.emulator

/**
 * 기기에 보낼 입력. 좌표는 기기 디스플레이 픽셀이다. 화면에 그려진 크기가 아니라 프레임 해상도를
 * 기준으로 :ui 가 미리 변환해서 준다.
 */
sealed interface EmulatorGesture {
    data class Tap(
        val x: Int,
        val y: Int,
    ) : EmulatorGesture

    data class Swipe(
        val fromX: Int,
        val fromY: Int,
        val toX: Int,
        val toY: Int,
        val durationMillis: Long,
    ) : EmulatorGesture
}
