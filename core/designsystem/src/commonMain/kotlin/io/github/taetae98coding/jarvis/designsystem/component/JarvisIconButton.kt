package io.github.taetae98coding.jarvis.designsystem.component

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonColors
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import io.github.taetae98coding.jarvis.designsystem.theme.JarvisTheme

/** 아이콘만 있는 버튼은 읽어 줄 글자가 없어서 [contentDescription] 을 비울 수 없다. */
@Composable
fun JarvisIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: IconButtonColors = JarvisIconButtonDefaults.colors(),
) {
    IconButton(onClick = onClick, modifier = modifier, enabled = enabled, colors = colors) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(JarvisIconButtonDefaults.iconSize),
        )
    }
}

object JarvisIconButtonDefaults {
    val iconSize: Dp
        @Composable @ReadOnlyComposable get() = JarvisTheme.dimens.iconSize.medium

    @Composable
    fun colors(contentColor: Color = LocalContentColor.current): IconButtonColors =
        IconButtonDefaults.iconButtonColors(contentColor = contentColor)
}
