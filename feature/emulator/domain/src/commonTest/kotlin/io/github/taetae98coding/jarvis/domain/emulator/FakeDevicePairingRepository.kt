package io.github.taetae98coding.jarvis.domain.emulator

import kotlinx.coroutines.flow.MutableStateFlow

internal class FakeDevicePairingRepository(
    services: List<PairingService>? = emptyList(),
    private val result: PairingResult = PairingResult.Paired(isConnected = true),
) : DevicePairingRepository {
    val services = MutableStateFlow(services)

    val paired = mutableListOf<Pair<PairingService, String>>()

    override fun observePairingServices() = services

    override suspend fun pair(service: PairingService, code: String): PairingResult {
        paired += service to code
        return result
    }
}

internal val CodeService = PairingService(name = "adb-R54T202XEHN-Y2yH0N", host = "172.30.1.47", port = 37123)
