package io.github.taetae98coding.jarvis.data.emulator.agent

import io.github.taetae98coding.jarvis.domain.emulator.EmulatorDevice
import io.github.taetae98coding.jarvis.domain.emulator.EmulatorPlatform
import kotlinx.serialization.Serializable

/*
 * 목록의 와이어 포맷. 개수와 마찬가지로 도메인 모델과 따로 둔다.
 */

@Serializable
private data class EmulatorDeviceDto(
    val id: String,
    val name: String,
    val platform: String,
    val running: Boolean = false,
    val controllable: Boolean = false,
)

@Serializable
private data class EmulatorDevicesDto(
    val devices: List<EmulatorDeviceDto> = emptyList(),
)

internal fun encodeEmulatorDevices(devices: List<EmulatorDevice>): String =
    HostAgentJson.encodeToString(EmulatorDevicesDto(devices.map(EmulatorDevice::toDto)))

internal fun decodeEmulatorDevices(body: String): List<EmulatorDevice>? =
    runCatching { HostAgentJson.decodeFromString<EmulatorDevicesDto>(body) }
        .getOrNull()
        ?.devices
        // 모르는 플랫폼 이름은 통째로 버린다. 목록에 정체를 알 수 없는 줄을 남기는 것보다 낫다.
        ?.mapNotNull(EmulatorDeviceDto::toDomain)

private fun EmulatorDevice.toDto() =
    EmulatorDeviceDto(
        id = id,
        name = name,
        platform = platform.wireName,
        running = isRunning,
        controllable = canControl,
    )

private fun EmulatorDeviceDto.toDomain(): EmulatorDevice? {
    val known = EmulatorPlatform.entries.firstOrNull { it.wireName == platform } ?: return null

    return EmulatorDevice(
        id = id,
        name = name,
        platform = known,
        isRunning = running,
        canControl = controllable,
    )
}

// enum 이름을 그대로 쓰면 상수 이름을 바꾸는 순간 프로토콜이 깨진다. 와이어에 나가는 이름은 따로 둔다.
private val EmulatorPlatform.wireName: String
    get() = when (this) {
        EmulatorPlatform.ANDROID -> "android"
        EmulatorPlatform.IOS -> "ios"
    }
