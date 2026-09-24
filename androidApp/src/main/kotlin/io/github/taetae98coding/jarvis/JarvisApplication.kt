package io.github.taetae98coding.jarvis

import android.app.Application
import io.github.taetae98coding.jarvis.shared.startJarvisKoin
import io.github.taetae98coding.jarvis.shared.startJarvisNotificationWidgets

class JarvisApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // 홈 화면 위젯 탭은 Activity 없이 프로세스를 띄우고 ActionCallback 이 곧바로 Koin 을 요구한다.
        // 그래서 MainActivity 가 아니라 프로세스 시작 시점에 세운다.
        startJarvisKoin(this)

        // 앱이 포그라운드인 동안 "알림에 고정" 설정을 따라 알림을 올리고 내린다. onStart 마다 첫 방출이
        // 다시 게시하므로, Android 14+ 에서 사용자가 지운 알림도 여기서 돌아온다.
        startJarvisNotificationWidgets(this)
    }
}
