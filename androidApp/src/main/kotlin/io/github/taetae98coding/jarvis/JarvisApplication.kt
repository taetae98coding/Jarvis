package io.github.taetae98coding.jarvis

import android.app.Application
import io.github.taetae98coding.jarvis.shared.startJarvisKoin

class JarvisApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // 홈 화면 위젯 탭은 Activity 없이 프로세스를 띄우고 ActionCallback 이 곧바로 Koin 을 요구한다.
        // 그래서 MainActivity 가 아니라 프로세스 시작 시점에 세운다.
        startJarvisKoin(this)
    }
}
