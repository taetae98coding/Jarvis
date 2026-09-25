package io.github.taetae98coding.jarvis.ui.terminal

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitLongPressOrCancellation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.gestures.rememberScrollableState
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.style.Style
import androidx.compose.foundation.style.animate
import androidx.compose.foundation.style.border
import androidx.compose.foundation.style.hovered
import androidx.compose.foundation.style.rememberUpdatedStyleState
import androidx.compose.foundation.style.selected
import androidx.compose.foundation.style.styleable
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.taetae98coding.jarvis.designsystem.icon.JarvisIcons
import io.github.taetae98coding.jarvis.designsystem.theme.JarvisTheme
import io.github.taetae98coding.jarvis.designsystem.theme.jarvisColorScheme
import io.github.taetae98coding.jarvis.designsystem.theme.jarvisColors
import io.github.taetae98coding.jarvis.designsystem.theme.jarvisDimens
import io.github.taetae98coding.jarvis.domain.terminal.TerminalCell
import io.github.taetae98coding.jarvis.domain.terminal.TerminalEmulator
import io.github.taetae98coding.jarvis.domain.terminal.TerminalKey
import io.github.taetae98coding.jarvis.domain.terminal.TerminalKeyModifiers
import io.github.taetae98coding.jarvis.domain.terminal.TerminalLine
import io.github.taetae98coding.jarvis.domain.terminal.TerminalLink
import io.github.taetae98coding.jarvis.domain.terminal.TerminalSelection
import io.github.taetae98coding.jarvis.domain.terminal.TerminalSelectionBounds
import io.github.taetae98coding.jarvis.domain.terminal.TerminalStyle
import io.github.taetae98coding.jarvis.domain.terminal.encodeControlCharacter
import io.github.taetae98coding.jarvis.domain.terminal.encodeTerminalKey
import io.github.taetae98coding.jarvis.domain.terminal.linkAt
import io.github.taetae98coding.jarvis.domain.terminal.selectedText
import io.github.taetae98coding.jarvis.domain.terminal.selectionBounds
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop

fun terminalPaneTestTag(id: Long): String = "terminal:pane:$id"

const val TerminalLinkMenuUrlTestTag = "terminal:link-menu:url"
const val TerminalLinkMenuSystemTestTag = "terminal:link-menu:system"
const val TerminalLinkMenuJarvisTestTag = "terminal:link-menu:jarvis"

/**
 * [onOpenInJarvis] 가 null 이면 이 타깃에서 브라우저 탭을 띄울 수 없는 것이라, 링크 메뉴에 Jarvis 항목이 없고 누르면
 * 곧바로 시스템 브라우저로 연다(docs/common/terminal-link.html R7).
 */
@Composable
internal fun TerminalPane(
    state: TerminalPaneState,
    focused: Boolean,
    showFocusBorder: Boolean,
    onFocus: () -> Unit,
    modifier: Modifier = Modifier,
    onOpenInJarvis: ((String) -> Unit)? = null,
) {
    val revision by state.revision.collectAsStateWithLifecycle()
    val scrollOffset by state.scrollOffset.collectAsStateWithLifecycle()

    val colors = TerminalPaneDefaults.colors()
    val textStyle = TerminalPaneDefaults.textStyle(colors)

    // 한 프레임에 줄마다 여러 조각을 잰다. 기본 캐시(8개)로는 매 프레임 거의 전부 다시 잰다.
    val textMeasurer = rememberTextMeasurer(cacheSize = 512)
    val density = LocalDensity.current
    val cell = remember(textMeasurer, density, textStyle) { textMeasurer.measure("W", textStyle).size }
    val styleState = rememberUpdatedStyleState(null) { it.isSelected = showFocusBorder && focused }

    val focusRequester = remember { FocusRequester() }
    val currentOnFocus by rememberUpdatedState(onFocus)
    val field = rememberTextFieldState()

    // 마우스 위치와 누른 링크의 메뉴. 화면 안에서만 쓰는 값이라 ViewModel 이 아니라 여기 둔다.
    var pointer by remember { mutableStateOf<Offset?>(null) }
    var linkMenu by remember { mutableStateOf<LinkMenu?>(null) }
    val uriHandler = LocalUriHandler.current
    val currentOnOpenInJarvis by rememberUpdatedState(onOpenInJarvis)

    // 끌어 선택한 범위. 화면 안에서만 쓰는 값이라 여기 둔다(docs/common/terminal-selection.html).
    var selection by remember { mutableStateOf<TerminalSelection?>(null) }
    // Compose Multiplatform 1.12.1 의 LocalClipboard 는 공통 코드에서 텍스트 ClipEntry 를 만들 수 없다(데스크톱은
    // Transferable, Android 는 ClipData 생성자뿐). ClipEntry 에 공통 팩토리가 생기면 그리로 옮긴다.
    @Suppress("DEPRECATION")
    val clipboard by rememberUpdatedState(LocalClipboardManager.current)

    // 화면 좌표 → 칸 → 링크. 보이는 줄 번호에서 스크롤한 만큼 빼면 에뮬레이터의 줄 번호다.
    fun linkAt(offset: Offset): TerminalLink? {
        val row = (offset.y / cell.height).toInt() - state.scrollOffset.value
        val column = (offset.x / cell.width).toInt()
        return state.emulator.linkAt(row, column)
    }

    // 화면 좌표 → 칸. 끌기는 창 밖 좌표도 오므로 가장자리 칸으로 묶는다.
    fun cellAt(offset: Offset): TerminalCell {
        val emulator = state.emulator
        val column = (offset.x / cell.width).toInt().coerceIn(0, emulator.columns - 1)
        val row = (offset.y / cell.height).toInt().coerceIn(0, emulator.rows - 1) - state.scrollOffset.value

        return TerminalCell(row, column)
    }

    // 마우스가 그대로여도 출력이 오거나 스크롤하면 그 자리의 글자가 바뀐다. 그때마다 다시 찾는다.
    val hoveredLink = remember(pointer, revision, scrollOffset, cell) { pointer?.let(::linkAt) }

    fun openInSystemBrowser(url: String) {
        // 기본 브라우저가 없거나 주소가 잘못돼 던져도 셸은 그대로여야 한다.
        runCatching { uriHandler.openUri(url) }
    }

    // 확정된 글자만 셸로 보내고 필드를 비운다. 필드 상태가 곧 IME 버퍼라, 여기서 비우면 IME 쪽도 함께 비워진다.
    // (값을 받는 TextFieldValue API 는 빈 값을 다시 넘겨도 버퍼를 비우지 않아 다음 입력에 앞 글자가 누적됐다.)
    LaunchedEffect(field, state) {
        snapshotFlow { field.text.toString() to field.composition }
            .collect { (text, composition) ->
                if (composition != null || text.isEmpty()) return@collect

                field.clearText()
                state.paste(text)
            }
    }

    LaunchedEffect(focused) {
        if (focused) focusRequester.requestFocus()
    }

    // 대체 화면을 오가면 같은 줄 번호가 다른 화면의 글자를 가리킨다. 그때는 선택을 지운다.
    LaunchedEffect(state) {
        snapshotFlow { revision; state.emulator.isAlternateScreen }
            .distinctUntilChanged()
            .drop(1)
            .collect { selection = null }
    }

    Box(
        modifier = modifier
            .styleable(styleState, TerminalPaneDefaults.style)
            .onSizeChanged { size ->
                state.resize(
                    columns = (size.width / cell.width).coerceAtLeast(1),
                    rows = (size.height / cell.height).coerceAtLeast(1),
                )
            }
            .pointerInput(cell) {
                detectTapGestures { offset ->
                    selection = null
                    val link = linkAt(offset)
                    when {
                        link == null -> {
                            currentOnFocus()
                            focusRequester.requestFocus()
                        }

                        currentOnOpenInJarvis == null -> openInSystemBrowser(link.url)
                        else -> linkMenu = LinkMenu(link, offset)
                    }
                }
            }
            .pointerInput(cell) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        when (event.type) {
                            PointerEventType.Move, PointerEventType.Enter -> pointer = event.changes.firstOrNull()?.position
                            PointerEventType.Exit -> pointer = null
                        }
                    }
                }
            }
            .pointerHoverIcon(if (hoveredLink != null) PointerIcon.Hand else PointerIcon.Default)
            .scrollable(
                orientation = Orientation.Vertical,
                state = rememberScrollableState { delta ->
                    // 터치 드래그는 호버가 없어 위치를 모른다. 그때는 커서 칸이다.
                    val at = pointer
                    state.scrollBy(
                        pixels = delta,
                        lineHeight = cell.height.toFloat(),
                        column = at?.let { (it.x / cell.width).toInt() } ?: state.emulator.cursorColumn,
                        row = at?.let { (it.y / cell.height).toInt() } ?: state.emulator.cursorRow,
                    )
                    delta
                },
            )
            // 다른 pointerInput·scrollable 보다 뒤(안쪽)에 있어야 Main 패스에서 먼저 보고 이동을 소비한다. 그래야
            // detectTapGestures 가 누르기를 취소하고 scrollable 이 터치 끌기를 시작하지 않는다.
            .pointerInput(cell) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val start = when (down.type) {
                        PointerType.Mouse -> awaitMouseDragStart(down)
                        else -> awaitLongPressOrCancellation(down.id)?.also { it.consume() }
                    } ?: return@awaitEachGesture

                    selection = TerminalSelection(cellAt(down.position), cellAt(start.position), state.emulator.scrolledLines)
                    drag(start.id) { change ->
                        change.consume()
                        selection = selection?.copy(focus = cellAt(change.position))
                    }

                    val text = selection?.let { state.emulator.selectedText(it) }.orEmpty()
                    if (text.isEmpty()) {
                        selection = null
                    } else {
                        // 다른 앱이 클립보드를 잡고 있으면 AWT 가 던진다. 셸은 그대로여야 한다.
                        runCatching { clipboard.setText(AnnotatedString(text)) }
                    }
                }
            }
            .testTag(terminalPaneTestTag(state.id)),
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            // 에뮬레이터는 Compose 상태가 아니다. 이 값을 읽어 두어야 출력이 올 때 다시 그린다.
            revision

            drawTerminal(state.emulator, scrollOffset, TerminalCanvas(textMeasurer, textStyle, colors, cell), focused)
            selection?.let { state.emulator.selectionBounds(it) }?.let {
                drawSelection(it, scrollOffset, state.emulator, cell, colors.selection)
            }
            hoveredLink?.let { drawLinkUnderline(it, scrollOffset, state.emulator.rows, cell, colors.foreground) }
        }

        linkMenu?.let { menu ->
            TerminalLinkMenu(
                menu = menu,
                onDismiss = { linkMenu = null },
                onOpenInSystem = { openInSystemBrowser(menu.link.url) },
                onOpenInJarvis = currentOnOpenInJarvis?.let { open -> { open(menu.link.url) } },
            )
        }

        // IME 조합(한글)은 텍스트 필드만 받는다. 커서 자리에 겹쳐 두어 조합 중인 글자가 그 자리에 보이게
        // 하고, 확정된 글자만 셸로 보낸 뒤 비운다.
        BasicTextField(
            state = field,
            modifier = Modifier
                .offset {
                    revision
                    IntOffset(state.emulator.cursorColumn * cell.width, state.emulator.cursorRow * cell.height)
                }
                .widthIn(min = TerminalPaneDefaults.inputMinWidth)
                .focusRequester(focusRequester)
                .onFocusChanged { if (it.isFocused) onFocus() }
                .onPreviewKeyEvent { event -> onKey(event, composing = field.composition != null, state) },
            textStyle = textStyle.copy(background = colors.background),
            cursorBrush = SolidColor(Color.Transparent),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.None,
                autoCorrectEnabled = false,
            ),
        )
    }
}

/** 누른 링크와 누른 자리(창 안 픽셀). 메뉴는 그 자리에 뜬다(R4). */
private class LinkMenu(val link: TerminalLink, val position: Offset)

@Composable
private fun TerminalLinkMenu(
    menu: LinkMenu,
    onDismiss: () -> Unit,
    onOpenInSystem: () -> Unit,
    onOpenInJarvis: (() -> Unit)?,
) {
    val density = LocalDensity.current
    val offset = with(density) { DpOffset(menu.position.x.toDp(), menu.position.y.toDp()) }

    DropdownMenu(expanded = true, onDismissRequest = onDismiss, offset = offset) {
        // OSC 8 은 보이는 글자와 여는 주소가 다를 수 있다. 무엇을 열지 누르기 전에 보인다.
        Text(
            text = menu.link.url,
            modifier = Modifier
                .widthIn(max = TerminalPaneDefaults.linkMenuMaxWidth)
                .padding(horizontal = JarvisTheme.dimens.spacing.m, vertical = JarvisTheme.dimens.spacing.xs)
                .testTag(TerminalLinkMenuUrlTestTag),
            color = JarvisTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.MiddleEllipsis,
            style = JarvisTheme.typography.labelMedium,
        )
        DropdownMenuItem(
            text = { Text("시스템 브라우저에서 열기") },
            leadingIcon = { Icon(imageVector = JarvisIcons.OpenInNew, contentDescription = null) },
            onClick = {
                onDismiss()
                onOpenInSystem()
            },
            modifier = Modifier.testTag(TerminalLinkMenuSystemTestTag),
        )
        if (onOpenInJarvis != null) {
            DropdownMenuItem(
                text = { Text("Jarvis 브라우저에서 열기") },
                leadingIcon = { Icon(imageVector = JarvisIcons.Globe, contentDescription = null) },
                onClick = {
                    onDismiss()
                    onOpenInJarvis()
                },
                modifier = Modifier.testTag(TerminalLinkMenuJarvisTestTag),
            )
        }
    }
}

private fun onKey(event: KeyEvent, composing: Boolean, state: TerminalPaneState): Boolean {
    if (event.type != KeyEventType.KeyDown) return false
    if (event.isMetaPressed) {
        // 넘기지 않으면 입력 필드가 비어 있는 자기 안에서 커서를 옮기고 끝난다.
        val bytes = CommandKeys[event.key]
        if (bytes == null || composing || event.isShiftPressed || event.isAltPressed || event.isCtrlPressed) {
            // 나머지 ⌘ 조합은 화면의 분할·탭 단축키와 붙여넣기에 남긴다.
            return false
        }

        state.input(bytes)
        return true
    }

    val modifiers = TerminalKeyModifiers(shift = event.isShiftPressed, alt = event.isAltPressed, ctrl = event.isCtrlPressed)

    val key = SpecialKeys[event.key]
    if (key != null) {
        // 조합 중에는 Backspace·Enter·방향키가 IME 의 몫이다. 필드에 남아 있는 확정 글자는 이미 셸로 갔으므로
        // 그때의 Backspace 는 셸로 보낸다.
        if (composing && key in ImeKeys) return false

        state.input(encodeTerminalKey(key, modifiers, state.emulator.applicationCursorKeys))
        return true
    }

    if (event.isCtrlPressed && !composing) {
        val char = ControlKeys[event.key] ?: return false
        val bytes = encodeControlCharacter(char, alt = event.isAltPressed) ?: return false

        state.input(bytes)
        return true
    }

    return false
}

private val SpecialKeys: Map<Key, TerminalKey> = mapOf(
    Key.Enter to TerminalKey.Enter,
    Key.NumPadEnter to TerminalKey.Enter,
    Key.Backspace to TerminalKey.Backspace,
    // 넘기지 않으면 Compose 가 Tab 으로 포커스를 다음 컴포저블로 옮긴다.
    Key.Tab to TerminalKey.Tab,
    Key.Escape to TerminalKey.Escape,
    Key.DirectionUp to TerminalKey.Up,
    Key.DirectionDown to TerminalKey.Down,
    Key.DirectionLeft to TerminalKey.Left,
    Key.DirectionRight to TerminalKey.Right,
    Key.MoveHome to TerminalKey.Home,
    Key.MoveEnd to TerminalKey.End,
    Key.PageUp to TerminalKey.PageUp,
    Key.PageDown to TerminalKey.PageDown,
    Key.Insert to TerminalKey.Insert,
    Key.Delete to TerminalKey.Delete,
    Key.F1 to TerminalKey.F1,
    Key.F2 to TerminalKey.F2,
    Key.F3 to TerminalKey.F3,
    Key.F4 to TerminalKey.F4,
    Key.F5 to TerminalKey.F5,
    Key.F6 to TerminalKey.F6,
    Key.F7 to TerminalKey.F7,
    Key.F8 to TerminalKey.F8,
    Key.F9 to TerminalKey.F9,
    Key.F10 to TerminalKey.F10,
    Key.F11 to TerminalKey.F11,
    Key.F12 to TerminalKey.F12,
)

// Home/End 바이트가 아니라 Ctrl+A/Ctrl+E 다. macOS 기본 zsh 는 일반 모드의 Home/End 를 묶지 않는다(스펙의 결정).
// Ctrl+U 는 zsh 에서는 커서 앞이 아니라 줄 전체를 지운다.
private val CommandKeys: Map<Key, ByteArray> = mapOf(
    Key.DirectionLeft to byteArrayOf(0x01),
    Key.DirectionRight to byteArrayOf(0x05),
    Key.Backspace to byteArrayOf(0x15),
)

private val ImeKeys = setOf(
    TerminalKey.Enter,
    TerminalKey.Backspace,
    TerminalKey.Left,
    TerminalKey.Right,
    TerminalKey.Up,
    TerminalKey.Down,
)

// Ctrl 을 누르면 플랫폼마다 글자 값이 제각각이라(AWT 는 이미 제어 문자, Android 는 원래 글자) 키
// 위치로 고른다.
private val ControlKeys: Map<Key, Char> = buildMap {
    listOf(
        Key.A, Key.B, Key.C, Key.D, Key.E, Key.F, Key.G, Key.H, Key.I, Key.J, Key.K, Key.L, Key.M,
        Key.N, Key.O, Key.P, Key.Q, Key.R, Key.S, Key.T, Key.U, Key.V, Key.W, Key.X, Key.Y, Key.Z,
    ).forEachIndexed { index, key -> put(key, 'a' + index) }

    put(Key.Spacebar, ' ')
    put(Key.LeftBracket, '[')
    put(Key.Backslash, '\\')
    put(Key.RightBracket, ']')
    put(Key.Slash, '/')
}

@Immutable
private class TerminalCanvas(
    val textMeasurer: TextMeasurer,
    val textStyle: TextStyle,
    val colors: TerminalPaneColors,
    cell: IntSize,
) {
    val cellWidth = cell.width.toFloat()
    val cellHeight = cell.height.toFloat()
}

private fun DrawScope.drawTerminal(
    emulator: TerminalEmulator,
    scrollOffset: Int,
    canvas: TerminalCanvas,
    focused: Boolean,
) {
    val cellWidth = canvas.cellWidth
    val cellHeight = canvas.cellHeight

    for (row in 0 until emulator.rows) {
        val index = row - scrollOffset
        if (index < -emulator.scrollbackSize) continue

        drawLine(emulator.line(index), emulator.columns, row * cellHeight, canvas)
    }

    if (scrollOffset != 0 || !emulator.cursorVisible) return

    val line = emulator.line(emulator.cursorRow)
    val column = emulator.cursorColumn.coerceAtMost(line.columns - 1)
    val width = if (line.isWide(column)) cellWidth * 2 else cellWidth
    val topLeft = Offset(column * cellWidth, emulator.cursorRow * cellHeight)

    val cursor = canvas.colors.foreground.copy(alpha = TerminalPaneDefaults.DimAlpha)

    if (focused) {
        drawRect(cursor, topLeft, Size(width, cellHeight))
    } else {
        drawRect(cursor, topLeft, Size(width, cellHeight), style = Stroke(width = 1f))
    }
}

/** 마우스가 올라간 링크의 칸마다 아래쪽에 선을 긋는다(R3). 화면 밖(스크롤된) 칸은 건너뛴다. */
private fun DrawScope.drawLinkUnderline(link: TerminalLink, scrollOffset: Int, rows: Int, cell: IntSize, color: Color) {
    for (terminalCell in link.cells) {
        val row = terminalCell.row + scrollOffset
        if (row !in 0 until rows) continue

        val y = (row + 1) * cell.height.toFloat() - 1f
        drawLine(
            color = color,
            start = Offset(terminalCell.column * cell.width.toFloat(), y),
            end = Offset((terminalCell.column + 1) * cell.width.toFloat(), y),
            strokeWidth = 1f,
        )
    }
}

/** 선택된 칸을 글자 위에 반투명으로 덮는다(R3). 줄마다 선택된 첫 칸부터 마지막 칸까지 한 사각형이다. */
private fun DrawScope.drawSelection(
    bounds: TerminalSelectionBounds,
    scrollOffset: Int,
    emulator: TerminalEmulator,
    cell: IntSize,
    color: Color,
) {
    for (row in 0 until emulator.rows) {
        val index = row - scrollOffset
        if (index < -emulator.scrollbackSize) continue

        val range = bounds.columnsAt(index, minOf(emulator.columns, emulator.line(index).columns)) ?: continue
        drawRect(
            color = color,
            topLeft = Offset(range.first * cell.width.toFloat(), row * cell.height.toFloat()),
            size = Size((range.last - range.first + 1) * cell.width.toFloat(), cell.height.toFloat()),
        )
    }
}

/**
 * 같은 스타일의 ASCII 칸은 한 조각으로 모아 그린다. 그 밖의 글자는 칸마다 따로 그린다 — 고정폭 글꼴에
 * 없는 글자는 대체 글꼴로 그려져 폭이 달라서, 한 조각에 섞으면 뒤따르는 칸이 전부 밀린다.
 */
private fun DrawScope.drawLine(
    line: TerminalLine,
    columns: Int,
    y: Float,
    canvas: TerminalCanvas,
) {
    val end = minOf(columns, line.columns)
    var column = 0

    while (column < end) {
        if (line.isWideTail(column)) {
            column++
            continue
        }

        val style = line.styleAt(column)
        val start = column

        if (line.codePointAt(column) in 0..0x7E) {
            val text = StringBuilder()
            while (column < end && line.codePointAt(column) in 0..0x7E && line.styleAt(column) == style) {
                text.append(line.textAt(column).ifEmpty { " " })
                column++
            }
            drawCells(text.toString(), start, column - start, style, y, canvas)
        } else {
            val width = if (line.isWide(column)) 2 else 1
            drawCells(line.textAt(column), start, width, style, y, canvas)
            column += width
        }
    }
}

private fun DrawScope.drawCells(
    text: String,
    column: Int,
    cells: Int,
    style: TerminalStyle,
    y: Float,
    canvas: TerminalCanvas,
) {
    val cellWidth = canvas.cellWidth
    val defaults = canvas.colors
    var foreground = style.foreground.resolve(defaults.foreground)
    var background = style.background.resolve(defaults.background)
    if (style.inverse) foreground = background.also { background = foreground }
    if (style.hidden) foreground = background
    if (style.dim) foreground = foreground.copy(alpha = TerminalPaneDefaults.DimAlpha)

    val topLeft = Offset(column * cellWidth, y)

    if (background != defaults.background) {
        drawRect(background, topLeft, Size(cells * cellWidth, canvas.cellHeight))
    }

    if (text.isBlank()) return

    val layout: TextLayoutResult = canvas.textMeasurer.measure(
        text = text,
        style = canvas.textStyle.copy(
            color = foreground,
            fontWeight = if (style.bold) FontWeight.Bold else FontWeight.Normal,
            fontStyle = if (style.italic) FontStyle.Italic else FontStyle.Normal,
            textDecoration = when {
                style.underline && style.strikethrough -> TextDecoration.combine(
                    listOf(TextDecoration.Underline, TextDecoration.LineThrough),
                )

                style.underline -> TextDecoration.Underline
                style.strikethrough -> TextDecoration.LineThrough
                else -> null
            },
        ),
        softWrap = false,
        maxLines = 1,
    )

    // 전각 글자는 고정폭 글꼴에 없어 대체 글꼴로 그려지고, 그 폭이 두 칸보다 좁다. 왼쪽에 붙이면 글자 사이가
    // 벌어져 보여서 두 칸 가운데에 놓는다.
    val slack = if (cells == 2) (cells * cellWidth - layout.size.width).coerceAtLeast(0f) / 2 else 0f

    drawText(layout, topLeft = topLeft + Offset(slack, 0f))
}

@Immutable
internal class TerminalPaneColors(
    val background: Color,
    val foreground: Color,
    val selection: Color,
)

internal object TerminalPaneDefaults {
    /** 흐린 글자(SGR 2)와 커서에 쓰는 글자색 투명도. */
    const val DimAlpha = 0.6f

    /** 끌어 선택한 칸을 글자 위에 덮는 강조색 투명도. 어떤 배경 위에서도 보이고 글자도 읽혀야 한다. */
    const val SelectionAlpha = 0.35f

    val inputMinWidth: Dp = 1.dp

    /** 링크 메뉴 위 주소 줄의 최대 너비. 긴 주소는 가운데를 줄인다. */
    val linkMenuMaxWidth: Dp = 360.dp

    @Composable
    @ReadOnlyComposable
    fun colors(
        background: Color = JarvisTheme.colors.terminalBackground,
        foreground: Color = JarvisTheme.colors.terminalForeground,
        selection: Color = JarvisTheme.colorScheme.primary.copy(alpha = SelectionAlpha),
    ): TerminalPaneColors = TerminalPaneColors(background = background, foreground = foreground, selection = selection)

    @Composable
    @ReadOnlyComposable
    fun textStyle(colors: TerminalPaneColors): TextStyle = JarvisTheme.codeTextStyle.copy(color = colors.foreground)

    // 포커스 테두리는 레이아웃 크기에 들어가지 않는다. 테두리가 생길 때 칸 수가 바뀌어 셸이 다시 그리는
    // 일이 없다.
    val style: Style = Style {
        background(jarvisColors.terminalBackground)
        selected { border(jarvisDimens.stroke.thin, jarvisColorScheme.primary) }
    }

    /** 분할 경계선. 끌 수 있다는 것을 마우스를 올렸을 때 강조색으로 알린다. */
    val dividerStyle: Style = Style {
        background(jarvisColorScheme.outlineVariant)
        hovered { animate { background(jarvisColorScheme.primary) } }
    }
}
