package io.github.taetae98coding.jarvis.domain.rotation

import kotlinx.coroutines.flow.Flow

interface DeviceRotationRepository {
    /** cold 다. 수집하는 동안에만 회전과 권한을 지켜본다. */
    fun observeStatus(): Flow<DeviceRotationStatus>

    /** 첫 프레임에 쓸 초기값. 이 값으로 상태를 따라가지 않는다. */
    fun readStatus(): DeviceRotationStatus

    fun setAngle(angle: RotationAngle)

    fun setLocked(locked: Boolean)

    fun requestPermission()
}
