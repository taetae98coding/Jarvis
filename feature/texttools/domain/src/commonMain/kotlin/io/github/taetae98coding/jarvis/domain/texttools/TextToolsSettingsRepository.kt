package io.github.taetae98coding.jarvis.domain.texttools

import kotlinx.coroutines.flow.Flow

/**
 * 마지막에 고른 탭, 입력, 목표 글자 수, 비밀번호 옵션. 앱을 껐다 켜도 남는다. 만든 비밀번호는 여기 두지 않는다
 * (docs/common/text-tools.html#decision-no-password-persist).
 *
 * `observe*` 는 cold 라서 수집하는 동안에만 저장소 리스너가 붙는다. `read*` 는 첫 프레임에 쓸 초기값이다.
 */
interface TextToolsSettingsRepository {
    fun observeSelectedTool(): Flow<TextTool>

    fun readSelectedTool(): TextTool

    fun setSelectedTool(tool: TextTool)

    fun observeInput(): Flow<String>

    fun readInput(): String

    fun setInput(input: String)

    fun observeLimit(): Flow<TextLimit>

    fun readLimit(): TextLimit

    fun setLimit(limit: TextLimit)

    fun observePasswordOptions(): Flow<PasswordOptions>

    fun readPasswordOptions(): PasswordOptions

    fun setPasswordOptions(options: PasswordOptions)
}

data class TextToolsSettings(
    val tool: TextTool,
    val input: String,
    val limit: TextLimit,
    val passwordOptions: PasswordOptions,
)
