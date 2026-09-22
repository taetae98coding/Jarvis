package io.github.taetae98coding.jarvis.domain.emulator

import kotlinx.coroutines.flow.Flow

class ObserveEmulatorStatusUseCase(
    private val repository: EmulatorRepository,
) {
    operator fun invoke(): Flow<EmulatorStatus> = repository.observeStatus()
}
