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
import androidx.compose.runtime.rememberCoroutineScope
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
import io.github.taetae98coding.jarvis.domain.terminal.GitWorktree
import kotlinx.coroutines.launch

const val TerminalNewWorktreeDialogTestTag = "terminal:new-worktree-dialog"
const val TerminalNewWorktreeBranchTestTag = "terminal:new-worktree-branch"
const val TerminalNewWorktreeBaseTestTag = "terminal:new-worktree-base"
const val TerminalNewWorktreeDirectoryTestTag = "terminal:new-worktree-directory"
const val TerminalNewWorktreeErrorTestTag = "terminal:new-worktree-error"
const val TerminalNewWorktreeConfirmTestTag = "terminal:new-worktree-confirm"
const val TerminalNewWorktreeCancelTestTag = "terminal:new-worktree-cancel"

/**
 * 새 워크트리의 브랜치·기준 브랜치·폴더를 받는다. 탭 종류는 묻지 않는다 — 만든 패널은 Claude 탭 하나로 시작하고(못 띄우는 타깃은 빈 패널) 다른 탭은 + 메뉴에서 연다.
 * 기준 브랜치는 + 를 누른 패널의 워크트리 [worktree] 가 지금 체크아웃한 브랜치로 시작하고, 비우면 그 값으로 돌아간다
 * (둘 다 없으면 null — git 의 HEAD).
 * [onCreate] 가 git 을 돌리는 동안 창은 남고 입력이 잠기며, 실패하면 그 문구를 보이고 다시 누를 수 있다.
 * 성공하면 [onDismiss] 로 닫는다. 입력 중인 글자는 창이 닫히면 버린다.
 */
@Composable
internal fun NewWorktreeDialog(
    worktree: GitWorktree,
    onCreate: suspend (branch: String, baseBranch: String?, directory: String) -> Result<Unit>,
    onDismiss: () -> Unit,
) {
    var branch by remember { mutableStateOf("") }
    var baseBranch by remember { mutableStateOf(worktree.branch.orEmpty()) }
    var directory by remember { mutableStateOf("") }
    // 폴더를 한 번 직접 고치면 브랜치를 더 따라가지 않는다.
    var directoryEdited by remember { mutableStateOf(false) }
    var creating by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val branchFocus = remember { FocusRequester() }

    val trimmedBranch = branch.trim()
    val defaultDirectory = if (trimmedBranch.isEmpty()) "" else worktree.defaultWorktreePath(trimmedBranch)
    val shownDirectory = if (directoryEdited) directory else defaultDirectory
    val canCreate = trimmedBranch.isNotEmpty() && !creating
    val effectiveBase = baseBranch.trim().ifEmpty { worktree.branch.orEmpty() }.ifEmpty { null }

    fun create() {
        if (!canCreate) return
        creating = true
        error = null
        scope.launch {
            onCreate(trimmedBranch, effectiveBase, shownDirectory.trim().ifEmpty { defaultDirectory })
                .onSuccess { onDismiss() }
                .onFailure {
                    error = it.message?.takeIf(String::isNotBlank) ?: "워크트리를 만들지 못했습니다"
                    creating = false
                }
        }
    }

    // 데스크톱의 하드웨어 Enter 는 한 줄 입력란의 IME 동작으로 오지 않을 때가 있어 키로도 받는다.
    val enterCreates = Modifier.onPreviewKeyEvent { event ->
        val enter = event.key == Key.Enter || event.key == Key.NumPadEnter
        if (event.type == KeyEventType.KeyDown && enter) create()
        enter
    }
    val keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
    val keyboardActions = KeyboardActions(onDone = { create() })

    LaunchedEffect(Unit) { branchFocus.requestFocus() }

    AlertDialog(
        onDismissRequest = { if (!creating) onDismiss() },
        title = { Text("새 워크트리") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(JarvisTheme.dimens.spacing.s)) {
                OutlinedTextField(
                    value = branch,
                    onValueChange = { branch = it },
                    label = { Text("브랜치") },
                    singleLine = true,
                    enabled = !creating,
                    keyboardOptions = keyboardOptions,
                    keyboardActions = keyboardActions,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(branchFocus)
                        .then(enterCreates)
                        .testTag(TerminalNewWorktreeBranchTestTag),
                )

                OutlinedTextField(
                    value = baseBranch,
                    onValueChange = { baseBranch = it },
                    label = { Text("기준 브랜치") },
                    placeholder = { Text("HEAD") },
                    singleLine = true,
                    enabled = !creating,
                    keyboardOptions = keyboardOptions,
                    keyboardActions = keyboardActions,
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(enterCreates)
                        .testTag(TerminalNewWorktreeBaseTestTag),
                )

                OutlinedTextField(
                    value = shownDirectory,
                    onValueChange = {
                        directoryEdited = true
                        directory = it
                    },
                    label = { Text("폴더") },
                    placeholder = { Text(worktree.defaultWorktreePath("<브랜치>")) },
                    singleLine = true,
                    enabled = !creating,
                    keyboardOptions = keyboardOptions,
                    keyboardActions = keyboardActions,
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(enterCreates)
                        .testTag(TerminalNewWorktreeDirectoryTestTag),
                )

                error?.let {
                    Text(
                        text = it,
                        style = JarvisTheme.typography.bodySmall,
                        color = JarvisTheme.colorScheme.error,
                        modifier = Modifier.testTag(TerminalNewWorktreeErrorTestTag),
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = ::create,
                enabled = canCreate,
                modifier = Modifier.testTag(TerminalNewWorktreeConfirmTestTag),
            ) {
                ProgressButtonContent(text = "확인", inProgress = creating)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !creating,
                modifier = Modifier.testTag(TerminalNewWorktreeCancelTestTag),
            ) {
                Text("취소")
            }
        },
        modifier = Modifier.testTag(TerminalNewWorktreeDialogTestTag),
    )
}
