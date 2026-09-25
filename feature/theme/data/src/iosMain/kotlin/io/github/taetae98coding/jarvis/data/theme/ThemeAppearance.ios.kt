package io.github.taetae98coding.jarvis.data.theme

import io.github.taetae98coding.jarvis.data.PlatformContext
import io.github.taetae98coding.jarvis.domain.theme.ThemeMode
import platform.UIKit.UIApplication
import platform.UIKit.UIUserInterfaceStyle
import platform.UIKit.UIWindow
import platform.UIKit.UIWindowScene

internal actual fun createThemeAppearance(context: PlatformContext): ThemeAppearance = WindowThemeAppearance

/**
 * 창에 걸어야 상태 막대까지 따라간다. Compose 의 뷰 컨트롤러는 SwiftUI 호스팅 컨트롤러의 자식이라 거기에
 * 걸면 상태 막대 스타일은 그대로다.
 */
private object WindowThemeAppearance : ThemeAppearance {
    override fun apply(mode: ThemeMode) {
        val style = when (mode) {
            ThemeMode.SYSTEM -> UIUserInterfaceStyle.UIUserInterfaceStyleUnspecified
            ThemeMode.LIGHT -> UIUserInterfaceStyle.UIUserInterfaceStyleLight
            ThemeMode.DARK -> UIUserInterfaceStyle.UIUserInterfaceStyleDark
        }

        UIApplication.sharedApplication.connectedScenes
            .filterIsInstance<UIWindowScene>()
            .flatMap { it.windows }
            .filterIsInstance<UIWindow>()
            .forEach { it.overrideUserInterfaceStyle = style }
    }
}
