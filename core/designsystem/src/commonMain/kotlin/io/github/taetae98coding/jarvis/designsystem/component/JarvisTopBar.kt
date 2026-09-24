package io.github.taetae98coding.jarvis.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.style.Style
import androidx.compose.foundation.style.styleable
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import io.github.taetae98coding.jarvis.designsystem.icon.JarvisIcons
import io.github.taetae98coding.jarvis.designsystem.theme.JarvisTheme
import io.github.taetae98coding.jarvis.designsystem.theme.jarvisDimens

// Material3 의 TopAppBar 는 실험 API 라 버전을 올릴 때마다 시그니처가 흔들린다. 뒤로 버튼, 제목, 동작
// 버튼만 필요해서 직접 놓는다.
@Composable
fun JarvisTopBar(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    style: Style = Style,
    titleStyle: TextStyle = JarvisTopBarDefaults.titleStyle,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .styleable(null, JarvisTopBarDefaults.style, style),
        horizontalArrangement = Arrangement.spacedBy(JarvisTopBarDefaults.spacing),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        JarvisIconButton(
            icon = JarvisIcons.Back,
            contentDescription = JarvisTopBarDefaults.BackContentDescription,
            onClick = onBack,
        )

        Text(
            text = title,
            style = titleStyle,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )

        actions()
    }
}

object JarvisTopBarDefaults {
    const val BackContentDescription = "뒤로"

    val titleStyle: TextStyle
        @Composable @ReadOnlyComposable get() = JarvisTheme.typography.titleLarge

    val spacing: Dp
        @Composable @ReadOnlyComposable get() = JarvisTheme.dimens.spacing.xs

    val style: Style = Style {
        minHeight(jarvisDimens.layout.topBarHeight)
    }
}
