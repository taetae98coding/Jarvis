package io.github.taetae98coding.jarvis.data.theme

import io.github.taetae98coding.jarvis.data.PlatformContext
import io.github.taetae98coding.jarvis.domain.theme.ThemeMode
import kotlinx.browser.document
import org.w3c.dom.HTMLElement

internal actual fun createThemeAppearance(context: PlatformContext): ThemeAppearance = DocumentThemeAppearance

// 캔버스 밖(페이지 배경·스크롤바·폼 컨트롤)만 해당한다. 캔버스 안은 Compose 가 그린다.
private object DocumentThemeAppearance : ThemeAppearance {
    override fun apply(mode: ThemeMode) {
        val root = document.documentElement as? HTMLElement ?: return
        root.style.setProperty(
            "color-scheme",
            when (mode) {
                ThemeMode.SYSTEM -> "light dark"
                ThemeMode.LIGHT -> "light"
                ThemeMode.DARK -> "dark"
            },
        )
    }
}
