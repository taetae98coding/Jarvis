package io.github.taetae98coding.jarvis.domain.rotation

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class SetDeviceRotationLockUseCaseTest {
    @Test
    fun changesTheLockWithoutTouchingTheAngle() = runTest {
        val repository = FakeDeviceRotationRepository()

        SetDeviceRotationLockUseCase(repository)(true)

        assertEquals(listOf("locked=true"), repository.calls)
    }

    @Test
    fun opensThePermissionScreenWithoutPermission() = runTest {
        val repository = FakeDeviceRotationRepository(
            DeviceRotationStatus(supported = true, permitted = false),
        )

        SetDeviceRotationLockUseCase(repository)(false)

        assertEquals(1, repository.permissionRequests)
        assertEquals(emptyList<String>(), repository.calls)
    }
}
