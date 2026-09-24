package io.github.taetae98coding.jarvis.domain.rotation

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class RotateDeviceUseCaseTest {
    @Test
    fun rotatesForwardFromTheCurrentAngle() = runTest {
        val repository = FakeDeviceRotationRepository(
            DeviceRotationStatus(supported = true, angle = RotationAngle.Degrees90),
        )

        rotateDevice(repository)(1)

        assertEquals(RotationAngle.Degrees180, repository.status.value.angle)
    }

    @Test
    fun rotatesBackwardFromTheCurrentAngle() = runTest {
        val repository = FakeDeviceRotationRepository(
            DeviceRotationStatus(supported = true, angle = RotationAngle.Degrees90),
        )

        rotateDevice(repository)(-1)

        assertEquals(RotationAngle.Degrees0, repository.status.value.angle)
    }

    @Test
    fun treatsAnUnknownAngleAsZero() = runTest {
        val repository = FakeDeviceRotationRepository(
            DeviceRotationStatus(supported = true, angle = null),
        )

        rotateDevice(repository)(-1)

        assertEquals(RotationAngle.Degrees270, repository.status.value.angle)
    }

    @Test
    fun followsAnAngleThatChangedWhileTheScreenWasShowingTheOldOne() = runTest {
        val repository = FakeDeviceRotationRepository(
            DeviceRotationStatus(supported = true, angle = RotationAngle.Degrees0),
        )
        val rotate = rotateDevice(repository)

        // 화면이 0도를 그리고 있는 사이 기기가 돌아간 상황. 한 칸만 움직여야 한다.
        repository.status.value = repository.status.value.copy(angle = RotationAngle.Degrees180)
        rotate(1)

        assertEquals(RotationAngle.Degrees270, repository.status.value.angle)
    }
}

private fun rotateDevice(repository: DeviceRotationRepository) =
    RotateDeviceUseCase(repository, SetDeviceRotationAngleUseCase(repository))
