package io.github.taetae98coding.jarvis.designsystem

import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Icon
import androidx.compose.ui.graphics.vector.VectorPath
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import io.github.taetae98coding.jarvis.designsystem.icon.JarvisIcons
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class JarvisIconsTest {
    @Test
    fun everyIconIsA24dpOutline() = runComposeUiTest {
        JarvisIcons.all.forEach { icon ->
            assertEquals(24.dp, icon.defaultWidth, icon.name)
            assertEquals(24.dp, icon.defaultHeight, icon.name)
            assertEquals(24f, icon.viewportWidth, icon.name)
            assertEquals(24f, icon.viewportHeight, icon.name)

            val path = icon.root[0] as VectorPath
            assertTrue(path.pathData.isNotEmpty(), icon.name)
            assertEquals(null, path.fill, icon.name)
        }

        // 경로를 해석하는 것과 그리는 것은 다른 단계다. 한 번씩 그려서 둘 다 통과하는지 본다.
        setContent {
            Row {
                JarvisIcons.all.forEach { Icon(imageVector = it, contentDescription = it.name) }
            }
        }
        waitForIdle()
    }
}
