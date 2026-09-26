package io.github.taetae98coding.jarvis.domain.battery

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class ObserveBatteryUseCaseTest {
    @Test
    fun startsLoadingThenFollowsRepository() = runTest {
        val repository = FakeBatteryRepository()
        val state = ObserveBatteryUseCase(repository)(backgroundScope)

        assertEquals(BatteryStatus.Loading, state.value)

        backgroundScope.launch { state.collect {} }
        runCurrent()
        repository.statuses.emit(BatteryStatus.NoBattery)
        runCurrent()

        assertEquals(BatteryStatus.NoBattery, state.value)
    }

    private class FakeBatteryRepository : BatteryRepository {
        val statuses = MutableSharedFlow<BatteryStatus>()

        override fun observeBattery(): Flow<BatteryStatus> = statuses
    }
}
