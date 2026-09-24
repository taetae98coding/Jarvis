package io.github.taetae98coding.jarvis.ui.terminal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import io.github.taetae98coding.jarvis.designsystem.theme.JarvisTheme
import io.github.taetae98coding.jarvis.domain.terminal.GitWorktree

const val TerminalCloseWorktreeDialogTestTag = "terminal:close-worktree-dialog"
const val TerminalCloseWorktreeRemoveTestTag = "terminal:close-worktree-remove"
const val TerminalCloseWorktreeDeleteDirectoryTestTag = "terminal:close-worktree-delete-directory"
const val TerminalCloseWorktreeErrorTestTag = "terminal:close-worktree-error"
const val TerminalCloseWorktreeConfirmTestTag = "terminal:close-worktree-confirm"
const val TerminalCloseWorktreeCancelTestTag = "terminal:close-worktree-cancel"

/**
 * 워크트리 패널을 닫기 전에 워크트리·브랜치와 폴더를 함께 지울지 고른다. 둘 다 켜진 채로 뜨고, "폴더도 지우기" 는
 * "워크트리·브랜치 지우기" 가 켜져 있을 때만 뜻이 있다. [onClose] 는 git 을 기다리지 않는다 — 창은 부른 쪽이 곧바로
 * 닫고, git 은 뒤에서 돈다. 실패하면 부른 쪽이 누를 때의 값([initialRemoveWorktree]·[initialDeleteDirectory])과
 * [error] 로 창을 다시 띄운다.
 */
@Composable
internal fun CloseWorktreeDialog(
    worktree: GitWorktree,
    onClose: (removeWorktree: Boolean, deleteDirectory: Boolean) -> Unit,
    onDismiss: () -> Unit,
    initialRemoveWorktree: Boolean = true,
    initialDeleteDirectory: Boolean = true,
    error: String? = null,
) {
    var removeWorktree by remember { mutableStateOf(initialRemoveWorktree) }
    // 워크트리·브랜치 지우기를 껐다 켜면 끄기 전 값으로 돌아오도록 따로 기억한다. 다시 뜬 창에서 앞이 꺼져 있으면
    // 넘어온 값이 늘 false 라 기본값(켬)으로 돌아온다.
    var deleteDirectory by remember { mutableStateOf(!initialRemoveWorktree || initialDeleteDirectory) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("워크트리 닫기") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(JarvisTheme.dimens.spacing.s)) {
                Column {
                    worktree.branch?.let { Text(text = it, style = JarvisTheme.typography.bodyMedium) }
                    Text(
                        text = worktree.path,
                        style = TerminalPanelItemDefaults.directoryStyle,
                        color = TerminalPanelItemDefaults.directoryColor(selected = false),
                    )
                }

                Column {
                    CheckboxRow(
                        text = "워크트리·브랜치 지우기",
                        checked = removeWorktree,
                        enabled = true,
                        onCheckedChange = { removeWorktree = it },
                        modifier = Modifier.testTag(TerminalCloseWorktreeRemoveTestTag),
                    )
                    CheckboxRow(
                        text = "폴더도 지우기",
                        checked = removeWorktree && deleteDirectory,
                        enabled = removeWorktree,
                        onCheckedChange = { deleteDirectory = it },
                        modifier = Modifier.testTag(TerminalCloseWorktreeDeleteDirectoryTestTag),
                    )
                }

                error?.let {
                    Text(
                        text = it,
                        style = JarvisTheme.typography.bodySmall,
                        color = JarvisTheme.colorScheme.error,
                        modifier = Modifier.testTag(TerminalCloseWorktreeErrorTestTag),
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onClose(removeWorktree, removeWorktree && deleteDirectory) },
                modifier = Modifier.testTag(TerminalCloseWorktreeConfirmTestTag),
            ) {
                Text("확인")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag(TerminalCloseWorktreeCancelTestTag),
            ) {
                Text("취소")
            }
        },
        modifier = Modifier.testTag(TerminalCloseWorktreeDialogTestTag),
    )
}

@Composable
private fun CheckboxRow(
    text: String,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .toggleable(value = checked, enabled = enabled, role = Role.Checkbox, onValueChange = onCheckedChange),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 토글 시맨틱은 줄이 갖고 있으므로 체크박스는 표시만 한다.
        Checkbox(checked = checked, onCheckedChange = null, enabled = enabled)
        Text(
            text = text,
            style = JarvisTheme.typography.bodyMedium,
            color = if (enabled) JarvisTheme.colorScheme.onSurface else JarvisTheme.colorScheme.onSurface.copy(alpha = DisabledAlpha),
        )
    }
}

private const val DisabledAlpha = 0.38f
