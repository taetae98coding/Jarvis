package io.github.taetae98coding.jarvis.ui.terminal

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberScrollableState
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.taetae98coding.jarvis.domain.terminal.TerminalEmulator
import io.github.taetae98coding.jarvis.domain.terminal.TerminalKey
import io.github.taetae98coding.jarvis.domain.terminal.TerminalKeyModifiers
import io.github.taetae98coding.jarvis.domain.terminal.TerminalLine
import io.github.taetae98coding.jarvis.domain.terminal.TerminalStyle
import io.github.taetae98coding.jarvis.domain.terminal.encodeControlCharacter
import io.github.taetae98coding.jarvis.domain.terminal.encodeTerminalKey

fun terminalPaneTestTag(id: Long): String = "terminal:pane:$id"

private val PaneTextStyle = TextStyle(
    fontFamily = FontFamily.Monospace,
    fontSize = 13.sp,
    color = TerminalForeground,
)

@Composable
internal fun TerminalPane(
    state: TerminalPaneState,
    focused: Boolean,
    showFocusBorder: Boolean,
    onFocus: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val revision by state.revision.collectAsState()
    val scrollOffset by state.scrollOffset.collectAsState()

    // 한 프레임에 줄마다 여러 조각을 잰다. 기본 캐시(8개)로는 매 프레임 거의 전부 다시 잰다.
    val textMeasurer = rememberTextMeasurer(cacheSize = 512)
    val density = LocalDensity.current
    val cell = remember(textMeasurer, density) { textMeasurer.measure("W", PaneTextStyle).size }

    val focusRequester = remember { FocusRequester() }
    val currentOnFocus by rememberUpdatedState(onFocus)
    val field = rememberTextFieldState()

    // 확정된 글자만 셸로 보내고 필드를 비운다. 필드 상태가 곧 IME 버퍼라, 여기서 비우면 IME 쪽도 함께 비워진다.
    // (값을 받는 TextFieldValue API 는 빈 값을 다시 넘겨도 버퍼를 비우지 않아 다음 입력에 앞 글자가 누적됐다.)
    LaunchedEffect(field, state) {
        snapshotFlow { field.text.toString() to field.composition }
            .collect { (text, composition) ->
                if (composition != null || text.isEmpty()) return@collect

                field.clearText()
                state.input(committedBytes(text, state))
            }
    }

    LaunchedEffect(focused) {
        if (focused) focusRequester.requestFocus()
    }

    Box(
        modifier = modifier
            .background(TerminalBackground)
            .then(if (showFocusBorder && focused) Modifier.border(1.dp, MaterialTheme.colorScheme.primary) else Modifier)
            .onSizeChanged { size ->
                state.resize(
                    columns = (size.width / cell.width).coerceAtLeast(1),
                    rows = (size.height / cell.height).coerceAtLeast(1),
                )
            }
            .pointerInput(Unit) {
                detectTapGestures {
                    currentOnFocus()
                    focusRequester.requestFocus()
                }
            }
            .scrollable(
                orientation = Orientation.Vertical,
                state = rememberScrollableState { delta ->
                    state.scrollBy(delta, cell.height.toFloat())
                    delta
                },
            )
            .testTag(terminalPaneTestTag(state.id)),
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            // 에뮬레이터는 Compose 상태가 아니다. 이 값을 읽어 두어야 출력이 올 때 다시 그린다.
            revision

            drawTerminal(state.emulator, scrollOffset, textMeasurer, cell, focused)
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
                .widthIn(min = 1.dp)
                .focusRequester(focusRequester)
                .onFocusChanged { if (it.isFocused) onFocus() }
                .onPreviewKeyEvent { event -> onKey(event, composing = field.composition != null, state) },
            textStyle = PaneTextStyle.copy(background = TerminalBackground),
            cursorBrush = SolidColor(Color.Transparent),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.None,
                autoCorrectEnabled = false,
            ),
        )
    }
}

private fun committedBytes(text: String, state: TerminalPaneState): ByteArray {
    // 소프트 키보드의 Enter 는 키 이벤트가 아니라 줄바꿈 글자로 온다.
    val normalized = text.replace("\r\n", "\r").replace('\n', '\r')
    val wrapped = if (normalized.length > 1 && state.emulator.bracketedPaste) {
        "\u001b[200~$normalized\u001b[201~"
    } else {
        normalized
    }

    return wrapped.encodeToByteArray()
}

private fun onKey(event: KeyEvent, composing: Boolean, state: TerminalPaneState): Boolean {
    if (event.type != KeyEventType.KeyDown) return false
    // ⌘ 조합은 화면의 분할·탭 단축키와 붙여넣기에 남긴다.
    if (event.isMetaPressed) return false

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

private fun DrawScope.drawTerminal(
    emulator: TerminalEmulator,
    scrollOffset: Int,
    textMeasurer: TextMeasurer,
    cell: IntSize,
    focused: Boolean,
) {
    val cellWidth = cell.width.toFloat()
    val cellHeight = cell.height.toFloat()

    for (row in 0 until emulator.rows) {
        val index = row - scrollOffset
        if (index < -emulator.scrollbackSize) continue

        drawLine(emulator.line(index), emulator.columns, row * cellHeight, textMeasurer, cellWidth, cellHeight)
    }

    if (scrollOffset != 0 || !emulator.cursorVisible) return

    val line = emulator.line(emulator.cursorRow)
    val column = emulator.cursorColumn.coerceAtMost(line.columns - 1)
    val width = if (line.isWide(column)) cellWidth * 2 else cellWidth
    val topLeft = Offset(column * cellWidth, emulator.cursorRow * cellHeight)

    if (focused) {
        drawRect(TerminalForeground.copy(alpha = 0.6f), topLeft, Size(width, cellHeight))
    } else {
        drawRect(TerminalForeground.copy(alpha = 0.6f), topLeft, Size(width, cellHeight), style = Stroke(width = 1f))
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
    textMeasurer: TextMeasurer,
    cellWidth: Float,
    cellHeight: Float,
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
            drawCells(text.toString(), start, column - start, style, y, textMeasurer, cellWidth, cellHeight)
        } else {
            val width = if (line.isWide(column)) 2 else 1
            drawCells(line.textAt(column), start, width, style, y, textMeasurer, cellWidth, cellHeight)
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
    textMeasurer: TextMeasurer,
    cellWidth: Float,
    cellHeight: Float,
) {
    var foreground = style.foreground.resolve(TerminalForeground)
    var background = style.background.resolve(TerminalBackground)
    if (style.inverse) foreground = background.also { background = foreground }
    if (style.hidden) foreground = background
    if (style.dim) foreground = foreground.copy(alpha = 0.6f)

    val topLeft = Offset(column * cellWidth, y)

    if (background != TerminalBackground) {
        drawRect(background, topLeft, Size(cells * cellWidth, cellHeight))
    }

    if (text.isBlank()) return

    val layout: TextLayoutResult = textMeasurer.measure(
        text = text,
        style = PaneTextStyle.copy(
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
