package io.github.taetae98coding.jarvis.domain.screen

import kotlinx.coroutines.flow.StateFlow

/**
 * 화면 꺼짐 방지 설정. 앱을 껐다 켜도 남고, 앱 밖에서 값이 바뀌어도 따라간다.
 *
 * Flow 가 아니라 StateFlow 인 이유는 화면의 첫 프레임부터 저장된 값이 보여야 해서다. Flow 의 첫
 * 방출은 컴포지션이 한 번 끝난 뒤에야 도착한다.
 */
interface ScreenAwakeSettingsRepository {
    val keepScreenAwake: StateFlow<Boolean>

    val keepSystemScreenAwake: StateFlow<Boolean>

    fun setKeepScreenAwake(value: Boolean)

    fun setKeepSystemScreenAwake(value: Boolean)
}
