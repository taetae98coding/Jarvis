package io.github.taetae98coding.jarvis.domain.rotation

import kotlinx.coroutines.flow.first

class SetDeviceRotationAngleUseCase(
    private val deviceRotation: DeviceRotationRepository,
) {
    suspend operator fun invoke(angle: RotationAngle) {
        // 화면 꺼짐 방지와 달리 권한이 없을 때 저장해 둘 값이 없다. 시스템이 각도를 들고 있으므로
        // 허용하고 돌아온 뒤 다시 누르는 것이 유일한 경로다.
        if (!deviceRotation.observeStatus().first().permitted) {
            deviceRotation.requestPermission()

            return
        }

        // 자동 회전이 켜진 채로 각도를 쓰면 값만 남고 화면은 돌지 않는다. 그래서 잠금이 먼저다.
        deviceRotation.setLocked(true)
        deviceRotation.setAngle(angle)
    }
}
