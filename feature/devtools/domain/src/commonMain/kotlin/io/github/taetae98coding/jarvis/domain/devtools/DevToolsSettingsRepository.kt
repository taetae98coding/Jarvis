package io.github.taetae98coding.jarvis.domain.devtools

import kotlinx.coroutines.flow.Flow

/**
 * 마지막에 고른 도구와 도구마다의 입력. 앱을 껐다 켜도 남는다.
 *
 * `observe*` 는 cold 라서 수집하는 동안에만 저장소 리스너가 붙는다. `read*` 는 첫 프레임에 쓸 초기값이다.
 */
interface DevToolsSettingsRepository {
    fun observeSelectedTool(): Flow<DevTool>

    fun readSelectedTool(): DevTool

    fun setSelectedTool(tool: DevTool)

    fun observeInput(tool: DevTool): Flow<String>

    fun readInput(tool: DevTool): String

    fun setInput(tool: DevTool, input: String)
}
