package io.github.taetae98coding.jarvis.data.emulator.agent

import io.github.taetae98coding.jarvis.domain.emulator.EmulatorGesture
import io.github.taetae98coding.jarvis.domain.emulator.TouchAction
import kotlinx.serialization.Serializable

/*
 * 제스처의 와이어 포맷. 두 종류를 한 모양에 담고 type 으로 가른다. sealed 계층을 그대로 직렬화하면
 * 판별자 필드 이름이 kotlinx.serialization 의 설정에 묶여서, 사람이 curl 로 흉내 내기 어려워진다.
 */

@Serializable
private data class EmulatorGestureDto(
    val id: String,
    val type: String,
    val action: String? = null,
    val x: Int = 0,
    val y: Int = 0,
    val width: Int = 0,
    val height: Int = 0,
)

internal class HostAgentGesture(
    val deviceId: String,
    val gesture: EmulatorGesture,
)

internal fun encodeEmulatorGesture(deviceId: String, gesture: EmulatorGesture): String =
    HostAgentJson.encodeToString(
        EmulatorGestureDto(
            id = deviceId,
            type = when (gesture) {
                is EmulatorGesture.Touch -> TouchType
                is EmulatorGesture.Hover -> HoverType
            },
            action = (gesture as? EmulatorGesture.Touch)?.action?.name?.lowercase(),
            x = gesture.x,
            y = gesture.y,
            width = gesture.frameWidth,
            height = gesture.frameHeight,
        ),
    )

internal fun decodeEmulatorGesture(body: String): HostAgentGesture? {
    val dto = runCatching { HostAgentJson.decodeFromString<EmulatorGestureDto>(body) }.getOrNull() ?: return null

    val gesture = when (dto.type) {
        TouchType -> EmulatorGesture.Touch(
            action = TouchAction.entries.firstOrNull { it.name.equals(dto.action, ignoreCase = true) } ?: return null,
            x = dto.x,
            y = dto.y,
            frameWidth = dto.width,
            frameHeight = dto.height,
        )

        HoverType -> EmulatorGesture.Hover(x = dto.x, y = dto.y, frameWidth = dto.width, frameHeight = dto.height)

        // 예전 클라이언트의 tap·swipe 도 여기로 온다. 이벤트 단위 채널에 맞지 않으니 받지 않는다.
        else -> return null
    }

    return HostAgentGesture(deviceId = dto.id, gesture = gesture)
}

private const val TouchType = "touch"
private const val HoverType = "hover"
