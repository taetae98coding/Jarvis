package io.github.taetae98coding.jarvis.domain.screen

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class ApplyKeepScreenAwakeUseCaseTest {
    @Test
    fun followsTheSetting() = runTest {
        val settings = FakeScreenAwakeSettingsRepository()
        val applied = mutableListOf<Boolean>()
        val useCase = ApplyKeepScreenAwakeUseCase(settings, ScreenAwakeRepository { applied += it })

        backgroundScope.launch { useCase() }
        runCurrent()
        settings.setKeepScreenAwake(true)
        runCurrent()

        assertEquals(listOf(false, true), applied)
    }

    @Test
    fun releasesTheEffectWhenCancelled() = runTest {
        val settings = FakeScreenAwakeSettingsRepository(keepScreenAwake = true)
        val applied = mutableListOf<Boolean>()
        val useCase = ApplyKeepScreenAwakeUseCase(settings, ScreenAwakeRepository { applied += it })

        val job = launch { useCase() }
        runCurrent()
        job.cancel()
        runCurrent()

        assertEquals(listOf(true, false), applied)
    }
}
