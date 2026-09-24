package io.github.taetae98coding.jarvis.designsystem

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.style.Style
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.taetae98coding.jarvis.designsystem.component.JarvisCard
import io.github.taetae98coding.jarvis.designsystem.theme.JarvisTheme
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class JarvisCardTest {
    @Test
    fun defaultStylePadsContent() = runComposeUiTest {
        setContent { TestCard(style = Style) }

        val (start, top) = contentInset()

        assertEquals(16f, start.value, absoluteTolerance = 0.5f)
        assertEquals(16f, top.value, absoluteTolerance = 0.5f)
    }

    @Test
    fun callerStyleOverridesOnlyWhatItSets() = runComposeUiTest {
        setContent { TestCard(style = Style { contentPaddingStart(24.dp) }) }

        val (start, top) = contentInset()

        assertEquals(24f, start.value, absoluteTolerance = 0.5f)
        // 호출자가 정하지 않은 위쪽 여백은 JarvisCardDefaults.style 의 값이 남는다.
        assertEquals(16f, top.value, absoluteTolerance = 0.5f)
    }

    @androidx.compose.runtime.Composable
    private fun TestCard(style: Style) {
        JarvisTheme(darkTheme = false) {
            JarvisCard(modifier = Modifier.testTag(CardTag), style = style) {
                Box(modifier = Modifier.size(10.dp).testTag(ContentTag))
            }
        }
    }

    private fun androidx.compose.ui.test.ComposeUiTest.contentInset(): Pair<Dp, Dp> {
        val card = onNodeWithTag(CardTag).getBoundsInRoot()
        val content = onNodeWithTag(ContentTag).getBoundsInRoot()

        return (content.left - card.left) to (content.top - card.top)
    }

    private companion object {
        const val CardTag = "card"
        const val ContentTag = "content"
    }
}
