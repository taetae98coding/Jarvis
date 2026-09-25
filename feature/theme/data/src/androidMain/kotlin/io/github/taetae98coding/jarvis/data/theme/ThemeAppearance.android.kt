package io.github.taetae98coding.jarvis.data.theme

import android.app.UiModeManager
import io.github.taetae98coding.jarvis.data.PlatformContext
import io.github.taetae98coding.jarvis.domain.theme.ThemeMode

internal actual fun createThemeAppearance(context: PlatformContext): ThemeAppearance =
    UiModeThemeAppearance(context.context.getSystemService(UiModeManager::class.java))

/**
 * 앱 하나에만 걸리는 야간 모드 오버라이드. 앱의 uiMode 가 바뀌어 isSystemInDarkTheme() 과 시스템 막대
 * 아이콘 색이 함께 따라가고, OS 가 값을 보관해 다음 실행의 첫 프레임 이전부터 맞는다.
 */
private class UiModeThemeAppearance(
    private val uiModeManager: UiModeManager?,
) : ThemeAppearance {
    override fun apply(mode: ThemeMode) {
        uiModeManager?.setApplicationNightMode(
            when (mode) {
                // Javadoc 은 AUTO 를 "위치·센서로 자동 전환" 이라 적었지만, UiModeManagerService 는 YES·NO 외의
                // 값을 UI_MODE_NIGHT_UNDEFINED(오버라이드 없음 = 시스템 설정)로 커밋한다.
                ThemeMode.SYSTEM -> UiModeManager.MODE_NIGHT_AUTO
                ThemeMode.LIGHT -> UiModeManager.MODE_NIGHT_NO
                ThemeMode.DARK -> UiModeManager.MODE_NIGHT_YES
            },
        )
    }
}
