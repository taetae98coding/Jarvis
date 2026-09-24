package io.github.taetae98coding.jarvis.ui.terminal

import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.style.Style
import androidx.compose.foundation.style.animate
import androidx.compose.foundation.style.hovered
import androidx.compose.foundation.style.pressed
import androidx.compose.foundation.style.rememberUpdatedStyleState
import androidx.compose.foundation.style.selected
import androidx.compose.foundation.style.styleable
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.taetae98coding.jarvis.designsystem.icon.JarvisIcons
import io.github.taetae98coding.jarvis.designsystem.theme.JarvisTheme
import io.github.taetae98coding.jarvis.designsystem.theme.jarvisColorScheme
import io.github.taetae98coding.jarvis.designsystem.theme.jarvisShapes

/**
 * 탭 하나. 배경의 선택·hover·눌림 변화는 [TerminalTabDefaults.style] 이 정하고 [style] 이 그 위에 덮는다.
 *
 * 제목을 누르는 상호작용과 탭 전체의 hover 를 같은 [MutableInteractionSource] 로 모아서 Style 이 두
 * 상태를 함께 본다. 닫기 버튼은 자기 물결만 그리고 탭 배경은 건드리지 않는다.
 */
@Composable
internal fun TerminalTab(
    title: String,
    selected: Boolean,
    onSelect: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    closeModifier: Modifier = Modifier,
    style: Style = Style,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val styleState = rememberUpdatedStyleState(interactionSource) { it.isSelected = selected }
    val contentColor = TerminalTabDefaults.contentColor(selected)
    val spacing = JarvisTheme.dimens.spacing

    Row(
        modifier = modifier
            .hoverable(interactionSource)
            .styleable(styleState, TerminalTabDefaults.style, style),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = TerminalTabDefaults.titleStyle,
            color = contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .clip(JarvisTheme.shapes.small)
                // 눌림은 Style 의 배경이 보여 준다. 물결까지 그리면 같은 표시가 두 번 겹친다.
                .clickable(interactionSource = interactionSource, indication = null, onClick = onSelect)
                .padding(start = spacing.m, end = spacing.xs, top = spacing.s, bottom = spacing.s)
                .width(TerminalTabDefaults.titleWidth),
        )

        Box(
            modifier = closeModifier
                .clip(JarvisTheme.shapes.small)
                .clickable(onClick = onClose)
                .padding(spacing.s),
        ) {
            Icon(
                imageVector = JarvisIcons.Close,
                contentDescription = "탭 닫기",
                modifier = Modifier.size(JarvisTheme.dimens.iconSize.small),
                tint = contentColor,
            )
        }
    }
}

internal object TerminalTabDefaults {
    val titleWidth: Dp = 120.dp

    val titleStyle: TextStyle
        @Composable @ReadOnlyComposable get() = JarvisTheme.typography.labelLarge

    @Composable
    @ReadOnlyComposable
    fun contentColor(selected: Boolean): Color =
        if (selected) JarvisTheme.colorScheme.onPrimaryContainer else JarvisTheme.colorScheme.onSurfaceVariant

    // 겹치는 투명도는 M3 상태 레이어 값(hover 8%, pressed 10%)이다.
    val style: Style = Style {
        val scheme = jarvisColorScheme

        shape(jarvisShapes.small)
        background(scheme.surfaceVariant)
        hovered { animate { background(scheme.onSurfaceVariant.layer(HoverAlpha, scheme.surfaceVariant)) } }
        pressed { animate { background(scheme.onSurfaceVariant.layer(PressedAlpha, scheme.surfaceVariant)) } }

        selected {
            animate { background(scheme.primaryContainer) }
            hovered { animate { background(scheme.onPrimaryContainer.layer(HoverAlpha, scheme.primaryContainer)) } }
            pressed { animate { background(scheme.onPrimaryContainer.layer(PressedAlpha, scheme.primaryContainer)) } }
        }
    }

    private const val HoverAlpha = 0.08f
    private const val PressedAlpha = 0.10f

    private fun Color.layer(alpha: Float, over: Color): Color = copy(alpha = alpha).compositeOver(over)
}
