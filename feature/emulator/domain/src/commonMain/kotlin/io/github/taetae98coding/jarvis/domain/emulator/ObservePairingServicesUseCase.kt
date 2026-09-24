package io.github.taetae98coding.jarvis.domain.emulator

import kotlinx.coroutines.flow.Flow

class ObservePairingServicesUseCase(
    private val repository: DevicePairingRepository,
) {
    operator fun invoke(): Flow<List<PairingService>?> = repository.observePairingServices()
}
