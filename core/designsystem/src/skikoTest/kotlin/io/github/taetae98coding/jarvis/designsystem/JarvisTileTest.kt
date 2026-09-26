package io.github.taetae98coding.jarvis.designsystem

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.height
import io.github.taetae98coding.jarvis.designsystem.component.JarvisTile
import io.github.taetae98coding.jarvis.designsystem.icon.JarvisIcons
import io.github.taetae98coding.jarvis.designsystem.theme.JarvisTheme
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class JarvisTileTest {
    // 라벨 줄 수와 무관하게 높이가 같아야 그리드 한 줄의 타일 끝선이 맞는다(docs/common/home-adaptive-layout.html).
    @Test
    fun tileHeightIsFixedRegardlessOfLabelLength() = runComposeUiTest {
        setContent {
            TestTile(label = "짧음", tag = ShortTag)
            TestTile(label = "아주 길어서 두 줄을 넘기고도 남는 라벨 문구가 여기에 있다", tag = LongTag)
        }

        val short = onNodeWithTag(ShortTag).getBoundsInRoot().height
        val long = onNodeWithTag(LongTag).getBoundsInRoot().height

        assertEquals(112f, short.value, absoluteTolerance = 0.5f)
        assertEquals(short, long)
    }

    @Test
    fun lockedTileDoesNotClick() = runComposeUiTest {
        var clicks = 0
        setContent { TestTile(label = "잠김", tag = ShortTag, enabled = false, onClick = { clicks++ }) }

        onNodeWithTag(ShortTag).assertIsNotEnabled().performClick()

        assertEquals(0, clicks)
    }

    @Test
    fun tileClicks() = runComposeUiTest {
        var clicks = 0
        setContent { TestTile(label = "열림", tag = ShortTag, onClick = { clicks++ }) }

        onNodeWithTag(ShortTag).assertIsEnabled().performClick()

        assertEquals(1, clicks)
    }

    @Composable
    private fun TestTile(
        label: String,
        tag: String,
        enabled: Boolean = true,
        onClick: () -> Unit = {},
    ) {
        JarvisTheme(darkTheme = false) {
            JarvisTile(
                icon = JarvisIcons.Eye,
                label = label,
                onClick = onClick,
                modifier = Modifier.testTag(tag),
                enabled = enabled,
            )
        }
    }

    private companion object {
        const val ShortTag = "short"
        const val LongTag = "long"
    }
}
