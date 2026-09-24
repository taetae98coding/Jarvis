package io.github.taetae98coding.jarvis.domain.screen

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SetSystemScreenAwakeNotificationPinnedUseCaseTest {
    @Test
    fun storesTheSettingEvenWithoutPermission() = runTest {
        val repository = FakeSystemScreenAwakeNotificationRepository(
            SystemScreenAwakeNotificationStatus(supported = true, permitted = false),
        )

        SetSystemScreenAwakeNotificationPinnedUseCase(repository)(true)

        assertTrue(repository.status.value.pinned)
    }

    @Test
    fun requestsPermissionWhenTurnedOnWithoutPermission() = runTest {
        val repository = FakeSystemScreenAwakeNotificationRepository(
            SystemScreenAwakeNotificationStatus(supported = true, permitted = false),
        )

        SetSystemScreenAwakeNotificationPinnedUseCase(repository)(true)

        assertEquals(1, repository.permissionRequests)
    }

    @Test
    fun doesNotAskForPermissionWhenTurnedOffOrAlreadyPermitted() = runTest {
        val denied = FakeSystemScreenAwakeNotificationRepository(
            SystemScreenAwakeNotificationStatus(supported = true, permitted = false, pinned = true),
        )
        val permitted = FakeSystemScreenAwakeNotificationRepository()

        SetSystemScreenAwakeNotificationPinnedUseCase(denied)(false)
        SetSystemScreenAwakeNotificationPinnedUseCase(permitted)(true)

        assertEquals(0, denied.permissionRequests)
        assertEquals(0, permitted.permissionRequests)
    }
}
