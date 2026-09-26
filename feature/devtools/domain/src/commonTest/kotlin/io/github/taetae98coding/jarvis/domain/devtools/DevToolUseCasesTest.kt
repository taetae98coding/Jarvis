package io.github.taetae98coding.jarvis.domain.devtools

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Instant

class DevToolUseCasesTest {
    private val convert = ConvertDevToolInputUseCase()

    @Test
    fun convertDispatchesByTool() {
        assertEquals(DevToolOutputKind.TIMESTAMP_UNIT, convert(DevTool.TIMESTAMP, "0", base64UrlSafe = false).first().kind)
        assertEquals(DevToolValue.Text("Pz4_"), convert(DevTool.BASE64, "?>?", base64UrlSafe = true).first().value)
        assertEquals(DevToolOutputKind.URL_ENCODED, convert(DevTool.URL, "a", base64UrlSafe = false).first().kind)
        assertEquals(DevToolOutputKind.JSON_PRETTY, convert(DevTool.JSON, "1", base64UrlSafe = false).first().kind)
        assertEquals(3, convert(DevTool.HASH, "", base64UrlSafe = false).size)
        assertEquals(DevToolOutputKind.HEX, convert(DevTool.COLOR, "#fff", base64UrlSafe = false).first().kind)
        assertEquals(emptyList(), convert(DevTool.UUID, "anything", base64UrlSafe = false))
    }

    @Test
    fun swatchOnlyForColor() {
        assertEquals(RgbColor(255, 255, 255), convert.swatch(DevTool.COLOR, "#fff"))
        assertNull(convert.swatch(DevTool.COLOR, "nope"))
        assertNull(convert.swatch(DevTool.HASH, "#fff"))
    }

    @Test
    fun generatedUuidsAreVersion4() {
        val uuids = GenerateUuidsUseCase()(count = 10, uppercase = false)

        assertEquals(10, uuids.size)
        assertEquals(10, uuids.toSet().size)
        uuids.forEach { uuid ->
            assertTrue(Regex("[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}").matches(uuid), uuid)
        }
    }

    @Test
    fun uuidCaseAndCountLimits() {
        val generate = GenerateUuidsUseCase { "AbC" }

        assertEquals(listOf("ABC"), generate(count = 0, uppercase = true))
        assertEquals(listOf("abc", "abc"), generate(count = 2, uppercase = false))
        assertEquals(GenerateUuidsUseCase.MaxUuidCount, generate(count = 1_000, uppercase = false).size)
    }

    @Test
    fun currentEpochSecondsReadsClock() {
        val clock = object : Clock {
            override fun now(): Instant = Instant.fromEpochMilliseconds(1_700_000_000_999)
        }

        assertEquals(1_700_000_000, GetCurrentEpochSecondsUseCase(clock)())
    }

    @Test
    fun stateFollowsSelectedToolAndItsInput() = runTest {
        val settings = FakeDevToolsSettingsRepository()
        settings.setInput(DevTool.JSON, "{}")
        settings.setSelectedTool(DevTool.JSON)

        val state = ObserveDevToolStateUseCase(settings)(backgroundScope)
        assertEquals(DevToolState(DevTool.JSON, "{}"), state.value)

        SelectDevToolUseCase(settings)(DevTool.HASH)
        SetDevToolInputUseCase(settings)(DevTool.HASH, "abc")
        assertEquals(DevToolState(DevTool.HASH, "abc"), state.first { it.tool == DevTool.HASH && it.input == "abc" })
    }
}

private class FakeDevToolsSettingsRepository : DevToolsSettingsRepository {
    private val tool = MutableStateFlow(DevTool.TIMESTAMP)
    private val inputs = MutableStateFlow(emptyMap<DevTool, String>())

    override fun observeSelectedTool() = tool

    override fun readSelectedTool(): DevTool = tool.value

    override fun setSelectedTool(tool: DevTool) {
        this.tool.value = tool
    }

    override fun observeInput(tool: DevTool) = inputs.map { it[tool].orEmpty() }

    override fun readInput(tool: DevTool): String = inputs.value[tool].orEmpty()

    override fun setInput(tool: DevTool, input: String) {
        inputs.value += tool to input
    }
}
