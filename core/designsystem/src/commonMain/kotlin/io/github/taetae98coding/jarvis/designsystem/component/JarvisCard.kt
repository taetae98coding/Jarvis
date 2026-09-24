package io.github.taetae98coding.jarvis.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.style.Style
import androidx.compose.foundation.style.contentPadding
import androidx.compose.foundation.style.styleable
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import io.github.taetae98coding.jarvis.designsystem.theme.JarvisTheme
import io.github.taetae98coding.jarvis.designsystem.theme.jarvisDimens

/**
 * 내용 영역은 [JarvisCardDefaults.style] 위에 [style] 을 덮어 그린다. 넘긴 Style 이 정하지 않은 속성은
 * 기본값이 남는다.
 */
@Composable
fun JarvisCard(
    modifier: Modifier = Modifier,
    style: Style = Style,
    colors: CardColors = JarvisCardDefaults.colors(),
    verticalArrangement: Arrangement.Vertical = JarvisCardDefaults.contentArrangement,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(modifier = modifier, shape = JarvisCardDefaults.shape, colors = colors) {
        JarvisCardContent(style, verticalArrangement, content)
    }
}

@Composable
fun JarvisCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    style: Style = Style,
    colors: CardColors = JarvisCardDefaults.colors(),
    verticalArrangement: Arrangement.Vertical = JarvisCardDefaults.contentArrangement,
    content: @Composable ColumnScope.() -> Unit,
) {
    // 클릭을 Card 에 넘기면 물결이 카드 안쪽 Surface 에서 그려져 카드 모양대로 잘린다. 바깥에
    // Modifier.clickable 을 붙이면 사각형이 된다.
    Card(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = JarvisCardDefaults.shape,
        colors = colors,
    ) {
        JarvisCardContent(style, verticalArrangement, content)
    }
}

@Composable
private fun JarvisCardContent(
    style: Style,
    verticalArrangement: Arrangement.Vertical,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier.styleable(null, JarvisCardDefaults.style, style),
        verticalArrangement = verticalArrangement,
        content = content,
    )
}

object JarvisCardDefaults {
    val shape: Shape
        @Composable @ReadOnlyComposable get() = JarvisTheme.shapes.medium

    val contentArrangement: Arrangement.Vertical
        @Composable @ReadOnlyComposable get() = Arrangement.spacedBy(JarvisTheme.dimens.spacing.s)

    val style: Style = Style {
        contentPadding(jarvisDimens.spacing.l)
    }

    @Composable
    fun colors(
        containerColor: Color = JarvisTheme.colorScheme.surfaceContainerHighest,
        contentColor: Color = JarvisTheme.colorScheme.onSurface,
    ): CardColors = CardDefaults.cardColors(containerColor = containerColor, contentColor = contentColor)
}
