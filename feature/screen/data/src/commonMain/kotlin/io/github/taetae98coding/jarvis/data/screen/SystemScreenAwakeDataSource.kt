package io.github.taetae98coding.jarvis.data.screen

import io.github.taetae98coding.jarvis.data.PlatformContext
import io.github.taetae98coding.jarvis.domain.screen.SystemScreenAwakeStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

/**
 * 시스템 전역 화면 꺼짐 설정을 읽고 쓴다.
 *
 * 읽기와 쓰기를 한 타입에 둔 이유는 둘 다 같은 플랫폼 자원(권한과 설정값)을 다뤄서다. 나누면
 * 권한 폴링이 두 벌 돌게 된다.
 */
internal interface SystemScreenAwakeDataSource {
    /** 구독 전에 쓸 첫 값. Flow 의 첫 방출은 컴포지션이 한 번 끝난 뒤에야 도착한다. */
    fun readStatus(): SystemScreenAwakeStatus

    fun observeStatus(): Flow<SystemScreenAwakeStatus>

    fun setEnabled(enabled: Boolean)

    fun requestPermission()
}

internal object UnsupportedSystemScreenAwakeDataSource : SystemScreenAwakeDataSource {
    override fun readStatus(): SystemScreenAwakeStatus = SystemScreenAwakeStatus()

    // 지원하지 않는 플랫폼에서는 상태가 바뀔 일이 없어서 첫 값 뒤로 아무것도 흘리지 않는다.
    override fun observeStatus(): Flow<SystemScreenAwakeStatus> = emptyFlow()

    override fun setEnabled(enabled: Boolean) = Unit

    override fun requestPermission() = Unit
}

internal expect fun createSystemScreenAwakeDataSource(context: PlatformContext): SystemScreenAwakeDataSource
