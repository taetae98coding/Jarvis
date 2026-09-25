package io.github.taetae98coding.jarvis.ui.app

/**
 * Wasm 에서만 건너뛴다. Wasm 의 `waitUntil` 은 하나뿐인 이벤트 루프를 막은 채 돌아서, 기다리는 동안 실제 시간 타이머
 * (`delay`, `withTimeout`)가 끝나지 못한다. 앱 코드가 실제 시간을 기다려야 하는 테스트는 JVM·iOS 에서만 돈다.
 */
@Target(AnnotationTarget.FUNCTION)
internal expect annotation class IgnoreOnWasm()
