package io.github.taetae98coding.jarvis

import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import io.github.taetae98coding.jarvis.shared.App

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Koin 은 JarvisApplication 이 프로세스 시작 시점에 세운다.
        setContent {
            App()
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // 매니페스트가 uiMode 를 직접 받아 액티비티가 다시 만들어지지 않는다. 시스템 막대 아이콘 색은
        // enableEdgeToEdge 를 부른 시점의 uiMode 로 정해지므로, 다크 모드가 바뀌면 다시 부른다
        // (docs/platform/android.html#theme-mode).
        enableEdgeToEdge()
    }
}
