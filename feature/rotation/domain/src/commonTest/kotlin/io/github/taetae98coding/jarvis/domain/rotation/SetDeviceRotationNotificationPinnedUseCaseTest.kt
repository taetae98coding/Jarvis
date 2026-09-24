package io.github.taetae98coding.jarvis.domain.rotation

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SetDeviceRotationNotificationPinnedUseCaseTest {
    @Test
    fun storesTheSettingEvenWithoutPermission() = runTest {
        val repository = FakeDeviceRotationNotificationRepository(
            DeviceRotationNotificationStatus(supported = true, permitted = false),
        )

        SetDeviceRotationNotificationPinnedUseCase(repository)(true)

        assertTrue(repository.status.value.pinned)
    }

    @Test
    fun requestsPermissionWhenTurnedOnWithoutPermission() = runTest {
        val repository = FakeDeviceRotationNotificationRepository(
            DeviceRotationNotificationStatus(supported = true, permitted = false),
        )

        SetDeviceRotationNotificationPinnedUseCase(repository)(true)

        assertEquals(1, repository.permissionRequests)
    }

    @Test
    fun doesNotAskForPermissionWhenTurnedOffOrAlreadyPermitted() = runTest {
        val denied = FakeDeviceRotationNotificationRepository(
            DeviceRotationNotificationStatus(supported = true, permitted = false, pinned = true),
        )
        val permitted = FakeDeviceRotationNotificationRepository()

        SetDeviceRotationNotificationPinnedUseCase(denied)(false)
        SetDeviceRotationNotificationPinnedUseCase(permitted)(true)

        assertEquals(0, denied.permissionRequests)
        assertEquals(0, permitted.permissionRequests)
    }
}
