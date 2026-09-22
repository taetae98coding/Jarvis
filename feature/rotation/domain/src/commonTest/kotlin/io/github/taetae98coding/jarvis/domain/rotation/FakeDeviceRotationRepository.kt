package io.github.taetae98coding.jarvis.domain.rotation

import kotlinx.coroutines.flow.MutableStateFlow

internal class FakeDeviceRotationRepository(
    initial: DeviceRotationStatus = DeviceRotationStatus(supported = true, angle = RotationAngle.Degrees0),
) : DeviceRotationRepository {
    override val status = MutableStateFlow(initial)

    // 각도와 잠금을 어떤 순서로 썼는지까지 봐야 한다. 잠금이 나중에 걸리면 Android 에서 화면이
    // 돌지 않는다.
    val calls = mutableListOf<String>()
    var permissionRequests = 0
        private set

    override fun setAngle(angle: RotationAngle) {
        calls += "angle=${angle.degrees}"
        status.value = status.value.copy(angle = angle)
    }

    override fun setLocked(locked: Boolean) {
        calls += "locked=$locked"
        status.value = status.value.copy(locked = locked)
    }

    override fun requestPermission() {
        permissionRequests++
    }
}
