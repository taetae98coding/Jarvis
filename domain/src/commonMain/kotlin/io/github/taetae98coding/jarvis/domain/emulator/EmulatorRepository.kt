package io.github.taetae98coding.jarvis.domain.emulator

import kotlinx.coroutines.flow.Flow

fun interface EmulatorRepository {
    fun observeStatus(): Flow<EmulatorStatus>
}
