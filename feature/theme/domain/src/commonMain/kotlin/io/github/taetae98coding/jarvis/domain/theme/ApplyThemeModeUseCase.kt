package io.github.taetae98coding.jarvis.domain.theme

/**
 * 설정이 바뀔 때마다 플랫폼 표면에 넘긴다. 호출한 쪽이 취소할 때까지 돌아간다.
 *
 * 취소돼도 되돌리지 않는다. 모드는 사용자가 고른 영구 상태라 앱이 사라진 뒤에도 그대로여야 하고,
 * OS 가 값을 보관하는 Android 에서는 되돌리면 다음 실행의 첫 프레임이 틀어진다.
 */
class ApplyThemeModeUseCase(
    private val settings: ThemeSettingsRepository,
    private val appearance: ThemeAppearanceRepository,
) {
    suspend operator fun invoke() {
        settings.observeThemeMode().collect(appearance::applyThemeMode)
    }
}
