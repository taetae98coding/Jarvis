package io.github.taetae98coding.jarvis.ui.emulator

import io.github.taetae98coding.jarvis.domain.emulator.EmulatorStatus
import io.github.taetae98coding.jarvis.domain.emulator.EmulatorSummary
import io.github.taetae98coding.jarvis.domain.emulator.ObserveEmulatorStatusUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class EmulatorStatusViewModelTest {
    // EmulatorDevicesViewModelTest 와 같은 이유로 runTest 밖에서 되돌린다.
    @AfterTest
    fun resetMainDispatcher() {
        Dispatchers.resetMain()
    }

    @Test
    fun countsOnlyWhileTheCardIsCollected() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val emulator = FakeEmulatorRepository(android = EmulatorSummary(total = 2, running = 1))
        val viewModel = EmulatorStatusViewModel(ObserveEmulatorStatusUseCase(emulator))
        runCurrent()
        assertEquals(0, emulator.status.subscriptionCount.value)

        val card = backgroundScope.launch { viewModel.status.collect() }
        runCurrent()
        assertEquals(1, emulator.status.subscriptionCount.value)
        assertEquals(EmulatorStatus(android = EmulatorSummary(total = 2, running = 1)), viewModel.status.value)

        card.cancel()
        runCurrent()
        assertEquals(0, emulator.status.subscriptionCount.value)
    }
}
