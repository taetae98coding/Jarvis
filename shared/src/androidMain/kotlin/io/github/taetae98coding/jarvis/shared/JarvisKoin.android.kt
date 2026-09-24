package io.github.taetae98coding.jarvis.shared

import android.content.Context
import io.github.taetae98coding.jarvis.data.PlatformContext
import io.github.taetae98coding.jarvis.widget.NotificationWidgets
import io.github.taetae98coding.jarvis.widget.rotation.DeviceRotationNotification
import io.github.taetae98coding.jarvis.widget.screen.SystemScreenAwakeNotification

/**
 * Android 만 인자가 있다. `SharedPreferences` 와 `Settings.System` 이 `Context` 를 요구한다.
 *
 * 리포지토리는 앱 수명 동안 사는 single 이라 Activity 컨텍스트를 넘기면 그만큼 붙들려 있는다.
 */
fun startJarvisKoin(context: Context) {
    startJarvisKoinWith(PlatformContext(context.applicationContext))
}

/**
 * 앱이 포그라운드인 동안 "알림에 고정" 설정을 따라 두 알림 위젯을 올리고 내린다(docs/common/notification-widget.html).
 * [startJarvisKoin] 뒤에 부른다. 알림 위젯이 Koin 에서 리포지토리를 꺼낸다.
 */
fun startJarvisNotificationWidgets(context: Context) {
    NotificationWidgets.syncWhileStarted(
        context.applicationContext,
        listOf(DeviceRotationNotification, SystemScreenAwakeNotification),
    )
}
