package io.github.taetae98coding.jarvis.domain.screen

class SetKeepSystemScreenAwakeUseCase(
    private val settings: ScreenAwakeSettingsRepository,
    private val systemScreenAwake: SystemScreenAwakeRepository,
) {
    operator fun invoke(value: Boolean) {
        // 권한이 없어도 설정값은 켜 둔다. 사용자가 설정 화면에서 허용하고 돌아오면 그때 효과가
        // 걸리고, 여기서 값을 되돌리면 왜 꺼졌는지 알 수 없다.
        settings.setKeepSystemScreenAwake(value)

        if (value && !systemScreenAwake.status.value.permitted) {
            systemScreenAwake.requestPermission()
        }
    }
}
