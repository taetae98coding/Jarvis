package io.github.taetae98coding.jarvis.shared

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import io.github.taetae98coding.jarvis.ui.app.JarvisApp

/**
 * 화면은 Koin 에서 필요한 것을 꺼내 쓴다. 이 함수를 부르기 전에 진입점이 `startJarvisKoin()` 을
 * 불러야 한다.
 */
@Composable
@Preview
fun App() {
    JarvisApp()
}
