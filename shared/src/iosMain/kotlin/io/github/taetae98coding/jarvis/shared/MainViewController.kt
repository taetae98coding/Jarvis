package io.github.taetae98coding.jarvis.shared

import androidx.compose.ui.window.ComposeUIViewController
import platform.UIKit.UIViewController

/**
 * Swift 가 부르는 유일한 화면 진입점이다. Koin 시작도 여기서 한다 — Swift 쪽에 시작 코드를 두면
 * 시작 시점이 Swift 와 Kotlin 두 곳으로 흩어진다.
 *
 * SwiftUI 가 `ComposeView` 를 다시 만들면 이 함수도 다시 불린다. 두 번째 호출은 아무 일도 하지 않는다.
 */
fun MainViewController(): UIViewController {
    startJarvisKoin()

    return ComposeUIViewController { App() }
}
