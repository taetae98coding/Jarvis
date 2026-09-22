package io.github.taetae98coding.jarvis.domain.rotation

import kotlinx.coroutines.flow.StateFlow

interface DeviceRotationRepository {
    val status: StateFlow<DeviceRotationStatus>

    fun setAngle(angle: RotationAngle)

    fun setLocked(locked: Boolean)

    fun requestPermission()
}
