package io.github.taetae98coding.jarvis.domain.screen

/**
 * 앱이 떠 있는 동안 화면을 켜 두는 장치.
 *
 * 네 타깃 중 셋은 Compose 의 `Modifier.keepScreenOn()` 이 대신 처리해서 구현이 비어 있고, 그것이
 * 동작하지 않는 JVM 만 실제 동작을 갖는다.
 */
fun interface ScreenAwakeRepository {
    fun setKeepScreenAwake(enabled: Boolean)
}
