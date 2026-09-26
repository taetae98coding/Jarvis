package io.github.taetae98coding.jarvis.data.texttools

import io.github.taetae98coding.jarvis.data.settings.InMemorySettingsStore
import io.github.taetae98coding.jarvis.domain.texttools.LimitBasis
import io.github.taetae98coding.jarvis.domain.texttools.PasswordOptions
import io.github.taetae98coding.jarvis.domain.texttools.TextLimit
import io.github.taetae98coding.jarvis.domain.texttools.TextTool
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

/** docs/common/text-tools.html R12 */
class TextToolsSettingsRepositoryTest {
    @Test
    fun defaults() {
        val repository = DefaultTextToolsSettingsRepository(InMemorySettingsStore())

        assertEquals(TextTool.COUNT, repository.readSelectedTool())
        assertEquals("", repository.readInput())
        assertEquals(TextLimit.None, repository.readLimit())
        assertEquals(PasswordOptions(), repository.readPasswordOptions())
    }

    @Test
    fun everythingSurvivesNewRepository() {
        val store = InMemorySettingsStore()
        val options = PasswordOptions(length = 32, uppercase = false, symbols = false, excludeAmbiguous = true)
        DefaultTextToolsSettingsRepository(store).apply {
            setSelectedTool(TextTool.CASE)
            setInput("첫 줄\n둘째 줄")
            setLimit(TextLimit(1000, LimitBasis.KOREAN_BYTES))
            setPasswordOptions(options)
        }

        val reopened = DefaultTextToolsSettingsRepository(store)
        assertEquals(TextTool.CASE, reopened.readSelectedTool())
        assertEquals("첫 줄\n둘째 줄", reopened.readInput())
        assertEquals(TextLimit(1000, LimitBasis.KOREAN_BYTES), reopened.readLimit())
        assertEquals(options, reopened.readPasswordOptions())
        assertEquals("case", store.getString(DefaultTextToolsSettingsRepository.SelectedToolKey, ""))
    }

    @Test
    fun clearedLimitIsStoredAsEmpty() {
        val store = InMemorySettingsStore()
        val repository = DefaultTextToolsSettingsRepository(store)
        repository.setLimit(TextLimit(500, LimitBasis.WITHOUT_SPACES))
        repository.setLimit(TextLimit(null, LimitBasis.WITHOUT_SPACES))

        assertEquals("", store.getString(DefaultTextToolsSettingsRepository.LimitKey, "x"))
        assertEquals(TextLimit(null, LimitBasis.WITHOUT_SPACES), repository.readLimit())
    }

    @Test
    fun invalidStoredValuesFallBack() {
        val store = InMemorySettingsStore(
            mutableMapOf<String, Any>(
                DefaultTextToolsSettingsRepository.SelectedToolKey to "removed-tool",
                DefaultTextToolsSettingsRepository.LimitKey to "abc",
                DefaultTextToolsSettingsRepository.LimitBasisKey to "lines",
                DefaultTextToolsSettingsRepository.PasswordLengthKey to "999",
                DefaultTextToolsSettingsRepository.PasswordUppercaseKey to false,
                DefaultTextToolsSettingsRepository.PasswordLowercaseKey to false,
                DefaultTextToolsSettingsRepository.PasswordDigitsKey to false,
                DefaultTextToolsSettingsRepository.PasswordSymbolsKey to false,
            ),
        )

        val repository = DefaultTextToolsSettingsRepository(store)
        assertEquals(TextTool.COUNT, repository.readSelectedTool())
        assertEquals(TextLimit.None, repository.readLimit())
        val options = repository.readPasswordOptions()
        assertEquals(PasswordOptions.MaxLength, options.length)
        // 켜진 종류가 없으면 소문자를 켠다.
        assertEquals(true, options.lowercase)
    }

    @Test
    fun observeFollowsWrites() = runTest {
        val repository = DefaultTextToolsSettingsRepository(InMemorySettingsStore())

        assertEquals(TextTool.COUNT, repository.observeSelectedTool().first())
        repository.setSelectedTool(TextTool.PASSWORD)
        assertEquals(TextTool.PASSWORD, repository.observeSelectedTool().first())

        repository.setInput("hello")
        assertEquals("hello", repository.observeInput().first())

        repository.setLimit(TextLimit(300, LimitBasis.WITHOUT_SPACES))
        assertEquals(TextLimit(300, LimitBasis.WITHOUT_SPACES), repository.observeLimit().first())

        repository.setPasswordOptions(PasswordOptions(length = 8, digits = false))
        assertEquals(PasswordOptions(length = 8, digits = false), repository.observePasswordOptions().first())
    }
}
