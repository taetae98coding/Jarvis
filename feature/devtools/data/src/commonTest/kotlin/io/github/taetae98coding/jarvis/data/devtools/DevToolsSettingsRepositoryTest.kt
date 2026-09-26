package io.github.taetae98coding.jarvis.data.devtools

import io.github.taetae98coding.jarvis.data.settings.InMemorySettingsStore
import io.github.taetae98coding.jarvis.domain.devtools.DevTool
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class DevToolsSettingsRepositoryTest {
    @Test
    fun defaultsToTimestampAndEmptyInput() {
        val repository = DefaultDevToolsSettingsRepository(InMemorySettingsStore())

        assertEquals(DevTool.TIMESTAMP, repository.readSelectedTool())
        assertEquals("", repository.readInput(DevTool.JSON))
    }

    @Test
    fun selectedToolSurvivesNewRepository() {
        val store = InMemorySettingsStore()
        DefaultDevToolsSettingsRepository(store).setSelectedTool(DevTool.COLOR)

        assertEquals(DevTool.COLOR, DefaultDevToolsSettingsRepository(store).readSelectedTool())
        assertEquals("color", store.getString(DefaultDevToolsSettingsRepository.SelectedToolKey, ""))
    }

    @Test
    fun inputIsKeptPerTool() {
        val store = InMemorySettingsStore()
        val repository = DefaultDevToolsSettingsRepository(store)
        repository.setInput(DevTool.JSON, "{\"a\":1}")
        repository.setInput(DevTool.HASH, "abc")

        val reopened = DefaultDevToolsSettingsRepository(store)
        assertEquals("{\"a\":1}", reopened.readInput(DevTool.JSON))
        assertEquals("abc", reopened.readInput(DevTool.HASH))
        assertEquals("", reopened.readInput(DevTool.URL))
    }

    @Test
    fun unknownStoredToolFallsBack() {
        val store = InMemorySettingsStore(mutableMapOf<String, Any>(DefaultDevToolsSettingsRepository.SelectedToolKey to "removed-tool"))

        assertEquals(DevTool.TIMESTAMP, DefaultDevToolsSettingsRepository(store).readSelectedTool())
    }

    @Test
    fun observeFollowsWrites() = runTest {
        val repository = DefaultDevToolsSettingsRepository(InMemorySettingsStore())

        assertEquals(DevTool.TIMESTAMP, repository.observeSelectedTool().first())
        repository.setSelectedTool(DevTool.BASE64)
        assertEquals(DevTool.BASE64, repository.observeSelectedTool().first())

        repository.setInput(DevTool.BASE64, "aGk=")
        assertEquals("aGk=", repository.observeInput(DevTool.BASE64).first())
    }
}
