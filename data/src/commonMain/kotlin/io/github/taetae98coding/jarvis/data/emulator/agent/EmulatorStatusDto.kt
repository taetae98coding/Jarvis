package io.github.taetae98coding.jarvis.data.emulator.agent

import io.github.taetae98coding.jarvis.domain.emulator.EmulatorStatus
import io.github.taetae98coding.jarvis.domain.emulator.EmulatorSummary
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/*
 * 와이어 포맷은 도메인 모델과 따로 둔다. JSON 필드 이름이 바뀌어도 화면과 규칙은 그대로다.
 */

@Serializable
private data class EmulatorSummaryDto(
    val total: Int = 0,
    val running: Int = 0,
)

@Serializable
private data class EmulatorStatusDto(
    val android: EmulatorSummaryDto? = null,
    val ios: EmulatorSummaryDto? = null,
)

internal fun encodeEmulatorStatus(status: EmulatorStatus): String =
    HostAgentJson.encodeToString(status.toDto())

internal fun decodeEmulatorStatus(body: String): EmulatorStatus? =
    runCatching { HostAgentJson.decodeFromString<EmulatorStatusDto>(body) }
        .getOrNull()
        ?.toDomain()

private fun EmulatorStatus.toDto() =
    EmulatorStatusDto(android = android?.toDto(), ios = ios?.toDto())

private fun EmulatorSummary.toDto() = EmulatorSummaryDto(total = total, running = running)

private fun EmulatorStatusDto.toDomain() =
    EmulatorStatus(android = android?.toDomain(), ios = ios?.toDomain())

private fun EmulatorSummaryDto.toDomain() = EmulatorSummary(total = total, running = running)

private val HostAgentJson = Json {
    // 에이전트와 클라이언트의 버전이 어긋날 수 있다. 서버가 필드를 더해도 옛 클라이언트가 읽을 수
    // 있도록 모르는 키는 넘긴다.
    ignoreUnknownKeys = true
    // 0개를 생략하면 응답만 보고는 "0개" 와 "셀 수 없음(null)" 을 가릴 수 없다. 사람이 curl 로 들여다볼
    // 엔드포인트라 값을 다 적는다.
    encodeDefaults = true
}
