package io.github.taetae98coding.jarvis.data.emulator.mirror

import io.github.taetae98coding.jarvis.domain.emulator.EmulatorGesture
import io.github.taetae98coding.jarvis.domain.emulator.TouchAction
import java.nio.ByteBuffer

/**
 * scrcpy 제어 소켓의 터치 이벤트 메시지(32바이트, 빅엔디언). 형식은 v4.1 의
 * `control/ControlMessageReader.java#parseInjectTouchEvent` 에서 읽었다.
 *
 * type(1) action(1) pointerId(8) x(4) y(4) frameW(2) frameH(2) pressure(2) actionButton(4) buttons(4)
 *
 * 손가락과 마우스를 pointerId 로 가른다. 서버의 `Controller#injectTouch` 는 pointerId 가 마우스(-1)이고
 * action 이 HOVER_MOVE 일 때만 SOURCE_MOUSE·TOOL_TYPE_MOUSE 로 주입한다 — 그래야 기기 안 UI 가 호버로 반응한다.
 */
internal object ControlMessages {
    private const val TypeInjectTouchEvent = 2

    // android.view.MotionEvent 의 ACTION 상수. 서버가 그대로 MotionEvent 로 만든다.
    private const val ActionDown = 0
    private const val ActionUp = 1
    private const val ActionMove = 2
    private const val ActionCancel = 3
    private const val ActionHoverMove = 7

    // 서버의 POINTER_ID_MOUSE(-1), POINTER_ID_GENERIC_FINGER(-2).
    private const val PointerIdMouse = -1L
    private const val PointerIdFinger = -2L

    private const val PressureDown = 0xFFFF // u16 고정소수점에서 1.0
    private const val PressureUp = 0

    fun encode(gesture: EmulatorGesture): ByteArray {
        val buffer = ByteBuffer.allocate(32)
        buffer.put(TypeInjectTouchEvent.toByte())

        when (gesture) {
            is EmulatorGesture.Touch -> {
                buffer.put(
                    when (gesture.action) {
                        TouchAction.DOWN -> ActionDown
                        TouchAction.MOVE -> ActionMove
                        TouchAction.UP -> ActionUp
                        TouchAction.CANCEL -> ActionCancel
                    }.toByte(),
                )
                buffer.putLong(PointerIdFinger)
                putPosition(buffer, gesture)
                // 뗄 때는 압력 0. 누르는 동안은 1.0.
                buffer.putShort((if (gesture.action == TouchAction.UP) PressureUp else PressureDown).toShort())
            }

            is EmulatorGesture.Hover -> {
                buffer.put(ActionHoverMove.toByte())
                buffer.putLong(PointerIdMouse)
                putPosition(buffer, gesture)
                buffer.putShort(PressureUp.toShort())
            }
        }

        buffer.putInt(0) // actionButton
        buffer.putInt(0) // buttons

        return buffer.array()
    }

    private fun putPosition(buffer: ByteBuffer, gesture: EmulatorGesture) {
        buffer.putInt(gesture.x)
        buffer.putInt(gesture.y)
        buffer.putShort(gesture.frameWidth.toShort())
        buffer.putShort(gesture.frameHeight.toShort())
    }
}
