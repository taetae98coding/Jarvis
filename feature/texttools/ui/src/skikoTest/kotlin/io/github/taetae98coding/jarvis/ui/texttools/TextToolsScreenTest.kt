package io.github.taetae98coding.jarvis.ui.texttools

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.text.AnnotatedString
import io.github.taetae98coding.jarvis.designsystem.theme.JarvisTheme
import io.github.taetae98coding.jarvis.domain.texttools.LimitBasis
import io.github.taetae98coding.jarvis.domain.texttools.PasswordOptions
import io.github.taetae98coding.jarvis.domain.texttools.TextCounter
import io.github.taetae98coding.jarvis.domain.texttools.TextLimit
import io.github.taetae98coding.jarvis.domain.texttools.TextTool
import io.github.taetae98coding.jarvis.domain.texttools.TextTransform
import io.github.taetae98coding.jarvis.domain.texttools.TextTransformer
import kotlin.test.Test
import kotlin.test.assertEquals

/** docs/common/text-tools.html */
@OptIn(ExperimentalTestApi::class)
class TextToolsScreenTest {
    @Test
    fun tabSelectsTool() = runComposeUiTest {
        var tool by mutableStateOf(TextTool.COUNT)
        setScreen(tool = { tool }, onSelectTool = { tool = it })

        onNodeWithTag(textToolsTabTestTag(TextTool.COUNT)).assertIsSelected()
        onNodeWithTag(textToolsTabTestTag(TextTool.PASSWORD)).performClick()

        onNodeWithTag(textToolsTabTestTag(TextTool.PASSWORD)).assertIsSelected()
        onNodeWithTag(textToolsTabTestTag(TextTool.COUNT)).assertIsNotSelected()
        onNodeWithTag(TextToolsPasswordTestTag).assertExists()
        onNodeWithTag(TextToolsInputTestTag).assertDoesNotExist()
        assertEquals(TextTool.PASSWORD, tool)
    }

    @Test
    fun countTabShowsStats() = runComposeUiTest {
        setScreen(tool = { TextTool.COUNT }, input = "안녕 하세요\n\nHello")

        assertStat("with_spaces", "13자")
        assertStat("without_spaces", "10자")
        assertStat("words", "3개")
        assertStat("lines", "3줄")
        assertStat("paragraphs", "2개")
        assertStat("utf8_bytes", "23바이트")
        assertStat("korean_bytes", "18바이트")
        assertStat("reading_time", "1초")
    }

    @Test
    fun overLimitIsReported() = runComposeUiTest {
        setScreen(tool = { TextTool.COUNT }, input = "가나다라마", limit = TextLimit(3, LimitBasis.WITH_SPACES))

        onNodeWithTag(TextToolsProgressTestTag).performScrollTo().assertIsDisplayed()
        onNodeWithText("5 / 3자 · 2자 초과").assertExists()
        onNodeWithTag(textToolsBasisTestTag(LimitBasis.WITH_SPACES)).assertIsSelected()
    }

    @Test
    fun underLimitShowsProgressWithoutExcess() = runComposeUiTest {
        setScreen(tool = { TextTool.COUNT }, input = "가나", limit = TextLimit(1000, LimitBasis.KOREAN_BYTES))

        onNodeWithText("4 / 1,000바이트").assertExists()
    }

    @Test
    fun basisChipReportsSelection() = runComposeUiTest {
        var basis: LimitBasis? = null
        setScreen(tool = { TextTool.COUNT }, onLimitBasisChange = { basis = it })

        onNodeWithTag(textToolsBasisTestTag(LimitBasis.WITHOUT_SPACES)).performScrollTo().performClick()

        assertEquals(LimitBasis.WITHOUT_SPACES, basis)
    }

    @Test
    fun passwordCopyAndRegenerate() = runComposeUiTest {
        val clipboard = RecordingClipboardManager()
        var regenerated = 0
        setScreen(tool = { TextTool.PASSWORD }, password = "Abc123!@", clipboard = clipboard, onRegeneratePassword = { regenerated++ })

        onNodeWithText("Abc123!@", useUnmergedTree = true).assertExists()
        onNodeWithTag(TextToolsPasswordCopyTestTag).performClick()
        onNodeWithTag(TextToolsPasswordRegenerateTestTag).performClick()

        assertEquals(listOf("Abc123!@"), clipboard.copied)
        assertEquals(1, regenerated)
        onNodeWithText("강도: 매우 강함 (103비트)", useUnmergedTree = true).assertExists()
    }

    @Test
    fun lastCharacterClassSwitchIsLocked() = runComposeUiTest {
        val options = PasswordOptions(uppercase = false, lowercase = false, digits = true, symbols = false)
        setScreen(tool = { TextTool.PASSWORD }, passwordOptions = options)

        onNodeWithTag(textToolsPasswordOptionTestTag("digits")).performScrollTo().assertIsNotEnabled()
        onNodeWithTag(textToolsPasswordOptionTestTag("uppercase")).assertIsEnabled()
        onNodeWithText("강도: 보통 (53비트)", useUnmergedTree = true).assertExists()
    }

    @Test
    fun switchAndSliderChangeOptions() = runComposeUiTest {
        var changed: PasswordOptions? = null
        setScreen(tool = { TextTool.PASSWORD }, onPasswordOptionsChange = { changed = it })

        onNodeWithTag(textToolsPasswordOptionTestTag("symbols")).performScrollTo().performClick()
        assertEquals(PasswordOptions(symbols = false), changed)

        onNodeWithTag(TextToolsPasswordLengthTestTag).performSemanticsAction(SemanticsActions.SetProgress) { it(32f) }
        assertEquals(PasswordOptions(length = 32), changed)
    }

    @Test
    fun caseTabShowsTransformsAndCopies() = runComposeUiTest {
        val clipboard = RecordingClipboardManager()
        setScreen(tool = { TextTool.CASE }, input = "hello world", clipboard = clipboard)

        onNode(hasText("helloWorld") and hasAnyAncestor(hasTestTag(textToolsTransformTestTag(TextTransform.CAMEL_CASE))), useUnmergedTree = true)
            .performScrollTo()
            .assertExists()
        onNodeWithTag(textToolsTransformCopyTestTag(TextTransform.SNAKE_CASE)).performScrollTo().performClick()

        assertEquals(listOf("hello_world"), clipboard.copied)
    }

    @Test
    fun caseTabWithEmptyInputHasNoRows() = runComposeUiTest {
        setScreen(tool = { TextTool.CASE }, input = "")

        onNodeWithTag(TextToolsInputTestTag).assertExists()
        onNodeWithTag(textToolsTransformTestTag(TextTransform.UPPERCASE)).assertDoesNotExist()
    }

    private fun ComposeUiTest.assertStat(kind: String, value: String) {
        onNode(hasText(value) and hasAnyAncestor(hasTestTag(textToolsStatTestTag(kind))), useUnmergedTree = true).assertExists()
    }

    private fun ComposeUiTest.setScreen(
        tool: () -> TextTool,
        input: String = "",
        onSelectTool: (TextTool) -> Unit = {},
        limit: TextLimit = TextLimit.None,
        onLimitBasisChange: (LimitBasis) -> Unit = {},
        password: String = "password",
        passwordOptions: PasswordOptions = PasswordOptions(),
        onPasswordOptionsChange: (PasswordOptions) -> Unit = {},
        onRegeneratePassword: () -> Unit = {},
        clipboard: RecordingClipboardManager = RecordingClipboardManager(),
    ) {
        val inputState = TextFieldState(input)
        val limitState = TextFieldState(limit.target?.toString().orEmpty())
        val stats = TextCounter.count(input)

        setContent {
            CompositionLocalProvider(LocalClipboardManager provides clipboard) {
                JarvisTheme {
                    TextToolsScreen(
                        tool = tool(),
                        onSelectTool = onSelectTool,
                        onBack = {},
                        input = inputState,
                        stats = stats,
                        limitInput = limitState,
                        limitBasis = limit.basis,
                        onLimitBasisChange = onLimitBasisChange,
                        progress = limit.progress(stats),
                        password = password,
                        passwordOptions = passwordOptions,
                        onPasswordOptionsChange = onPasswordOptionsChange,
                        onRegeneratePassword = onRegeneratePassword,
                        transforms = TextTransformer.transformAll(input),
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
