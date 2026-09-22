package io.github.taetae98coding.jarvis

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import io.github.taetae98coding.jarvis.shared.App
import io.github.taetae98coding.jarvis.shared.startJarvisKoin

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // 화면이 Koin 에서 ViewModel 을 꺼내므로 setContent 보다 먼저 서 있어야 한다. Activity 가
        // 다시 만들어지면 여기도 다시 도는데, 두 번째 호출은 아무 일도 하지 않는다.
        startJarvisKoin(applicationContext)

        setContent {
            App()
        }
    }
}
