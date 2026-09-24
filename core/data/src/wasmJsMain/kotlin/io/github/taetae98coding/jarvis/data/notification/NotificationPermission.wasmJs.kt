package io.github.taetae98coding.jarvis.data.notification

import io.github.taetae98coding.jarvis.data.PlatformContext

// 브라우저 알림은 OS 설정에 닿지 못한다.
actual fun createNotificationPermission(context: PlatformContext): NotificationPermission =
    UnsupportedNotificationPermission
