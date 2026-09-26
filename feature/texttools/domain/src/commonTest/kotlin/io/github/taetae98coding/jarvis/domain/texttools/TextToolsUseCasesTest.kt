package io.github.taetae98coding.jarvis.domain.texttools

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** docs/common/text-tools.html R6·R12·R13 */
class TextToolsUseCasesTest {
    private class FakeRepository : TextToolsSettingsRepository {
        val tool = MutableStateFlow(TextTool.COUNT)
        val input = MutableStateFlow("")
        val limit = MutableStateFlow(TextLimit.None)
        val options = MutableStateFlow(PasswordOptions())

        override fun observeSelectedTool() = tool
        override fun readSelectedTool() = tool.value
        override fun setSelectedTool(tool: TextTool) { this.tool.value = tool }
        override fun observeInput() = input
        override fun readInput() = input.value
        override fun setInput(input: String) { this.input.value = input }
        override fun observeLimit() = limit
        override fun readLimit() = limit.value
        override fun setLimit(limit: TextLimit) { this.limit.value = limit }
        override fun observePasswordOptions() = options
        override fun readPasswordOptions() = options.value
        override fun setPasswordOptions(options: PasswordOptions) { this.options.value = options }
    }

    @Test
    fun shortInputIsStoredAsIs() {
        val repository = FakeRepository()

        SetTextInputUseCase(repository)("자기소개서")

        assertEquals("자기소개서", repository.input.value)
    }

    @Test
    fun longInputIsTruncatedForStorage() {
        val repository = FakeRepository()

        SetTextInputUseCase(repository)("a".repeat(SetTextInputUseCase.MaxStoredLength + 10))

        assertEquals(SetTextInputUseCase.MaxStoredLength, repository.input.value.length)
    }

    @Test
    fun truncationDoesNotSplitSurrogatePair() {
        // 마지막으로 남는 글자가 😀 의 high surrogate 가 되게 맞춘다.
        val input = "a".repeat(SetTextInputUseCase.MaxStoredLength - 1) + "😀" + "b"

        val stored = SetTextInputUseCase.truncateForStorage(input)

        assertEquals(SetTextInputUseCase.MaxStoredLength - 1, stored.length)
        assertTrue(!stored.last().isHighSurrogate())
    }

    @Test
    fun lastCharacterClassCannotBeTurnedOff() {
        val repository = FakeRepository()
        val current = PasswordOptions(uppercase = false, lowercase = false, digits = true, symbols = false)

        val next = SetPasswordOptionsUseCase(repository)(current, current.copy(digits = false))

        assertEquals(current, next)
        assertEquals(current, repository.options.value)
    }

    @Test
    fun passwordLengthIsClampedWhenSaved() {
        val repository = FakeRepository()

        val next = SetPasswordOptionsUseCase(repository)(PasswordOptions(), PasswordOptions(length = 100))

        assertEquals(PasswordOptions.MaxLength, next.length)
        assertEquals(PasswordOptions.MaxLength, repository.options.value.length)
    }

    @Test
    fun limitIsNormalized() {
        val repository = FakeRepository()

        SetTextLimitUseCase(repository)(0, LimitBasis.KOREAN_BYTES)

        assertEquals(TextLimit(null, LimitBasis.KOREAN_BYTES), repository.limit.value)
    }

    @Test
    fun observeCombinesAllSettings() = runTest {
        val repository = FakeRepository()
        val state = ObserveTextToolsSettingsUseCase(repository)(backgroundScope)

        assertEquals(TextTool.COUNT, state.value.tool)

        SelectTextToolUseCase(repository)(TextTool.PASSWORD)
        SetTextInputUseCase(repository)("hi")
        SetTextLimitUseCase(repository)(500, LimitBasis.WITHOUT_SPACES)

        val settings = state.first { it.tool == TextTool.PASSWORD && it.input == "hi" && it.limit.target == 500 }
        assertEquals(LimitBasis.WITHOUT_SPACES, settings.limit.basis)
    }

    @Test
    fun generatedPasswordIsNotWrittenToSettings() {
        val repository = FakeRepository()
        var calls = 0
        val generate = GeneratePasswordUseCase(PasswordGenerator { calls++ })

        val password = generate(repository.readPasswordOptions())

        assertEquals(PasswordOptions.DefaultLength, password.length)
        assertEquals(PasswordOptions(), repository.options.value)
        assertEquals("", repository.input.value)
    }

    @Test
    fun countAndTransformUseCases() {
        assertEquals(2, CountTextUseCase()("ab").charactersWithSpaces)
        assertEquals("AB", TransformTextUseCase()("ab").first { it.transform == TextTransform.UPPERCASE }.text)
    }
}
