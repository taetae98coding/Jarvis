package io.github.taetae98coding.jarvis.data.emulator.agent

import kotlinx.serialization.Serializable

/**
 * 에이전트가 들고 있는 로그 중 `after` 순번 뒤의 줄. [next] 는 다음 요청의 `after` 다. 순번은 에이전트 전체에서 늘기만 해서, 에이전트가
 * 그 기기의 읽기를 새로 시작해도 이전 커서가 새 줄을 가리지 않는다(docs/common/device-logcat.html#implementation).
 */
@Serializable
internal data class DeviceLogChunk(
    val next: Long,
    val lines: List<String>,
)

internal fun encodeDeviceLogChunk(chunk: DeviceLogChunk): String = HostAgentJson.encodeToString(chunk)

internal fun decodeDeviceLogChunk(body: String): DeviceLogChunk? =
    runCatching { HostAgentJson.decodeFromString<DeviceLogChunk>(body) }.getOrNull()
