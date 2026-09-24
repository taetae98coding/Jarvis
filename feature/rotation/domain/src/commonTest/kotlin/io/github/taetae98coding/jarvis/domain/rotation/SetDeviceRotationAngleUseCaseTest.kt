package io.github.taetae98coding.jarvis.domain.rotation

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class SetDeviceRotationAngleUseCaseTest {
    @Test
    fun locksBeforeWritingTheAngle() = runTest {
        val repository = FakeDeviceRotationRepository()

        SetDeviceRotationAngleUseCase(repository)(RotationAngle.Degrees90)

        assertEquals(listOf("locked=true", "angle=90"), repository.calls)
    }

    @Test
    fun opensThePermissionScreenWithoutPermission() = runTest {
        val repository = FakeDeviceRotationRepository(
            DeviceRotationStatus(supported = true, permitted = false),
        )

        SetDeviceRotationAngleUseCase(repository)(RotationAngle.Degrees90)

        assertEquals(1, repository.permissionRequests)
    }

    @Test
    fun writesNothingWithoutPermission() = runTest {
        val repository = FakeDeviceRotationRepository(
            DeviceRotationStatus(supported = true, permitted = false),
        )

        SetDeviceRotationAngleUseCase(repository)(RotationAngle.Degrees90)

        assertEquals(emptyList<String>(), repository.calls)
    }
}
