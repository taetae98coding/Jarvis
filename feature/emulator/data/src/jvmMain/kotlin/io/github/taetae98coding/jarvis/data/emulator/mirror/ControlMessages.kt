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
    private const val TypeInjectKeycode = 0
    private const val TypeInjectText = 1
    private const val TypeInjectTouchEvent = 2
    private const val TypeSetClipboard = 9

    // 서버의 ControlMessageReader.INJECT_TEXT_MAX_LENGTH. 넘으면 서버가 메시지를 버린다.
    const val InjectTextMaxBytes = 300

    // android.view.KeyEvent 의 ACTION_DOWN·ACTION_UP.
    const val KeyActionDown = 0
    const val KeyActionUp = 1

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

    /** type(1) action(1) keycode(4) repeat(4) metaState(4). */
    fun encodeKeycode(action: Int, keycode: Int): ByteArray =
        ByteBuffer.allocate(14)
            .put(TypeInjectKeycode.toByte())
            .put(action.toByte())
            .putInt(keycode)
            .putInt(0)
            .putInt(0)
            .array()

    /**
     * type(1) length(4) utf8. 서버는 기기의 KeyCharacterMap 으로 키를 만들어 넣으므로 ASCII 만 믿을 수 있다.
     * [text] 는 [InjectTextMaxBytes] 를 넘지 않아야 한다.
     */
    fun encodeText(text: String): ByteArray {
        val bytes = text.encodeToByteArray()
        require(bytes.size <= InjectTextMaxBytes) { "INJECT_TEXT 는 ${InjectTextMaxBytes}바이트까지다" }

        return ByteBuffer.allocate(5 + bytes.size).put(TypeInjectText.toByte()).putInt(bytes.size).put(bytes).array()
    }

    /**
     * type(1) sequence(8) paste(1) length(4) utf8. [paste] 면 서버가 클립보드를 바꾼 뒤 KEYCODE_PASTE 를 넣는다.
     * sequence 0(SEQUENCE_INVALID)이면 서버가 확인 응답을 보내지 않는다 — 제어 소켓을 읽는 곳이 없다.
     */
    fun encodeSetClipboard(text: String, paste: Boolean): ByteArray {
        val bytes = text.encodeToByteArray()

        return ByteBuffer.allocate(14 + bytes.size)
            .put(TypeSetClipboard.toByte())
            .putLong(0)
            .put((if (paste) 1 else 0).toByte())
            .putInt(bytes.size)
            .put(bytes)
            .array()
    }

    private fun putPosition(buffer: ByteBuffer, gesture: EmulatorGesture) {
        buffer.putInt(gesture.x)
        buffer.putInt(gesture.y)
        buffer.putShort(gesture.frameWidth.toShort())
        buffer.putShort(gesture.frameHeight.toShort())
    }
}
