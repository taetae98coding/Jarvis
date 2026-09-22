package io.github.taetae98coding.jarvis.domain.screen

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SetKeepSystemScreenAwakeUseCaseTest {
    @Test
    fun storesTheSettingEvenWithoutPermission() {
        val settings = FakeScreenAwakeSettingsRepository()
        val system = FakeSystemScreenAwakeRepository(
            SystemScreenAwakeStatus(supported = true, permitted = false),
        )

        SetKeepSystemScreenAwakeUseCase(settings, system)(true)

        assertTrue(settings.keepSystemScreenAwake.value)
    }

    @Test
    fun opensThePermissionScreenWhenTurnedOnWithoutPermission() {
        val system = FakeSystemScreenAwakeRepository(
            SystemScreenAwakeStatus(supported = true, permitted = false),
        )

        SetKeepSystemScreenAwakeUseCase(FakeScreenAwakeSettingsRepository(), system)(true)

        assertEquals(1, system.permissionRequests)
    }

    @Test
    fun doesNotAskForPermissionWhenTurnedOff() {
        val system = FakeSystemScreenAwakeRepository(
            SystemScreenAwakeStatus(supported = true, permitted = false),
        )

        SetKeepSystemScreenAwakeUseCase(FakeScreenAwakeSettingsRepository(), system)(false)

        assertEquals(0, system.permissionRequests)
    }
}
