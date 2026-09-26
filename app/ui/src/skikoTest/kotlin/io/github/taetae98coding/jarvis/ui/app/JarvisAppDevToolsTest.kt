package io.github.taetae98coding.jarvis.ui.app

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.runComposeUiTest
import io.github.taetae98coding.jarvis.designsystem.component.JarvisTopBarDefaults
import io.github.taetae98coding.jarvis.domain.devtools.DevTool
import io.github.taetae98coding.jarvis.domain.devtools.DevToolOutputKind
import io.github.taetae98coding.jarvis.ui.devtools.DevToolsInputTestTag
import io.github.taetae98coding.jarvis.ui.devtools.DevToolsScreenTestTag
import io.github.taetae98coding.jarvis.ui.devtools.DevToolsTestTag
import io.github.taetae98coding.jarvis.ui.devtools.devToolsCopyTestTag
import io.github.taetae98coding.jarvis.ui.devtools.devToolsOutputTestTag
import io.github.taetae98coding.jarvis.ui.devtools.devToolsToolTestTag
import kotlin.test.Test
import kotlin.test.assertEquals

/** docs/common/dev-utilities.html */
@OptIn(ExperimentalTestApi::class)
class JarvisAppDevToolsTest {
    @Test
    fun cardOpensScreenAndBackReturnsHome() = runComposeUiTest {
        setContent { TestJarvisApp() }

        onNodeWithTag(DevToolsTestTag).performClick()
        onNodeWithTag(DevToolsScreenTestTag).assertIsDisplayed()

        onNodeWithContentDescription(JarvisTopBarDefaults.BackContentDescription).performClick()
        onNodeWithTag(DevToolsTestTag).assertIsDisplayed()
    }

    @Test
    fun typedInputIsConvertedPersistedAndCopied() = runComposeUiTest {
        val devTools = FakeDevToolsSettingsRepository()
        val clipboard = RecordingClipboardManager()
        setContent { TestJarvisApp(devTools = devTools, clipboard = clipboard) }

        onNodeWithTag(DevToolsTestTag).performClick()
        onNodeWithTag(devToolsToolTestTag(DevTool.HASH)).performClick().assertIsSelected()
        onNodeWithTag(DevToolsInputTestTag).performTextInput("abc")

        onNode(hasText("900150983cd24fb0d6963f7d28e17f72") and hasAnyAncestor(hasTestTag(devToolsOutputTestTag(DevToolOutputKind.MD5))), useUnmergedTree = true)
            .assertExists()
        onNodeWithTag(devToolsCopyTestTag(DevToolOutputKind.MD5)).performClick()

        assertEquals(DevTool.HASH, devTools.tool.value)
        assertEquals("abc", devTools.inputs.value[DevTool.HASH])
        assertEquals(listOf("900150983cd24fb0d6963f7d28e17f72"), clipboard.copied)
    }

    @Test
    fun savedToolAndInputAreRestored() = runComposeUiTest {
        val devTools = FakeDevToolsSettingsRepository().apply {
            tool.value = DevTool.COLOR
            inputs.value = mapOf(DevTool.COLOR to "#abc")
        }
        setContent { TestJarvisApp(devTools = devTools) }

        onNodeWithTag(DevToolsTestTag).performClick()

        onNodeWithTag(devToolsToolTestTag(DevTool.COLOR)).assertIsSelected()
        onNodeWithTag(DevToolsInputTestTag).assertTextContains("#abc")
        onNode(hasText("#AABBCC") and hasAnyAncestor(hasTestTag(devToolsOutputTestTag(DevToolOutputKind.HEX))), useUnmergedTree = true)
            .assertExists()
    }
}
