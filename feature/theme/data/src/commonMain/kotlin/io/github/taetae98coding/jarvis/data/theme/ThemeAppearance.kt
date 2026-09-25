package io.github.taetae98coding.jarvis.data.theme

import io.github.taetae98coding.jarvis.data.PlatformContext
import io.github.taetae98coding.jarvis.domain.theme.ThemeAppearanceRepository
import io.github.taetae98coding.jarvis.domain.theme.ThemeMode

/** 플랫폼이 앱 밖 표면에 모드를 적용하는 장치. 무엇을 하는지는 각 플랫폼 스펙의 theme-mode 절에 있다. */
internal fun interface ThemeAppearance {
    fun apply(mode: ThemeMode)
}

internal expect fun createThemeAppearance(context: PlatformContext): ThemeAppearance

internal class DefaultThemeAppearanceRepository(
    private val appearance: ThemeAppearance,
) : ThemeAppearanceRepository {
    override fun applyThemeMode(mode: ThemeMode) {
        appearance.apply(mode)
    }
}
