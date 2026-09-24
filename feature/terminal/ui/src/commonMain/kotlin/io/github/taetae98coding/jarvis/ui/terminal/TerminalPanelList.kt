package io.github.taetae98coding.jarvis.ui.terminal

import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.style.Style
import androidx.compose.foundation.style.animate
import androidx.compose.foundation.style.hovered
import androidx.compose.foundation.style.pressed
import androidx.compose.foundation.style.rememberUpdatedStyleState
import androidx.compose.foundation.style.selected
import androidx.compose.foundation.style.styleable
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.selectAll
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.taetae98coding.jarvis.designsystem.icon.JarvisIcons
import io.github.taetae98coding.jarvis.designsystem.theme.JarvisTheme
import io.github.taetae98coding.jarvis.designsystem.theme.jarvisColorScheme
import io.github.taetae98coding.jarvis.designsystem.theme.jarvisShapes
import io.github.taetae98coding.jarvis.domain.terminal.TerminalPanel
import io.github.taetae98coding.jarvis.domain.terminal.TerminalProgram

const val TerminalNewPanelTestTag = "terminal:new-panel"
const val TerminalPanelNameFieldTestTag = "terminal:panel-name-field"

fun terminalPanelTestTag(id: Long): String = "terminal:panel:$id"

fun terminalPanelRenameTestTag(id: Long): String = "terminal:panel-rename:$id"

fun terminalPanelCloseTestTag(id: Long): String = "terminal:panel-close:$id"

/** 왼쪽의 패널 목록. 패널이 하나뿐이면 닫을 수 없다. "새 패널" 은 [NewPanelDialog] 를 거쳐 [onAdd] 를 부른다. */
@Composable
internal fun TerminalPanelList(
    panels: List<TerminalPanel>,
    selectedPanelId: Long?,
    nextPanelName: String,
    canOpenClaude: Boolean,
    onSelect: (Long) -> Unit,
    onRename: (Long, String) -> Unit,
    onClose: (Long) -> Unit,
    onAdd: (name: String, directory: String, program: TerminalProgram) -> Unit,
    modifier: Modifier = Modifier,
) {
    var creating by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.width(TerminalPanelListDefaults.width).fillMaxHeight(),
        verticalArrangement = Arrangement.spacedBy(JarvisTheme.dimens.spacing.xs),
    ) {
        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(JarvisTheme.dimens.spacing.xxs),
        ) {
            panels.forEach { panel ->
                TerminalPanelItem(
                    name = panel.name,
                    directory = panel.directory,
                    selected = panel.id == selectedPanelId,
                    closable = panels.size > 1,
                    onSelect = { onSelect(panel.id) },
                    onRename = { onRename(panel.id, it) },
                    onClose = { onClose(panel.id) },
                    modifier = Modifier.fillMaxWidth().testTag(terminalPanelTestTag(panel.id)),
                    renameModifier = Modifier.testTag(terminalPanelRenameTestTag(panel.id)),
                    closeModifier = Modifier.testTag(terminalPanelCloseTestTag(panel.id)),
                )
            }
        }

        TextButton(onClick = { creating = true }, modifier = Modifier.fillMaxWidth().testTag(TerminalNewPanelTestTag)) {
            Icon(
                imageVector = JarvisIcons.Add,
                contentDescription = null,
                modifier = Modifier.size(JarvisTheme.dimens.iconSize.small),
            )
            Text(text = "새 패널", modifier = Modifier.padding(start = JarvisTheme.dimens.spacing.s))
        }
    }

    if (creating) {
        NewPanelDialog(
            defaultName = nextPanelName,
            canOpenClaude = canOpenClaude,
            onCreate = { name, directory, program ->
                creating = false
                onAdd(name, directory, program)
            },
            onDismiss = { creating = false },
        )
    }
}

/**
 * 패널 한 줄. 이름을 바꾸는 동안은 이름 자리에 입력 필드가 온다. 편집 중인지는 이 줄만 아는 값이라
 * ViewModel 에 두지 않는다.
 */
@Composable
internal fun TerminalPanelItem(
    name: String,
    directory: String?,
    selected: Boolean,
    closable: Boolean,
    onSelect: () -> Unit,
    onRename: (String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    renameModifier: Modifier = Modifier,
    closeModifier: Modifier = Modifier,
    style: Style = Style,
) {
    var editing by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    val styleState = rememberUpdatedStyleState(interactionSource) { it.isSelected = selected }
    val contentColor = TerminalPanelItemDefaults.contentColor(selected)
    val spacing = JarvisTheme.dimens.spacing

    Row(
        modifier = modifier
            .hoverable(interactionSource)
            .styleable(styleState, TerminalPanelItemDefaults.style, style),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val nameModifier = Modifier
            .weight(1f)
            .padding(start = spacing.m, end = spacing.xs, top = spacing.s, bottom = spacing.s)

        if (editing) {
            PanelNameField(
                initial = name,
                color = contentColor,
                onDone = { value ->
                    editing = false
                    onRename(value)
                },
                onCancel = { editing = false },
                modifier = nameModifier,
            )
        } else {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(JarvisTheme.shapes.small)
                    // 눌림은 Style 의 배경이 보여 준다. 물결까지 그리면 같은 표시가 두 번 겹친다.
                    .clickable(interactionSource = interactionSource, indication = null, onClick = onSelect)
                    .padding(start = spacing.m, end = spacing.xs, top = spacing.s, bottom = spacing.s),
            ) {
                Text(
                    text = name,
                    style = TerminalPanelItemDefaults.nameStyle,
                    color = contentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (directory != null) {
                    Text(
                        text = directory,
                        style = TerminalPanelItemDefaults.directoryStyle,
                        color = TerminalPanelItemDefaults.directoryColor(selected),
                        maxLines = 1,
                        // 경로는 끝의 폴더 이름이 알아보는 데 중요하다.
                        overflow = TextOverflow.StartEllipsis,
                    )
                }
            }

            PanelItemIcon(JarvisIcons.Edit, "이름 바꾸기", contentColor, onClick = { editing = true }, modifier = renameModifier)
            if (closable) {
                PanelItemIcon(JarvisIcons.Close, "패널 닫기", contentColor, onClick = onClose, modifier = closeModifier)
            }
        }
    }
}

@Composable
private fun PanelItemIcon(
    icon: ImageVector,
    contentDescription: String,
    tint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(JarvisTheme.shapes.small)
            .clickable(onClick = onClick)
            .padding(JarvisTheme.dimens.spacing.s),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(JarvisTheme.dimens.iconSize.small),
            tint = tint,
        )
    }
}

/** Enter·포커스를 잃으면 확정, Esc 는 취소. 들어올 때 이름 전체가 선택돼 곧바로 덮어쓸 수 있다. */
@Composable
private fun PanelNameField(
    initial: String,
    color: Color,
    onDone: (String) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state = rememberTextFieldState(initial)
    val focusRequester = remember { FocusRequester() }
    // 포커스를 받기 전의 "포커스 없음" 알림을 확정으로 읽지 않게, 한 번 포커스를 받은 뒤부터 본다.
    var focused by remember { mutableStateOf(false) }
    var finished by remember { mutableStateOf(false) }

    fun finish(commit: Boolean) {
        if (finished) return
        finished = true
        if (commit) onDone(state.text.toString()) else onCancel()
    }

    LaunchedEffect(Unit) {
        state.edit { selectAll() }
        focusRequester.requestFocus()
    }

    BasicTextField(
        state = state,
        lineLimits = TextFieldLineLimits.SingleLine,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        textStyle = TerminalPanelItemDefaults.nameStyle.copy(color = color),
        cursorBrush = SolidColor(color),
        onKeyboardAction = { finish(commit = true) },
        modifier = modifier
            .testTag(TerminalPanelNameFieldTestTag)
            .focusRequester(focusRequester)
            .onFocusChanged {
                if (it.isFocused) focused = true else if (focused) finish(commit = true)
            }
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when (event.key) {
                    Key.Enter, Key.NumPadEnter -> finish(commit = true)
                    Key.Escape -> finish(commit = false)
                    else -> return@onPreviewKeyEvent false
                }
                true
            },
    )
}

internal object TerminalPanelListDefaults {
    val width: Dp = 200.dp
}

internal object TerminalPanelItemDefaults {
    val nameStyle: TextStyle
        @Composable @ReadOnlyComposable get() = JarvisTheme.typography.labelLarge

    val directoryStyle: TextStyle
        @Composable @ReadOnlyComposable get() = JarvisTheme.typography.labelSmall

    @Composable
    @ReadOnlyComposable
    fun contentColor(selected: Boolean): Color =
        if (selected) JarvisTheme.colorScheme.onPrimaryContainer else JarvisTheme.colorScheme.onSurface

    @Composable
    @ReadOnlyComposable
    fun directoryColor(selected: Boolean): Color =
        if (selected) JarvisTheme.colorScheme.onPrimaryContainer else JarvisTheme.colorScheme.onSurfaceVariant

    // 겹치는 투명도는 M3 상태 레이어 값(hover 8%, pressed 10%)이다. 선택되지 않은 줄은 바탕에 묻힌다.
    val style: Style = Style {
        val scheme = jarvisColorScheme

        shape(jarvisShapes.small)
        background(Color.Transparent)
        hovered { animate { background(scheme.onSurface.copy(alpha = HoverAlpha)) } }
        pressed { animate { background(scheme.onSurface.copy(alpha = PressedAlpha)) } }

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
