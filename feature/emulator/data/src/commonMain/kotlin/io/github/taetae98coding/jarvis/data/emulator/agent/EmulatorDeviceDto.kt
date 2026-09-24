package io.github.taetae98coding.jarvis.data.emulator.agent

import io.github.taetae98coding.jarvis.domain.emulator.DeviceConnection
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
    val physical: Boolean = false,
    val running: Boolean = false,
    val asleep: Boolean = false,
    val streamable: Boolean = false,
    val controllable: Boolean = false,
    val launchable: Boolean = false,
    val connection: String? = null,
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
        physical = isPhysical,
        running = isRunning,
        asleep = isAsleep,
        streamable = canStream,
        controllable = canControl,
        launchable = canLaunch,
        connection = connection?.wireName,
    )

private fun EmulatorDeviceDto.toDomain(): EmulatorDevice? {
    val known = EmulatorPlatform.entries.firstOrNull { it.wireName == platform } ?: return null

    return EmulatorDevice(
        id = id,
        name = name,
        platform = known,
        isPhysical = physical,
        isRunning = running,
        isAsleep = asleep,
        // 옛 에이전트에는 없던 필드다. 없으면 false 로 읽혀서 화면이 요청을 막는다. 할 수 없는 일을
        // 할 수 있다고 읽는 것보다 낫다.
        canStream = streamable,
        canControl = controllable,
        canLaunch = launchable,
        // 옛 에이전트에는 없다. 모르는 값이면 null 이라 화면에 아무것도 붙지 않는다.
        connection = DeviceConnection.entries.firstOrNull { it.wireName == connection },
    )
}

// enum 이름을 그대로 쓰지 않는다. 상수 이름을 바꿔도 프로토콜이 깨지지 않게 와이어 이름을 따로 둔다.
private val DeviceConnection.wireName: String
    get() = when (this) {
        DeviceConnection.WIRED -> "wired"
        DeviceConnection.WIRELESS -> "wireless"
    }

// enum 이름을 그대로 쓰면 상수 이름을 바꾸는 순간 프로토콜이 깨진다. 와이어에 나가는 이름은 따로 둔다.
private val EmulatorPlatform.wireName: String
    get() = when (this) {
        EmulatorPlatform.ANDROID -> "android"
        EmulatorPlatform.IOS -> "ios"
    }
