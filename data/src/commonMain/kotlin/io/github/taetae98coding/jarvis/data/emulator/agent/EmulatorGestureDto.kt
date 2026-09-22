package io.github.taetae98coding.jarvis.data.emulator.agent

import io.github.taetae98coding.jarvis.domain.emulator.EmulatorGesture
import kotlinx.serialization.Serializable

/*
 * 제스처의 와이어 포맷. 두 종류를 한 모양에 담고 type 으로 가른다. sealed 계층을 그대로 직렬화하면
 * 판별자 필드 이름이 kotlinx.serialization 의 설정에 묶여서, 사람이 curl 로 흉내 내기 어려워진다.
 */

@Serializable
private data class EmulatorGestureDto(
    val id: String,
    val type: String,
    val x: Int = 0,
    val y: Int = 0,
    val toX: Int = 0,
    val toY: Int = 0,
    val durationMillis: Long = 0,
)

internal class HostAgentGesture(
    val deviceId: String,
    val gesture: EmulatorGesture,
)

internal fun encodeEmulatorGesture(deviceId: String, gesture: EmulatorGesture): String =
    HostAgentJson.encodeToString(
        when (gesture) {
            is EmulatorGesture.Tap -> EmulatorGestureDto(
                id = deviceId,
                type = TapType,
                x = gesture.x,
                y = gesture.y,
            )

            is EmulatorGesture.Swipe -> EmulatorGestureDto(
                id = deviceId,
                type = SwipeType,
                x = gesture.fromX,
                y = gesture.fromY,
                toX = gesture.toX,
                toY = gesture.toY,
                durationMillis = gesture.durationMillis,
            )
        },
    )

internal fun decodeEmulatorGesture(body: String): HostAgentGesture? {
    val dto = runCatching { HostAgentJson.decodeFromString<EmulatorGestureDto>(body) }.getOrNull() ?: return null

    val gesture = when (dto.type) {
        TapType -> EmulatorGesture.Tap(x = dto.x, y = dto.y)

        SwipeType -> EmulatorGesture.Swipe(
            fromX = dto.x,
            fromY = dto.y,
            toX = dto.toX,
            toY = dto.toY,
            durationMillis = dto.durationMillis,
        )

        else -> return null
    }

    return HostAgentGesture(deviceId = dto.id, gesture = gesture)
}

private const val TapType = "tap"
private const val SwipeType = "swipe"
