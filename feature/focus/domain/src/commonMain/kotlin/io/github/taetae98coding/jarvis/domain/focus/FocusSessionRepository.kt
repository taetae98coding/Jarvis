package io.github.taetae98coding.jarvis.domain.focus

import kotlinx.coroutines.flow.Flow

/**
 * 타이머 상태. 앱을 껐다 켜도 남는다.
 *
 * `observe*` 는 cold 라서 수집하는 동안에만 저장소 리스너가 붙는다. `read*` 는 첫 프레임에 쓸 초기값이다.
 */
interface FocusSessionRepository {
    fun observeFocusSession(): Flow<FocusSession>

    fun readFocusSession(): FocusSession

    fun saveFocusSession(session: FocusSession)
}
