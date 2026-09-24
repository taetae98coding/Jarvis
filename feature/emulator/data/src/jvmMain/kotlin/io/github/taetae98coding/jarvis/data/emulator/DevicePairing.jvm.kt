package io.github.taetae98coding.jarvis.data.emulator

import io.github.taetae98coding.jarvis.data.state.observeByPolling
import io.github.taetae98coding.jarvis.domain.emulator.PairingResult
import io.github.taetae98coding.jarvis.domain.emulator.PairingService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.seconds

internal actual val devicePairingDataSource: DevicePairingDataSource = object : DevicePairingDataSource {
    // 페어링 화면이 떠 있는 동안만 본다. 기기 목록과 달리 에이전트가 미리 구독해 두지 않는다.
    override fun observePairingServices(): Flow<List<PairingService>?> =
        observeByPolling(interval = PairingServicePollInterval) {
            withContext(Dispatchers.IO) { listPairingServices() }
        }

    override suspend fun pair(service: PairingService, code: String): PairingResult =
        withContext(Dispatchers.IO) { pairDevice(service, code) }
}

// SDK 가 없거나 mDNS 를 물을 수 없으면 null 이다. 0개와 다르다.
private fun listPairingServices(): List<PairingService>? {
    val sdk = androidSdkDirectory() ?: return null

    return runCommand(listOf(adbBinary(sdk), "mdns", "services"))?.let(::parsePairingServices)
}

private suspend fun pairDevice(service: PairingService, code: String): PairingResult {
    // 에이전트로 들어온 값이 그대로 인자가 된다. `-` 로 시작하는 주소는 adb 가 옵션으로 읽는다.
    if (!isPairableAddress(service)) return PairingResult.Failed("주소가 올바르지 않습니다: ${service.address}")

    val sdk = androidSdkDirectory() ?: return PairingResult.Failed("Android SDK 를 찾을 수 없습니다.")
    val adb = adbBinary(sdk)

    val output = runCommandOutput(listOf(adb, "pair", pairTarget(service), code), PairTimeoutSeconds)
        ?: return PairingResult.Failed("adb pair 가 응답하지 않습니다.")

    val guid = parsePairedGuid(output) ?: return PairingResult.Failed(parsePairFailure(output))

    // adb 서버가 페어링 직후 스스로 연결한다. `adb connect` 를 더 부르면 같은 기기가 두 시리얼로 붙는다.
    repeat(ConnectAttempts) {
        val devices = runCommand(listOf(adb, "devices"))?.let(::parseAttachedSerials).orEmpty()

        if (devices.any { it.startsWith(guid) || it.startsWith("${service.host}:") }) {
            return PairingResult.Paired(isConnected = true)
        }

        delay(ConnectPollInterval)
    }

    return PairingResult.Paired(isConnected = false)
}

private fun pairTarget(service: PairingService): String =
    if (':' in service.host) "[${service.host}]:${service.port}" else service.address

private fun isPairableAddress(service: PairingService): Boolean =
    PairableHost.matches(service.host) && !service.host.startsWith('-') && service.port in 1..65535

private val PairableHost = Regex("""[0-9A-Za-z.:\-]+""")

private const val PairTimeoutSeconds = 15L
private const val ConnectAttempts = 10
private val ConnectPollInterval = 1.seconds

private const val PairingServiceType = "_adb-tls-pairing._tcp"

/**
 * `adb mdns services` 는 머리 줄 뒤에 `<이름>\t<종류>\t<IPv4>:<포트>` 를 한 줄씩 준다. 이름에는
 * 공백이 들어갈 수 있어서(`adb-… (2)`) 탭으로만 자른다. 옛 버전은 종류 끝에 점을 붙였다.
 */
internal fun parsePairingServices(output: String): List<PairingService> =
    output.lineSequence()
        .map { it.split('\t') }
        .filter { it.size >= 3 && it[1].trim().removeSuffix(".") == PairingServiceType }
        .mapNotNull { columns ->
            val address = columns[2].trim()
            val separator = address.lastIndexOf(':')
            val port = address.substring(separator + 1).toIntOrNull()

            if (separator <= 0 || port == null) {
                null
            } else {
                PairingService(
                    name = columns[0].trim(),
                    host = address.substring(0, separator).removeSurrounding("[", "]"),
                    port = port,
                )
            }
        }
        .distinct()
        .toList()

/**
 * 성공은 `Successfully paired to <주소> [guid=<guid>]` 한 줄로만 가린다. 종료 코드는 버전마다 다르다 —
 * android-14 까지의 서버는 실패해도 0 을 준다.
 */
internal fun parsePairedGuid(output: String): String? =
    PairedLine.find(output)?.groupValues?.get(1)

// 15 부터는 `error: Failed: …` 로, 그 전에는 `Failed: …` 로 온다. 둘 다 마지막 줄이 이유다.
internal fun parsePairFailure(output: String): String =
    output.lineSequence()
        .map(String::trim)
        .lastOrNull(String::isNotEmpty)
        ?.removePrefix("error: ")
        ?: "adb pair 가 실패했습니다."

private val PairedLine = Regex("""^Successfully paired to \S+ \[guid=([^\]]+)]""", RegexOption.MULTILINE)
