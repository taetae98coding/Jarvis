package io.github.taetae98coding.jarvis.ui.terminal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import io.github.taetae98coding.jarvis.designsystem.theme.JarvisTheme
import io.github.taetae98coding.jarvis.domain.terminal.TerminalCommand

const val TerminalCommandDialogTestTag = "terminal:command-dialog"
const val TerminalCommandDialogTitleTestTag = "terminal:command-dialog:title"
const val TerminalCommandDialogCommandTestTag = "terminal:command-dialog:command"
const val TerminalCommandDialogConfirmTestTag = "terminal:command-dialog:confirm"

/** 사용자 명령을 더하거나([initial] 이 null) 고친다(docs/common/terminal-run.html R15). 공백 정리는 domain 이 한다. */
@Composable
internal fun TerminalCommandDialog(
    initial: TerminalCommand?,
    onSave: (title: String, command: String) -> Unit,
    onDismiss: () -> Unit,
) {
    var title by remember { mutableStateOf(initial?.title.orEmpty()) }
    var command by remember { mutableStateOf(initial?.command.orEmpty()) }
    val focus = remember { FocusRequester() }
    val canSave = command.isNotBlank()

    fun save() {
        if (canSave) onSave(title, command)
    }

    // 데스크톱의 하드웨어 Enter 는 한 줄 입력란의 IME 동작으로 오지 않을 때가 있어 키로도 받는다(NewWorktreeDialog 와 같다).
    val enterSaves = Modifier.onPreviewKeyEvent { event ->
        val enter = event.key == Key.Enter || event.key == Key.NumPadEnter
        if (event.type == KeyEventType.KeyDown && enter) save()
        enter
    }
    val keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
    val keyboardActions = KeyboardActions(onDone = { save() })

    LaunchedEffect(Unit) { focus.requestFocus() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "명령 추가" else "명령 편집") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(JarvisTheme.dimens.spacing.s)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("이름") },
                    placeholder = { Text("비우면 명령을 이름으로 씁니다") },
                    singleLine = true,
                    keyboardOptions = keyboardOptions,
                    keyboardActions = keyboardActions,
                    modifier = Modifier.fillMaxWidth().then(enterSaves).testTag(TerminalCommandDialogTitleTestTag),
                )
                OutlinedTextField(
                    value = command,
                    onValueChange = { command = it },
                    label = { Text("명령") },
                    placeholder = { Text("예: ./gradlew test") },
                    singleLine = true,
                    keyboardOptions = keyboardOptions,
                    keyboardActions = keyboardActions,
                    modifier = Modifier.fillMaxWidth().focusRequester(focus).then(enterSaves).testTag(TerminalCommandDialogCommandTestTag),
                )
            }
        },
        confirmButton = {
            Button(onClick = ::save, enabled = canSave, modifier = Modifier.testTag(TerminalCommandDialogConfirmTestTag)) { Text("저장") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("취소") } },
        modifier = Modifier.testTag(TerminalCommandDialogTestTag),
    )
}
