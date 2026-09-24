package io.github.taetae98coding.jarvis.domain.screen

import kotlinx.coroutines.flow.Flow

/**
 * 화면 꺼짐 방지 설정. 앱을 껐다 켜도 남고, 앱 밖에서 값이 바뀌어도 따라간다.
 *
 * `observe*` 는 cold 라서 수집하는 동안에만 저장소 리스너가 붙는다. `read*` 는 첫 프레임에 쓸 초기값이다.
 * Flow 의 첫 방출은 컴포지션이 한 번 끝난 뒤에야 도착한다. 이 값으로 상태를 따라가지 않는다.
 */
interface ScreenAwakeSettingsRepository {
    fun observeKeepScreenAwake(): Flow<Boolean>

    fun readKeepScreenAwake(): Boolean

    fun observeKeepSystemScreenAwake(): Flow<Boolean>

    fun readKeepSystemScreenAwake(): Boolean

    fun setKeepScreenAwake(value: Boolean)

    fun setKeepSystemScreenAwake(value: Boolean)
}
