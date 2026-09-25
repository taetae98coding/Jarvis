package io.github.taetae98coding.jarvis.domain.theme

import kotlinx.coroutines.flow.Flow

/**
 * 고른 화면 테마. 앱을 껐다 켜도 남고, 앱 밖에서 값이 바뀌어도 따라간다.
 *
 * `observe*` 는 cold 라서 수집하는 동안에만 저장소 리스너가 붙는다. `read*` 는 첫 프레임에 쓸 초기값이다.
 */
interface ThemeSettingsRepository {
    fun observeThemeMode(): Flow<ThemeMode>

    fun readThemeMode(): ThemeMode

    fun setThemeMode(mode: ThemeMode)
}
