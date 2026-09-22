package io.github.taetae98coding.jarvis.data.emulator

import io.github.taetae98coding.jarvis.domain.emulator.EmulatorRepository
import io.github.taetae98coding.jarvis.domain.emulator.EmulatorStatus
import kotlinx.coroutines.flow.Flow

internal class DefaultEmulatorRepository(
    private val dataSource: EmulatorDataSource,
) : EmulatorRepository {
    override fun observeStatus(): Flow<EmulatorStatus> = dataSource.observeStatus()
}
