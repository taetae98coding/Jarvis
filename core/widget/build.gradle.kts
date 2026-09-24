plugins {
    id("jarvis.android.library")
}

dependencies {
    // NotificationCompat 와 DecoratedCustomViewStyle. 알림 위젯(docs/common/notification-widget.html).
    // api 다. 기능 위젯 모듈의 알림 레이아웃 XML 이 TextAppearance.Compat.Notification.* 스타일을 참조한다.
    api(libs.androidx.core)
    // ProcessLifecycleOwner. 앱이 포그라운드인 동안만 알림을 동기화한다.
    implementation(libs.androidx.lifecycle.process)
    // 알림 리시버와 타일이 suspend 유스케이스를 부른다.
    implementation(libs.kotlinx.coroutines.core)
}
