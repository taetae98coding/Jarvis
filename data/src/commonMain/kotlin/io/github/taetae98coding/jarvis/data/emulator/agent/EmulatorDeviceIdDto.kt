package io.github.taetae98coding.jarvis.data.emulator.agent

import kotlinx.serialization.Serializable

/*
 * 기기 하나를 가리키는 것이 전부인 요청의 와이어 포맷. 실행과 화면 깨우기가 같은 모양을 쓴다.
 */

@Serializable
private data class EmulatorDeviceIdDto(
    val id: String,
)

internal fun encodeEmulatorDeviceId(deviceId: String): String =
    HostAgentJson.encodeToString(EmulatorDeviceIdDto(deviceId))

internal fun decodeEmulatorDeviceId(body: String): String? =
    runCatching { HostAgentJson.decodeFromString<EmulatorDeviceIdDto>(body) }.getOrNull()?.id
