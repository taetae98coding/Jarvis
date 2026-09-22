package io.github.taetae98coding.jarvis.domain.screen

import kotlinx.coroutines.flow.StateFlow

interface SystemScreenAwakeRepository {
    val status: StateFlow<SystemScreenAwakeStatus>

    /** 시스템 전역 화면 꺼짐 시간을 늘리거나 원래대로 되돌린다. */
    fun setEnabled(enabled: Boolean)

    /** 권한 설정 화면을 연다. 이미 허용됐거나 플랫폼이 지원하지 않으면 아무 일도 하지 않는다. */
    fun requestPermission()
}
