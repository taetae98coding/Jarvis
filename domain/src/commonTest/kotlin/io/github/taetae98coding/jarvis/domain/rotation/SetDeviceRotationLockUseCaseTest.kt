package io.github.taetae98coding.jarvis.domain.rotation

import kotlin.test.Test
import kotlin.test.assertEquals

class SetDeviceRotationLockUseCaseTest {
    @Test
    fun changesTheLockWithoutTouchingTheAngle() {
        val repository = FakeDeviceRotationRepository()

        SetDeviceRotationLockUseCase(repository)(true)

        assertEquals(listOf("locked=true"), repository.calls)
    }

    @Test
    fun opensThePermissionScreenWithoutPermission() {
        val repository = FakeDeviceRotationRepository(
            DeviceRotationStatus(supported = true, permitted = false),
        )

        SetDeviceRotationLockUseCase(repository)(false)

        assertEquals(1, repository.permissionRequests)
        assertEquals(emptyList<String>(), repository.calls)
    }
}
