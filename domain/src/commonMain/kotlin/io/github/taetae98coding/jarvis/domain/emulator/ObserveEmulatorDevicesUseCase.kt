package io.github.taetae98coding.jarvis.domain.emulator

import kotlinx.coroutines.flow.Flow

class ObserveEmulatorDevicesUseCase(
    private val repository: EmulatorRepository,
) {
    operator fun invoke(): Flow<List<EmulatorDevice>> = repository.observeDevices()
}
