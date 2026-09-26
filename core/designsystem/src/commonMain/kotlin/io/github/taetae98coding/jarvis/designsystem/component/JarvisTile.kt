package io.github.taetae98coding.jarvis.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import io.github.taetae98coding.jarvis.designsystem.theme.JarvisTheme

/**
 * 아이콘 하나와 라벨 한 줄뿐인 누를 수 있는 카드. 좁은 창의 홈이 기능 카드 대신 놓는다.
 *
 * 높이는 [JarvisTileDefaults.height] 로 고정한다. 그리드는 한 줄의 항목을 같은 높이로 늘리지 않아서, 라벨이
 * 한 줄인 타일과 두 줄인 타일이 나란히 놓이면 아래 끝선이 어긋난다. 아이콘은 장식이라 설명을 달지 않고 라벨이
 * 같은 것을 말한다.
 */
@Composable
fun JarvisTile(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    iconColor: Color = JarvisTileDefaults.iconColor(enabled),
    labelStyle: TextStyle = JarvisTileDefaults.labelStyle,
) {
    JarvisCard(
        onClick = onClick,
        modifier = modifier.height(JarvisTileDefaults.height),
        enabled = enabled,
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(JarvisTileDefaults.spacing, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(JarvisTileDefaults.iconSize),
                tint = iconColor,
            )

            Text(
                text = label,
                style = labelStyle,
                textAlign = TextAlign.Center,
                maxLines = JarvisTileDefaults.LabelMaxLines,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

object JarvisTileDefaults {
    /** 라벨은 두 줄까지다. 높이 계산이 이 값을 전제로 한다(JarvisLayout.tileHeight). */
    const val LabelMaxLines = 2

    val height: Dp
        @Composable @ReadOnlyComposable get() = JarvisTheme.dimens.layout.tileHeight

    val iconSize: Dp
        @Composable @ReadOnlyComposable get() = JarvisTheme.dimens.iconSize.large

    val spacing: Dp
        @Composable @ReadOnlyComposable get() = JarvisTheme.dimens.spacing.s

    val labelStyle: TextStyle
        @Composable @ReadOnlyComposable get() = JarvisTheme.typography.bodyMedium

    // 카드 머리와 같은 이유다. 잠긴 타일은 Card 가 흐린 색으로 바꾼 LocalContentColor 를 따라야 아이콘만 눌릴 수
    // 있는 것처럼 보이지 않는다.
    @Composable
    @ReadOnlyComposable
    fun iconColor(enabled: Boolean): Color =
        if (enabled) JarvisTheme.colorScheme.primary else LocalContentColor.current
}
