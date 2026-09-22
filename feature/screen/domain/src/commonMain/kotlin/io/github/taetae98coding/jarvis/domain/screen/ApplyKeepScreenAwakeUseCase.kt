package io.github.taetae98coding.jarvis.domain.screen

/**
 * 설정이 켜져 있는 동안 화면 꺼짐 방지 효과를 걸어 둔다. 호출한 쪽이 취소할 때까지 돌아간다.
 */
class ApplyKeepScreenAwakeUseCase(
    private val settings: ScreenAwakeSettingsRepository,
    private val screenAwake: ScreenAwakeRepository,
) {
    suspend operator fun invoke() {
        try {
            settings.keepScreenAwake.collect(screenAwake::setKeepScreenAwake)
        } finally {
            // 앱 수명 내에서만 유효한 기능이라 화면이 사라지면 푼다. JVM 에서는 풀지 않으면
            // caffeinate 자식 프로세스가 남는다.
            screenAwake.setKeepScreenAwake(false)
        }
    }
}
