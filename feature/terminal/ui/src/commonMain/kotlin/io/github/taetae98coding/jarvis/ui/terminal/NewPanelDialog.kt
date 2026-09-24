package io.github.taetae98coding.jarvis.ui.terminal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
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
import kotlinx.coroutines.launch

const val TerminalNewPanelDialogTestTag = "terminal:new-panel-dialog"
const val TerminalNewPanelNameTestTag = "terminal:new-panel-name"
const val TerminalNewPanelDirectoryTestTag = "terminal:new-panel-directory"
const val TerminalNewPanelBrowseTestTag = "terminal:new-panel-browse"
const val TerminalNewPanelConfirmTestTag = "terminal:new-panel-confirm"
const val TerminalNewPanelCancelTestTag = "terminal:new-panel-cancel"

/**
 * 새 패널의 제목·폴더를 받는다. 탭 종류는 묻지 않는다 — 패널은 비어 있고 첫 탭은 빈 패널의 + 메뉴에서 연다.
 * 빈 값의 기본값은 도메인이 정하고, 여기서는 친 그대로 넘긴다. 입력 중인 글자는 창이 닫히면 버린다.
 */
@Composable
internal fun NewPanelDialog(
    defaultName: String,
    onCreate: (name: String, directory: String) -> Unit,
    onDismiss: () -> Unit,
    picker: DirectoryPicker? = directoryPicker,
) {
    var name by remember { mutableStateOf("") }
    var directory by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val nameFocus = remember { FocusRequester() }

    fun create() = onCreate(name, directory)

    // 데스크톱의 하드웨어 Enter 는 한 줄 입력란의 IME 동작으로 오지 않을 때가 있어 키로도 받는다.
    val enterCreates = Modifier.onPreviewKeyEvent { event ->
        val enter = event.key == Key.Enter || event.key == Key.NumPadEnter
        if (event.type == KeyEventType.KeyDown && enter) create()
        enter
    }
    val keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
    val keyboardActions = KeyboardActions(onDone = { create() })

    LaunchedEffect(Unit) { nameFocus.requestFocus() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("새 패널") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(JarvisTheme.dimens.spacing.s)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("제목") },
                    placeholder = { Text(defaultName) },
                    singleLine = true,
                    keyboardOptions = keyboardOptions,
                    keyboardActions = keyboardActions,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(nameFocus)
                        .then(enterCreates)
                        .testTag(TerminalNewPanelNameTestTag),
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(JarvisTheme.dimens.spacing.xs),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedTextField(
                        value = directory,
                        onValueChange = { directory = it },
                        label = { Text("폴더") },
                        placeholder = { Text("홈 디렉터리") },
                        singleLine = true,
                        keyboardOptions = keyboardOptions,
                        keyboardActions = keyboardActions,
                        modifier = Modifier
                            .weight(1f)
                            .then(enterCreates)
                            .testTag(TerminalNewPanelDirectoryTestTag),
                    )

                    if (picker != null) {
                        TextButton(
                            onClick = {
                                scope.launch { picker.pick(directory.trim().ifEmpty { null })?.let { directory = it } }
                            },
                            modifier = Modifier.testTag(TerminalNewPanelBrowseTestTag),
                        ) {
                            Text("찾아보기")
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = ::create, modifier = Modifier.testTag(TerminalNewPanelConfirmTestTag)) {
                Text("확인")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.testTag(TerminalNewPanelCancelTestTag)) {
                Text("취소")
            }
        },
        modifier = Modifier.testTag(TerminalNewPanelDialogTestTag),
    )
}
