package io.github.taetae98coding.jarvis.domain.screen

import kotlinx.coroutines.flow.Flow

interface SystemScreenAwakeNotificationRepository {
    /** cold 다. 수집하는 동안에만 설정과 권한을 지켜본다. */
    fun observeStatus(): Flow<SystemScreenAwakeNotificationStatus>

    /** 첫 프레임에 쓸 초기값. */
    fun readStatus(): SystemScreenAwakeNotificationStatus

    fun setPinned(pinned: Boolean)

    fun requestPermission()
}
