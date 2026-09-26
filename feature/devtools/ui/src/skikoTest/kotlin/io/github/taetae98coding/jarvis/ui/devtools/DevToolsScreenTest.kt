package io.github.taetae98coding.jarvis.ui.devtools

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.text.AnnotatedString
import io.github.taetae98coding.jarvis.designsystem.theme.JarvisTheme
import io.github.taetae98coding.jarvis.domain.devtools.ConvertDevToolInputUseCase
import io.github.taetae98coding.jarvis.domain.devtools.DevTool
import io.github.taetae98coding.jarvis.domain.devtools.DevToolOutputKind
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class DevToolsScreenTest {
    private val convert = ConvertDevToolInputUseCase()

    @Test
    fun chipSelectsTool() = runComposeUiTest {
        var tool by mutableStateOf(DevTool.TIMESTAMP)
        setScreen(tool = { tool }, onSelectTool = { tool = it }, input = "")

        onNodeWithTag(devToolsToolTestTag(DevTool.TIMESTAMP)).assertIsSelected()
        onNodeWithTag(devToolsToolTestTag(DevTool.HASH)).performClick()

        onNodeWithTag(devToolsToolTestTag(DevTool.HASH)).assertIsSelected()
        onNodeWithTag(devToolsToolTestTag(DevTool.TIMESTAMP)).assertIsNotSelected()
        assertEquals(DevTool.HASH, tool)
    }

    @Test
    fun copyButtonPutsValueOnClipboard() = runComposeUiTest {
        val clipboard = RecordingClipboardManager()
        setScreen(tool = { DevTool.HASH }, input = "abc", clipboard = clipboard)

        onNodeWithTag(devToolsOutputTestTag(DevToolOutputKind.SHA256)).assertIsDisplayed()
        onNodeWithTag(devToolsCopyTestTag(DevToolOutputKind.MD5)).performClick()

        assertEquals(listOf("900150983cd24fb0d6963f7d28e17f72"), clipboard.copied)
    }

    @Test
    fun errorRowHasNoCopyButton() = runComposeUiTest {
        setScreen(tool = { DevTool.JSON }, input = "{")

        // 정렬·한 줄 두 줄이 같은 오류를 보인다.
        onAllNodesWithText("1 줄 2 칸: 입력이 중간에 끝남", useUnmergedTree = true).assertCountEquals(2)
        onNodeWithTag(devToolsCopyTestTag(DevToolOutputKind.JSON_PRETTY)).assertDoesNotExist()
        onNodeWithTag(devToolsCopyTestTag(DevToolOutputKind.JSON_MINIFIED)).assertDoesNotExist()
    }

    @Test
    fun timestampUnitIsShownInKorean() = runComposeUiTest {
        setScreen(tool = { DevTool.TIMESTAMP }, input = "1700000000")

        onNodeWithText("초", useUnmergedTree = true).assertExists()
        onNodeWithText("2023-11-14T22:13:20Z", useUnmergedTree = true).assertExists()
    }

    @Test
    fun colorShowsSwatch() = runComposeUiTest {
        setScreen(tool = { DevTool.COLOR }, input = "#2E5DAA")

        onNodeWithTag(DevToolsSwatchTestTag).assertExists()
        onNodeWithText("hsl(217, 57%, 42%)", useUnmergedTree = true).assertExists()
    }

    @Test
    fun uuidToolShowsGeneratorInsteadOfInput() = runComposeUiTest {
        var generated = 0
        setScreen(tool = { DevTool.UUID }, input = "", uuids = listOf("a", "b"), onGenerateUuids = { generated++ })

        onNodeWithTag(DevToolsInputTestTag).assertDoesNotExist()
        onNodeWithText("a\nb", useUnmergedTree = true).assertExists()
        onNodeWithTag(DevToolsGenerateTestTag).performClick()
        assertEquals(1, generated)
    }

    private fun androidx.compose.ui.test.ComposeUiTest.setScreen(
        tool: () -> DevTool,
        input: String,
        onSelectTool: (DevTool) -> Unit = {},
        clipboard: RecordingClipboardManager = RecordingClipboardManager(),
        uuids: List<String> = emptyList(),
        onGenerateUuids: () -> Unit = {},
    ) {
        val state = TextFieldState(input)

        setContent {
            CompositionLocalProvider(LocalClipboardManager provides clipboard) {
                JarvisTheme {
                    val current = tool()
                    DevToolsScreen(
                        tool = current,
                        input = state,
                        results = DevToolResults(
                            outputs = convert(current, input, base64UrlSafe = false),
                            swatch = convert.swatch(current, input),
                        ),
                        onSelectTool = onSelectTool,
                        onBack = {},
                        onNow = {},
                        base64UrlSafe = false,
                        onBase64UrlSafeChange = {},
                        uuids = uuids,
                        uuidCount = 1,
                        onUuidCountChange = {},
                        uuidUppercase = false,
                        onUuidUppercaseChange = {},
                        onGenerateUuids = onGenerateUuids,
                    )
                }
            }
        }
    }
}

// 기본 관리자(AwtClipboardManager 등)는 테스트 중에 실제 시스템 클립보드를 덮어쓴다. 기록만 하는 것으로 바꾼다.
@Suppress("DEPRECATION")
private class RecordingClipboardManager : ClipboardManager {
    val copied = mutableListOf<String>()

    override fun setText(annotatedString: AnnotatedString) {
        copied += annotatedString.text
    }

    override fun getText(): AnnotatedString? = copied.lastOrNull()?.let(::AnnotatedString)
}
