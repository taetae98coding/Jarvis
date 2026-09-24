package io.github.taetae98coding.jarvis.ui.app

// 화면 테스트가 메인 스레드에서 돈다. Main 이 곧 테스트가 도는 곳이다.
internal actual fun installTestMainDispatcher() = Unit
