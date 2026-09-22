package io.github.taetae98coding.jarvis.domain.screen

import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * 권한이 허용된 동안 설정값을 시스템 전역 화면 꺼짐 시간에 반영한다. 호출한 쪽이 취소할 때까지
 * 돌아간다.
 *
 * [ApplyKeepScreenAwakeUseCase] 와 달리 취소될 때 효과를 풀지 않는다. 앱이 없는 동안 화면을 켜
 * 두는 것이 이 기능의 목적이라, 되돌리는 시점은 사용자가 토글을 끌 때뿐이다.
 */
class ApplySystemScreenAwakeUseCase(
    private val settings: ScreenAwakeSettingsRepository,
    private val systemScreenAwake: SystemScreenAwakeRepository,
) {
    suspend operator fun invoke() {
        combine(settings.keepSystemScreenAwake, systemScreenAwake.status) { enabled, status ->
            // 권한이 없으면 적용도 복원도 할 수 없다. 설정값은 그대로 두고 권한이 들어오기를 기다린다.
            enabled.takeIf { status.permitted }
        }
            .distinctUntilChanged()
            .collect { enabled -> enabled?.let(systemScreenAwake::setEnabled) }
    }
}
