package io.github.taetae98coding.jarvis.domain.emulator

import kotlinx.coroutines.flow.Flow

class ObserveEmulatorScreenUseCase(
    private val repository: EmulatorRepository,
) {
    operator fun invoke(deviceId: String): Flow<ByteArray?> = repository.observeScreen(deviceId)
}
