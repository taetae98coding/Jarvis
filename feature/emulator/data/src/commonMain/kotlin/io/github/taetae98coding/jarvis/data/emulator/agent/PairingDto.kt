package io.github.taetae98coding.jarvis.data.emulator.agent

import io.github.taetae98coding.jarvis.domain.emulator.PairingResult
import io.github.taetae98coding.jarvis.domain.emulator.PairingService
import kotlinx.serialization.Serializable

/*
 * 페어링 대기 기기 목록, 페어링 요청, 그 결과의 와이어 포맷.
 */

@Serializable
private data class PairingServiceDto(
    val name: String,
    val host: String,
    val port: Int,
)

@Serializable
private data class PairingServicesDto(
    val services: List<PairingServiceDto> = emptyList(),
)

@Serializable
private data class PairRequestDto(
    val name: String,
    val host: String,
    val port: Int,
    val code: String,
)

@Serializable
private data class PairResultDto(
    val paired: Boolean,
    val connected: Boolean = false,
    val reason: String? = null,
)

internal class PairRequest(
    val service: PairingService,
    val code: String,
)

internal fun encodePairingServices(services: List<PairingService>): String =
    HostAgentJson.encodeToString(PairingServicesDto(services.map { PairingServiceDto(it.name, it.host, it.port) }))

internal fun decodePairingServices(body: String): List<PairingService>? =
    runCatching { HostAgentJson.decodeFromString<PairingServicesDto>(body) }
        .getOrNull()
        ?.services
        ?.map { PairingService(name = it.name, host = it.host, port = it.port) }

internal fun encodePairRequest(service: PairingService, code: String): String =
    HostAgentJson.encodeToString(PairRequestDto(service.name, service.host, service.port, code))

internal fun decodePairRequest(body: String): PairRequest? =
    runCatching { HostAgentJson.decodeFromString<PairRequestDto>(body) }
        .getOrNull()
        ?.let { PairRequest(PairingService(name = it.name, host = it.host, port = it.port), it.code) }

internal fun encodePairResult(result: PairingResult): String =
    HostAgentJson.encodeToString(
        when (result) {
            is PairingResult.Paired -> PairResultDto(paired = true, connected = result.isConnected)
            is PairingResult.Failed -> PairResultDto(paired = false, reason = result.reason)
        },
    )

internal fun decodePairResult(body: String): PairingResult? =
    runCatching { HostAgentJson.decodeFromString<PairResultDto>(body) }
        .getOrNull()
        ?.let { dto ->
            if (dto.paired) {
                PairingResult.Paired(isConnected = dto.connected)
            } else {
                PairingResult.Failed(dto.reason ?: UnknownPairingFailure)
            }
        }

internal const val UnknownPairingFailure = "adb 가 이유를 알려 주지 않았습니다."
