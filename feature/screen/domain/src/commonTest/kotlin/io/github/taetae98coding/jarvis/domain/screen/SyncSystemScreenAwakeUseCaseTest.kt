package io.github.taetae98coding.jarvis.domain.screen

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class SyncSystemScreenAwakeUseCaseTest {
    @Test
    fun appliesTheStoredSettingWhenPermitted() = runTest {
        val settings = FakeScreenAwakeSettingsRepository(keepSystemScreenAwake = true)
        val system = FakeSystemScreenAwakeRepository()

        SyncSystemScreenAwakeUseCase(settings, system)()

        assertEquals(listOf(true), system.applied)
    }

    @Test
    fun restoresWhenTheStoredSettingIsOff() = runTest {
        val settings = FakeScreenAwakeSettingsRepository(keepSystemScreenAwake = false)
        val system = FakeSystemScreenAwakeRepository()

        SyncSystemScreenAwakeUseCase(settings, system)()

        assertEquals(listOf(false), system.applied)
    }

    @Test
    fun doesNothingWithoutPermission() = runTest {
        val settings = FakeScreenAwakeSettingsRepository(keepSystemScreenAwake = true)
        val system = FakeSystemScreenAwakeRepository(
            SystemScreenAwakeStatus(supported = true, permitted = false),
        )

        SyncSystemScreenAwakeUseCase(settings, system)()

        assertEquals(emptyList(), system.applied)
    }
}
