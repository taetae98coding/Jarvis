package io.github.taetae98coding.jarvis.ui.app

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.runComposeUiTest
import io.github.taetae98coding.jarvis.designsystem.component.JarvisTopBarDefaults
import io.github.taetae98coding.jarvis.domain.texttools.TextTool
import io.github.taetae98coding.jarvis.ui.texttools.TextToolsInputTestTag
import io.github.taetae98coding.jarvis.ui.texttools.TextToolsPasswordTestTag
import io.github.taetae98coding.jarvis.ui.texttools.TextToolsScreenTestTag
import io.github.taetae98coding.jarvis.ui.texttools.TextToolsTestTag
import io.github.taetae98coding.jarvis.ui.texttools.textToolsStatTestTag
import io.github.taetae98coding.jarvis.ui.texttools.textToolsTabTestTag
import kotlin.test.Test
import kotlin.test.assertEquals

/** docs/common/text-tools.html */
@OptIn(ExperimentalTestApi::class)
class JarvisAppTextToolsTest {
    @Test
    fun cardOpensScreenAndBackReturnsHome() = runComposeUiTest {
        setContent { TestJarvisApp() }

        openTextTools()
        onNodeWithTag(TextToolsScreenTestTag).assertIsDisplayed()

        onNodeWithContentDescription(JarvisTopBarDefaults.BackContentDescription).performClick()
        onNode(hasScrollToNodeAction()).performScrollToNode(hasTestTag(TextToolsTestTag))
        onNodeWithTag(TextToolsTestTag).assertIsDisplayed()
    }

    @Test
    fun typedInputIsCountedAndPersisted() = runComposeUiTest {
        val textTools = FakeTextToolsSettingsRepository()
        setContent { TestJarvisApp(textTools = textTools) }

        openTextTools()
        onNodeWithTag(TextToolsInputTestTag).performTextInput("자기소개서 첫 줄")

        onNode(hasText("9자") and hasAnyAncestor(hasTestTag(textToolsStatTestTag("with_spaces"))), useUnmergedTree = true).assertExists()
        assertEquals("자기소개서 첫 줄", textTools.input.value)

        onNodeWithTag(textToolsTabTestTag(TextTool.PASSWORD)).performClick()
        onNodeWithTag(TextToolsPasswordTestTag).assertExists()
        assertEquals(TextTool.PASSWORD, textTools.tool.value)
    }

    @Test
    fun savedTabAndInputAreRestored() = runComposeUiTest {
        val textTools = FakeTextToolsSettingsRepository().apply {
            tool.value = TextTool.CASE
            input.value = "hello world"
        }
        setContent { TestJarvisApp(textTools = textTools) }

        openTextTools()

        onNodeWithTag(textToolsTabTestTag(TextTool.CASE)).assertIsSelected()
        onNodeWithTag(TextToolsInputTestTag).assertTextContains("hello world")
        onNode(hasText("hello_world"), useUnmergedTree = true).assertExists()
    }

    private fun ComposeUiTest.openTextTools() {
        // 카드가 그리드 끝이라 창이 작으면 아직 구성되지 않았을 수 있다.
        onNode(hasScrollToNodeAction()).performScrollToNode(hasTestTag(TextToolsTestTag))
        onNodeWithTag(TextToolsTestTag).performClick()
    }
}
