package io.github.taetae98coding.jarvis.domain.screen

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class ApplySystemScreenAwakeUseCaseTest {
    @Test
    fun appliesTheSettingWhenPermitted() = runTest {
        val settings = FakeScreenAwakeSettingsRepository()
        val system = FakeSystemScreenAwakeRepository()
        backgroundScope.launch { ApplySystemScreenAwakeUseCase(settings, system)() }
        runCurrent()

        settings.setKeepSystemScreenAwake(true)
        runCurrent()

        assertEquals(listOf(false, true), system.applied)
    }

    @Test
    fun doesNothingWithoutPermission() = runTest {
        val settings = FakeScreenAwakeSettingsRepository(keepSystemScreenAwake = true)
        val system = FakeSystemScreenAwakeRepository(
            SystemScreenAwakeStatus(supported = true, permitted = false),
        )
        backgroundScope.launch { ApplySystemScreenAwakeUseCase(settings, system)() }
        runCurrent()

        // 되돌릴 수 없는 상태라 끄는 것조차 시도하지 않는다.
        settings.setKeepSystemScreenAwake(false)
        runCurrent()

        assertEquals(emptyList(), system.applied)
    }

    @Test
    fun appliesWhenPermissionArrivesLater() = runTest {
        val settings = FakeScreenAwakeSettingsRepository(keepSystemScreenAwake = true)
        val system = FakeSystemScreenAwakeRepository(
            SystemScreenAwakeStatus(supported = true, permitted = false),
        )
        backgroundScope.launch { ApplySystemScreenAwakeUseCase(settings, system)() }
        runCurrent()

        system.status.value = SystemScreenAwakeStatus(supported = true, permitted = true)
        runCurrent()

        assertEquals(listOf(true), system.applied)
    }
}
