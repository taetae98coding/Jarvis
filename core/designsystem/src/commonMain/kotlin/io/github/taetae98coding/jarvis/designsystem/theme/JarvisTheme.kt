package io.github.taetae98coding.jarvis.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalAccessorScope
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle

/**
 * 앱의 루트 테마. 안에서 [MaterialTheme] 을 부르므로 M3 컴포넌트는 인자 없이도 Jarvis 값으로 그려진다.
 *
 * 토큰 값과 결정은 docs/common/design-system.html 에 있다.
 */
@Composable
fun JarvisTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) JarvisDarkColorScheme else JarvisLightColorScheme

    CompositionLocalProvider(
        LocalJarvisColorScheme provides colorScheme,
        LocalJarvisColors provides if (darkTheme) JarvisDarkColors else JarvisLightColors,
        LocalJarvisTypography provides JarvisTypography,
        LocalJarvisCodeTextStyle provides JarvisCodeTextStyle,
        LocalJarvisShapes provides JarvisShapes,
        LocalJarvisDimens provides JarvisDimens(),
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = JarvisTypography,
            shapes = JarvisShapes,
            content = content,
        )
    }
}

object JarvisTheme {
    val colorScheme: ColorScheme
        @Composable @ReadOnlyComposable get() = LocalJarvisColorScheme.current

    val colors: JarvisColors
        @Composable @ReadOnlyComposable get() = LocalJarvisColors.current

    val typography: Typography
        @Composable @ReadOnlyComposable get() = LocalJarvisTypography.current

    val codeTextStyle: TextStyle
        @Composable @ReadOnlyComposable get() = LocalJarvisCodeTextStyle.current

    val shapes: Shapes
        @Composable @ReadOnlyComposable get() = LocalJarvisShapes.current

    val dimens: JarvisDimens
        @Composable @ReadOnlyComposable get() = LocalJarvisDimens.current
}

// Style 블록은 컴포지션 밖에서 실행되어 JarvisTheme 의 @Composable getter 를 부를 수 없다. 같은 값을
// CompositionLocalAccessorScope 로 읽는 통로다. M3 의 LocalColorScheme 은 internal 이라 색도 우리
// local 에 한 번 더 넣어 둔다.
val CompositionLocalAccessorScope.jarvisColorScheme: ColorScheme
    get() = LocalJarvisColorScheme.currentValue

val CompositionLocalAccessorScope.jarvisColors: JarvisColors
    get() = LocalJarvisColors.currentValue

val CompositionLocalAccessorScope.jarvisShapes: Shapes
    get() = LocalJarvisShapes.currentValue

val CompositionLocalAccessorScope.jarvisDimens: JarvisDimens
    get() = LocalJarvisDimens.currentValue

// JarvisTheme 밖(프리뷰, 컴포넌트 단위 테스트)에서도 그릴 수 있게 라이트 값을 기본으로 둔다.
private val LocalJarvisColorScheme = staticCompositionLocalOf { JarvisLightColorScheme }
private val LocalJarvisColors = staticCompositionLocalOf { JarvisLightColors }
private val LocalJarvisTypography = staticCompositionLocalOf { JarvisTypography }
private val LocalJarvisCodeTextStyle = staticCompositionLocalOf { JarvisCodeTextStyle }
private val LocalJarvisShapes = staticCompositionLocalOf { JarvisShapes }
private val LocalJarvisDimens = staticCompositionLocalOf { JarvisDimens() }
