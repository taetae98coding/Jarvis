package io.github.taetae98coding.jarvis.data.notification

import io.github.taetae98coding.jarvis.data.PlatformContext

// macOS 알림 센터에는 문구만 띄울 수 있고, 두 기능이 JVM 에서 불가라 보여줄 것도 없다.
actual fun createNotificationPermission(context: PlatformContext): NotificationPermission =
    UnsupportedNotificationPermission
