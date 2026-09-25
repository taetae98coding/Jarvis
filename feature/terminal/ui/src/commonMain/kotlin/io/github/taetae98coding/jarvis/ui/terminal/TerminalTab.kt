package io.github.taetae98coding.jarvis.ui.terminal

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.taetae98coding.jarvis.designsystem.icon.JarvisIcons
import io.github.taetae98coding.jarvis.designsystem.theme.JarvisTheme
import io.github.taetae98coding.jarvis.designsystem.theme.jarvisColorScheme
import io.github.taetae98coding.jarvis.designsystem.theme.jarvisShapes
import io.github.taetae98coding.jarvis.domain.terminal.TerminalTabKind

/**
 * 탭 하나. 배경의 선택·hover·눌림 변화는 [TerminalTabDefaults.style] 이 정하고 [style] 이 그 위에 덮는다.
 *
 * 제목을 누르는 상호작용과 탭 전체의 hover 를 같은 [MutableInteractionSource] 로 모아서 Style 이 두
 * 상태를 함께 본다. 닫기 버튼은 자기 물결만 그리고 탭 배경은 건드리지 않는다.
 *
 * [editing] 이면 제목 자리가 이름 입력칸이다. 제목을 두 번 누르면 [onStartRename] 이 불린다.
 */
@Composable
internal fun TerminalTab(
    title: String,
    kind: TerminalTabKind,
    selected: Boolean,
    editing: Boolean,
    onSelect: () -> Unit,
    onStartRename: () -> Unit,
    onRename: (String) -> Unit,
    onCancelRename: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    kindModifier: Modifier = Modifier,
    titleModifier: Modifier = Modifier,
    nameFieldModifier: Modifier = Modifier,
    closeModifier: Modifier = Modifier,
    style: Style = Style,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val styleState = rememberUpdatedStyleState(interactionSource) { it.isSelected = selected }
    val contentColor = TerminalTabDefaults.contentColor(selected)
    val spacing = JarvisTheme.dimens.spacing
    // 누른 시각은 포인터 이벤트의 uptime 으로 잰다. 벽시계로 재면 테스트의 가상 시계와 어긋난다.
    var lastPressUptime by remember { mutableStateOf<Long?>(null) }
    var doublePress by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .hoverable(interactionSource)
            .styleable(styleState, TerminalTabDefaults.style, style),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = TerminalTabDefaults.kindIcon(kind),
            contentDescription = TerminalTabDefaults.kindLabel(kind),
            modifier = kindModifier
                .padding(start = spacing.m)
                .size(JarvisTheme.dimens.iconSize.small),
            tint = TerminalTabDefaults.kindColor(kind) ?: contentColor,
        )

        val titlePadding = Modifier
            .padding(start = spacing.s, end = spacing.xs, top = spacing.s, bottom = spacing.s)
            .width(TerminalTabDefaults.titleWidth)

        if (editing) {
            TerminalNameField(
                initial = title,
                textStyle = TerminalTabDefaults.titleStyle,
                color = contentColor,
                onDone = onRename,
                onCancel = onCancelRename,
                modifier = nameFieldModifier.then(titlePadding),
            )
        } else {
            Text(
                text = title,
                style = TerminalTabDefaults.titleStyle,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = titleModifier
                    .clip(JarvisTheme.shapes.small)
                    // 눌림은 Style 의 배경이 보여 준다. 물결까지 그리면 같은 표시가 두 번 겹친다.
                    // combinedClickable(onDoubleClick) 은 두 번째 누름을 기다리느라 고르기를 미루므로, 누를 때마다 곧바로
                    // 고르고 앞 누름과의 간격을 직접 잰다(docs/common/terminal-tab-label.html#implementation).
                    // clickable 보다 먼저 보도록 Initial 단계에서 소비하지 않고 누름만 본다.
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                            val previous = lastPressUptime
                            doublePress = previous != null && down.uptimeMillis - previous <= viewConfiguration.doubleTapTimeoutMillis
                            lastPressUptime = if (doublePress) null else down.uptimeMillis
                        }
                    }
                    .clickable(interactionSource = interactionSource, indication = null) {
                        if (doublePress) onStartRename() else onSelect()
                    }
                    .then(titlePadding),
            )
        }

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

/** 탭 줄 끝의 새 탭 버튼. 탭과 같은 모양·높이라 "이 줄에 하나 더" 로 읽힌다. 메뉴는 부르는 쪽이 붙인다. */
@Composable
internal fun TerminalNewTabButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector = JarvisIcons.Add,
    contentDescription: String = "새 탭",
) {
    val interactionSource = remember { MutableInteractionSource() }
    val styleState = rememberUpdatedStyleState(interactionSource)

    Box(
        modifier = modifier
            .hoverable(interactionSource)
            .styleable(styleState, TerminalTabDefaults.style)
            // 눌림은 Style 의 배경이 보여 준다. 물결까지 그리면 같은 표시가 두 번 겹친다.
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            // 탭이 없는 빈 패널의 줄에서는 따라갈 탭이 없다. 그때 aspectRatio 가 intrinsic 높이를 아이콘 너비로
            // 정해 버튼이 아이콘 크기까지 줄어드므로, 탭 높이를 최소로 둔다.
            .heightIn(min = TerminalTabDefaults.height())
            .fillMaxHeight()
            .aspectRatio(1f, matchHeightConstraintsFirst = true),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(JarvisTheme.dimens.iconSize.small),
            tint = TerminalTabDefaults.contentColor(selected = false),
        )
    }
}

internal object TerminalTabDefaults {
    val titleWidth: Dp = 120.dp

    val titleStyle: TextStyle
        @Composable @ReadOnlyComposable get() = JarvisTheme.typography.labelLarge

    /** 제목 한 줄과 위아래 spacing.s. [TerminalTab] 의 제목 여백과 함께 고친다. */
    @Composable
    fun height(): Dp {
        val measurer = rememberTextMeasurer()
        val style = titleStyle
        val density = LocalDensity.current
        val line = remember(measurer, style, density) { with(density) { measurer.measure(" ", style).size.height.toDp() } }

        return maxOf(line, JarvisTheme.dimens.iconSize.small) + JarvisTheme.dimens.spacing.s * 2
    }

    fun kindIcon(kind: TerminalTabKind): ImageVector =
        when (kind) {
            TerminalTabKind.Terminal -> JarvisIcons.Terminal
            TerminalTabKind.Claude -> JarvisIcons.Claude
            TerminalTabKind.Browser -> JarvisIcons.Globe
            TerminalTabKind.Android -> JarvisIcons.Android
            TerminalTabKind.IOS -> JarvisIcons.Apple
            TerminalTabKind.Device -> JarvisIcons.Smartphone
            TerminalTabKind.File -> JarvisIcons.File
            TerminalTabKind.Run -> JarvisIcons.Play
        }

    fun kindLabel(kind: TerminalTabKind): String =
        when (kind) {
            TerminalTabKind.Terminal -> "터미널"
            TerminalTabKind.Claude -> "Claude"
            TerminalTabKind.Browser -> "브라우저"
            TerminalTabKind.Android -> "Android"
            TerminalTabKind.IOS -> "iOS"
            TerminalTabKind.Device -> "기기"
            TerminalTabKind.File -> "파일"
            TerminalTabKind.Run -> "실행"
        }

    // null 이면 탭 글자색을 따른다. 고정 색인 이유는 docs/common/terminal-tab-label.html#implementation.
    fun kindColor(kind: TerminalTabKind): Color? =
        when (kind) {
            TerminalTabKind.Terminal, TerminalTabKind.Device, TerminalTabKind.File, TerminalTabKind.Run -> null
            TerminalTabKind.Claude -> Color(0xFFD97757)
            TerminalTabKind.Browser -> Color(0xFF3B8EEA)
            TerminalTabKind.Android -> Color(0xFF34A853)
            TerminalTabKind.IOS -> Color(0xFF8E8E93)
        }

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
