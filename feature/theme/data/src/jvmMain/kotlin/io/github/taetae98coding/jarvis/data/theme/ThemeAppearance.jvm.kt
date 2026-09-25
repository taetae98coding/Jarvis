package io.github.taetae98coding.jarvis.data.theme

import io.github.taetae98coding.jarvis.data.PlatformContext
import io.github.taetae98coding.jarvis.domain.theme.ThemeMode

internal actual fun createThemeAppearance(context: PlatformContext): ThemeAppearance = AwtThemeAppearance

/**
 * macOS 창 제목 막대의 외관. AWT 툴킷이 뜰 때 이 프로퍼티를 한 번만 읽으므로 런타임 변경은 다음 실행에서야
 * 보인다. 첫 창보다 먼저 저장된 모드로 부르는 것은 :shared 의 startJarvisKoin() 이다
 * (docs/platform/jvm.html#theme-mode). 다른 OS 는 프로퍼티를 무시한다.
 */
private object AwtThemeAppearance : ThemeAppearance {
    override fun apply(mode: ThemeMode) {
        System.setProperty(
            "apple.awt.application.appearance",
            when (mode) {
                ThemeMode.SYSTEM -> "system"
                ThemeMode.LIGHT -> "NSAppearanceNameAqua"
                ThemeMode.DARK -> "NSAppearanceNameDarkAqua"
            },
        )
    }
}
