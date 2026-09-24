package io.github.taetae98coding.jarvis.data.notification

import io.github.taetae98coding.jarvis.data.PlatformContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * 앱이 알림을 게시할 수 있는지. 화면 회전과 화면 꺼짐 방지의 "알림에 고정" 리포지토리가 함께 본다.
 *
 * 저장은 [io.github.taetae98coding.jarvis.data.settings.SettingsStore] 가 이미 플랫폼을 숨기므로,
 * 알림 고정 리포지토리에서 플랫폼에 기대는 것은 이 권한뿐이다.
 */
interface NotificationPermission {
    /** 이 플랫폼이 앱 밖에 알림 컨트롤을 놓을 수 있는가. false 면 카드에 "알림에 고정" 행이 없다. */
    val supported: Boolean

    /** 첫 프레임에 쓸 초기값. */
    fun readPermitted(): Boolean

    /** cold 다. 수집하는 동안에만 권한 변화를 지켜본다. */
    fun observePermitted(): Flow<Boolean>

    fun requestPermission()
}

object UnsupportedNotificationPermission : NotificationPermission {
    override val supported: Boolean = false

    override fun readPermitted(): Boolean = false

    // 상태가 바뀔 일이 없어도 첫 값은 흘린다. combine 이 첫 값을 기다린다.
    override fun observePermitted(): Flow<Boolean> = flowOf(false)

    override fun requestPermission() = Unit
}

expect fun createNotificationPermission(context: PlatformContext): NotificationPermission
