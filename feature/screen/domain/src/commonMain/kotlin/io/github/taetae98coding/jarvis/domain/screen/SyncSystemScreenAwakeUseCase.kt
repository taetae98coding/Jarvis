package io.github.taetae98coding.jarvis.domain.screen

import kotlinx.coroutines.flow.first

/**
 * 저장된 설정값을 시스템 전역 화면 꺼짐 시간에 한 번 반영한다.
 *
 * [ApplySystemScreenAwakeUseCase] 는 설정 Flow 를 따라 계속 반영하지만 앱 루트 ViewModel 의 스코프가
 * 있어야 돈다. 화면 없이 설정을 바꾸는 쪽(홈 화면 위젯)은 이것을 바꾼 직후 부른다.
 */
class SyncSystemScreenAwakeUseCase(
    private val settings: ScreenAwakeSettingsRepository,
    private val systemScreenAwake: SystemScreenAwakeRepository,
) {
    suspend operator fun invoke() {
        // 권한이 없으면 적용도 복원도 할 수 없다. ApplySystemScreenAwakeUseCase 와 같은 판단이다.
        if (!systemScreenAwake.observeStatus().first().permitted) return

        systemScreenAwake.setEnabled(settings.readKeepSystemScreenAwake())
    }
}
