package io.github.taetae98coding.jarvis.ui.terminal

import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.selectAll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction

/** 패널·탭의 이름 입력칸. Enter·포커스를 잃으면 확정, Esc 는 취소. 들어올 때 이름 전체가 선택돼 곧바로 덮어쓸 수 있다. */
@Composable
internal fun TerminalNameField(
    initial: String,
    textStyle: TextStyle,
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
        textStyle = textStyle.copy(color = color),
        cursorBrush = SolidColor(color),
        onKeyboardAction = { finish(commit = true) },
        modifier = modifier
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
