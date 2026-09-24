package io.github.taetae98coding.jarvis.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.unit.Dp
import io.github.taetae98coding.jarvis.designsystem.theme.JarvisTheme

/** 카드 첫 줄. 아이콘은 장식이라 설명을 달지 않는다. 제목이 같은 것을 말한다. */
@Composable
fun JarvisCardHeader(
    title: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    iconColor: Color = JarvisCardHeaderDefaults.iconColor(enabled),
    textStyle: TextStyle = JarvisCardHeaderDefaults.textStyle,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(JarvisCardHeaderDefaults.spacing),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(JarvisCardHeaderDefaults.iconSize),
                tint = iconColor,
            )
        }

        Text(text = title, style = textStyle, modifier = Modifier.weight(1f))

        trailing?.invoke()
    }
}

object JarvisCardHeaderDefaults {
    val textStyle: TextStyle
        @Composable @ReadOnlyComposable get() = JarvisTheme.typography.titleMedium

    val iconSize: Dp
        @Composable @ReadOnlyComposable get() = JarvisTheme.dimens.iconSize.medium

    val spacing: Dp
        @Composable @ReadOnlyComposable get() = JarvisTheme.dimens.spacing.m

    // 잠긴 카드는 Card 가 LocalContentColor 를 흐린 색으로 바꿔 둔다. 강조색을 그대로 두면 아이콘만
    // 눌릴 수 있는 것처럼 보인다.
    @Composable
    @ReadOnlyComposable
    fun iconColor(enabled: Boolean): Color =
        if (enabled) JarvisTheme.colorScheme.primary else LocalContentColor.current
}
