package io.github.taetae98coding.jarvis.domain.theme

/**
 * 앱이 직접 그리지 않는 표면(시스템 막대, 창 틀, 페이지 배경)에 고른 모드를 알린다.
 *
 * 앱 안의 색은 이것과 무관하게 `JarvisTheme(darkTheme = …)` 이 맞춘다. 할 수 있는 것이 없는
 * 플랫폼은 조용히 no-op 이다.
 */
fun interface ThemeAppearanceRepository {
    fun applyThemeMode(mode: ThemeMode)
}
