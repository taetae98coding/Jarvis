package io.github.taetae98coding.jarvis.designsystem

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import io.github.taetae98coding.jarvis.designsystem.theme.JarvisColors
import io.github.taetae98coding.jarvis.designsystem.theme.JarvisDarkColorScheme
import io.github.taetae98coding.jarvis.designsystem.theme.JarvisDarkColors
import io.github.taetae98coding.jarvis.designsystem.theme.JarvisLightColorScheme
import io.github.taetae98coding.jarvis.designsystem.theme.JarvisShapes
import io.github.taetae98coding.jarvis.designsystem.theme.JarvisTheme
import io.github.taetae98coding.jarvis.designsystem.theme.JarvisTypography
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

@OptIn(ExperimentalTestApi::class)
class JarvisThemeTest {
    @Test
    fun materialThemeFollowsJarvisTokens() = runComposeUiTest {
        lateinit var colorScheme: ColorScheme
        lateinit var typography: Typography
        lateinit var shapes: Shapes

        setContent {
            JarvisTheme(darkTheme = false) {
                colorScheme = MaterialTheme.colorScheme
                typography = MaterialTheme.typography
                shapes = MaterialTheme.shapes
            }
        }

        assertEquals(JarvisLightColorScheme.primary, colorScheme.primary)
        assertEquals(JarvisLightColorScheme.surfaceContainerHighest, colorScheme.surfaceContainerHighest)
        assertEquals(JarvisTypography, typography)
        assertEquals(JarvisShapes, shapes)
    }

    @Test
    fun darkThemeSwapsColors() = runComposeUiTest {
        lateinit var materialColorScheme: ColorScheme
        lateinit var jarvisColorScheme: ColorScheme
        lateinit var colors: JarvisColors

        setContent {
            JarvisTheme(darkTheme = true) {
                materialColorScheme = MaterialTheme.colorScheme
                jarvisColorScheme = JarvisTheme.colorScheme
                colors = JarvisTheme.colors
            }
        }

        assertEquals(JarvisDarkColorScheme.primary, materialColorScheme.primary)
        assertEquals(JarvisDarkColorScheme.surface, materialColorScheme.surface)
        assertSame(JarvisDarkColorScheme, jarvisColorScheme)
        assertSame(JarvisDarkColors, colors)
    }
}
