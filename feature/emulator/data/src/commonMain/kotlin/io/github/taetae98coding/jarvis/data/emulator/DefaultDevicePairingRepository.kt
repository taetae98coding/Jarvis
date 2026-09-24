package io.github.taetae98coding.jarvis.data.emulator

import io.github.taetae98coding.jarvis.domain.emulator.DevicePairingRepository
import io.github.taetae98coding.jarvis.domain.emulator.PairingResult
import io.github.taetae98coding.jarvis.domain.emulator.PairingService
import kotlinx.coroutines.flow.Flow

internal class DefaultDevicePairingRepository(
    private val dataSource: DevicePairingDataSource,
) : DevicePairingRepository {
    override fun observePairingServices(): Flow<List<PairingService>?> = dataSource.observePairingServices()

    override suspend fun pair(service: PairingService, code: String): PairingResult = dataSource.pair(service, code)
}
