package io.github.taetae98coding.jarvis.data.rotation

import io.github.taetae98coding.jarvis.data.PlatformContext
import io.github.taetae98coding.jarvis.domain.rotation.DeviceRotationStatus
import io.github.taetae98coding.jarvis.domain.rotation.RotationAngle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

/**
 * 화면 회전 각도와 잠금을 읽고 바꾼다.
 *
 * 읽기와 쓰기를 한 타입에 둔 이유는 시스템 전역 화면 유지와 같다. 둘 다 같은 플랫폼 자원을
 * 다루고, 나누면 관찰이 두 벌 돌게 된다.
 */
internal interface DeviceRotationDataSource {
    /** 구독 전에 쓸 첫 값. Flow 의 첫 방출은 컴포지션이 한 번 끝난 뒤에야 도착한다. */
    fun readStatus(): DeviceRotationStatus

    fun observeStatus(): Flow<DeviceRotationStatus>

    fun setAngle(angle: RotationAngle)

    fun setLocked(locked: Boolean)

    fun requestPermission()
}

internal object UnsupportedDeviceRotationDataSource : DeviceRotationDataSource {
    override fun readStatus(): DeviceRotationStatus = DeviceRotationStatus()

    // 지원하지 않는 플랫폼에서는 상태가 바뀔 일이 없어서 첫 값 뒤로 아무것도 흘리지 않는다.
    override fun observeStatus(): Flow<DeviceRotationStatus> = emptyFlow()

    override fun setAngle(angle: RotationAngle) = Unit

    override fun setLocked(locked: Boolean) = Unit

    override fun requestPermission() = Unit
}

internal expect fun createDeviceRotationDataSource(context: PlatformContext): DeviceRotationDataSource
