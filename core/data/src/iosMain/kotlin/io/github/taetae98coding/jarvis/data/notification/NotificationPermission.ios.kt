package io.github.taetae98coding.jarvis.data.notification

import io.github.taetae98coding.jarvis.data.PlatformContext

// iOS 알림은 버튼을 놓을 수 있지만 부를 시스템 동작이 없다(docs/platform/ios.html#notification-widget).
actual fun createNotificationPermission(context: PlatformContext): NotificationPermission =
    UnsupportedNotificationPermission
