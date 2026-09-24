package io.github.taetae98coding.jarvis.domain.rotation

import kotlinx.coroutines.flow.first

/**
 * 현재 각도에서 [steps] 칸(한 칸이 90도)만큼 돌린다.
 *
 * 화면이 들고 있는 각도가 아니라 리포지토리의 지금 값을 기준으로 삼는다. 화면의 값은 마지막
 * 방출 시점의 것이라, 그 사이 기기가 돌아가면 한 칸이 아니라 두 칸 돌게 된다.
 */
class RotateDeviceUseCase(
    private val deviceRotation: DeviceRotationRepository,
    private val setAngle: SetDeviceRotationAngleUseCase,
) {
    suspend operator fun invoke(steps: Int) {
        val current = deviceRotation.observeStatus().first().angle ?: RotationAngle.Degrees0

        setAngle(current.rotated(steps))
    }
}
