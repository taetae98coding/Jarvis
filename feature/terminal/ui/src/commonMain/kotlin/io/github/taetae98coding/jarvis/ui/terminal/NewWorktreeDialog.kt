package io.github.taetae98coding.jarvis.ui.terminal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import io.github.taetae98coding.jarvis.designsystem.theme.JarvisTheme
import io.github.taetae98coding.jarvis.domain.terminal.GitBranch
import io.github.taetae98coding.jarvis.domain.terminal.GitWorktree

const val TerminalNewWorktreeDialogTestTag = "terminal:new-worktree-dialog"
const val TerminalNewWorktreeBranchTestTag = "terminal:new-worktree-branch"
const val TerminalNewWorktreeBaseTestTag = "terminal:new-worktree-base"
const val TerminalNewWorktreeBaseSuggestionsTestTag = "terminal:new-worktree-base-suggestions"
const val TerminalNewWorktreeDirectoryTestTag = "terminal:new-worktree-directory"
const val TerminalNewWorktreeErrorTestTag = "terminal:new-worktree-error"
const val TerminalNewWorktreeConfirmTestTag = "terminal:new-worktree-confirm"
const val TerminalNewWorktreeCancelTestTag = "terminal:new-worktree-cancel"

fun terminalNewWorktreeBaseSuggestionTestTag(name: String): String = "terminal:new-worktree-base-suggestion:$name"

fun terminalNewWorktreeBaseSuggestionLabelTestTag(name: String): String = "terminal:new-worktree-base-suggestion-label:$name"

internal const val NewWorktreeBaseSuggestionMaxCount = 50

/**
 * 기준 브랜치 입력란 아래에 펼 후보(docs/common/terminal-worktree-base-branch.html R4·R5). 사용자가 [query] 를 고치기 전([edited]
 * 가 거짓)이나 비웠을 때는 모두, 아니면 이름에 대소문자 없이 들어 있는 것만. 순서는 [branches] 그대로다.
 */
internal fun baseBranchSuggestions(branches: List<GitBranch>, query: String, edited: Boolean): List<GitBranch> {
    val needle = query.trim()
    val matched = if (!edited || needle.isEmpty()) branches else branches.filter { it.name.contains(needle, ignoreCase = true) }
    return matched.take(NewWorktreeBaseSuggestionMaxCount)
}

/**
 * 새 워크트리의 브랜치·기준 브랜치·폴더를 받는다. 탭 종류는 묻지 않는다 — 만든 패널은 Claude 탭 하나로 시작하고(못 띄우는 타깃은 빈 패널) 다른 탭은 + 메뉴에서 연다.
 * 기준 브랜치는 + 를 누른 패널의 워크트리 [worktree] 가 지금 체크아웃한 브랜치로 시작하고, 비우면 그 값으로 돌아간다
 * (둘 다 없으면 null — git 의 HEAD). 입력란이 포커스를 받거나 글자가 바뀌면 [branches] 에서 거른 후보를 아래에 편다.
 * [onCreate] 는 git 을 기다리지 않는다 — 창은 부른 쪽이 곧바로 닫고, git 은 뒤에서 돈다. 실패하면 부른 쪽이
 * 누를 때의 값([initialBranch]·[initialBaseBranch]·[initialDirectory])과 [error] 로 창을 다시 띄운다.
 * 입력 중인 글자는 창이 닫히면 버린다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun NewWorktreeDialog(
    worktree: GitWorktree,
    branches: List<GitBranch>,
    onCreate: (branch: String, baseBranch: String?, directory: String) -> Unit,
    onDismiss: () -> Unit,
    initialBranch: String = "",
    initialBaseBranch: String? = null,
    initialDirectory: String? = null,
    error: String? = null,
) {
    var branch by remember { mutableStateOf(initialBranch) }
    // 후보를 고르면 커서를 끝으로 옮겨야 해서 글자만이 아니라 선택 범위까지 든다.
    var baseBranch by remember { mutableStateOf(TextFieldValue(initialBaseBranch ?: worktree.branch.orEmpty())) }
    var baseBranchEdited by remember { mutableStateOf(false) }
    var baseBranchMenuOpen by remember { mutableStateOf(false) }
    var directory by remember { mutableStateOf(initialDirectory.orEmpty()) }
    // 폴더를 한 번 직접 고치면 브랜치를 더 따라가지 않는다. 다시 뜬 창은 기본 폴더와 다를 때만 고친 것으로 본다.
    var directoryEdited by remember {
        mutableStateOf(initialDirectory != null && initialDirectory != worktree.defaultWorktreePath(initialBranch.trim()))
    }
    val branchFocus = remember { FocusRequester() }

    val trimmedBranch = branch.trim()
    val defaultDirectory = if (trimmedBranch.isEmpty()) "" else worktree.defaultWorktreePath(trimmedBranch)
    val shownDirectory = if (directoryEdited) directory else defaultDirectory
    val canCreate = trimmedBranch.isNotEmpty()
    val suggestions = baseBranchSuggestions(branches, baseBranch.text, baseBranchEdited)
    val suggestionsShown = baseBranchMenuOpen && suggestions.isNotEmpty()
    val effectiveBase = baseBranch.text.trim().ifEmpty { worktree.branch.orEmpty() }.ifEmpty { null }

    fun create() {
        if (!canCreate) return
        onCreate(trimmedBranch, effectiveBase, shownDirectory.trim().ifEmpty { defaultDirectory })
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
        onDismissRequest = onDismiss,
        title = { Text("새 워크트리") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(JarvisTheme.dimens.spacing.s)) {
                OutlinedTextField(
                    value = branch,
                    onValueChange = { branch = it },
                    label = { Text("브랜치") },
                    singleLine = true,
                    keyboardOptions = keyboardOptions,
                    keyboardActions = keyboardActions,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(branchFocus)
                        .then(enterCreates)
                        .testTag(TerminalNewWorktreeBranchTestTag),
                )

                ExposedDropdownMenuBox(
                    expanded = suggestionsShown,
                    // 입력란을 누르면 포커스로 이미 펴졌을 수 있어 닫는 쪽 토글은 받지 않는다. 닫기는 바깥 누르기·포커스 잃기·고르기다.
                    onExpandedChange = { if (it) baseBranchMenuOpen = true },
                ) {
                    OutlinedTextField(
                        value = baseBranch,
                        onValueChange = {
                            if (it.text != baseBranch.text) {
                                baseBranchEdited = true
                                baseBranchMenuOpen = true
                            }
                            baseBranch = it
                        },
                        label = { Text("기준 브랜치") },
                        placeholder = { Text("HEAD") },
                        singleLine = true,
                        keyboardOptions = keyboardOptions,
                        keyboardActions = keyboardActions,
                        modifier = Modifier
                            .fillMaxWidth()
                            // 편집 가능한 앵커여야 목록이 포커스를 가져가지 않아 목록이 떠 있어도 글자가 입력란에 간다.
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable)
                            .onFocusChanged { baseBranchMenuOpen = it.isFocused }
                            .then(enterCreates)
                            .testTag(TerminalNewWorktreeBaseTestTag),
                    )

                    ExposedDropdownMenu(
                        expanded = suggestionsShown,
                        onDismissRequest = { baseBranchMenuOpen = false },
                        modifier = Modifier.testTag(TerminalNewWorktreeBaseSuggestionsTestTag),
                    ) {
                        suggestions.forEach { suggestion ->
                            DropdownMenuItem(
                                text = { Text(text = suggestion.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                trailingIcon = {
                                    GitBranchKindLabel(
                                        remote = suggestion.remote,
                                        modifier = Modifier.testTag(terminalNewWorktreeBaseSuggestionLabelTestTag(suggestion.name)),
                                    )
                                },
                                onClick = {
                                    baseBranch = TextFieldValue(suggestion.name, TextRange(suggestion.name.length))
                                    baseBranchMenuOpen = false
                                },
                                contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                                modifier = Modifier.testTag(terminalNewWorktreeBaseSuggestionTestTag(suggestion.name)),
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = shownDirectory,
                    onValueChange = {
                        directoryEdited = true
                        directory = it
                    },
                    label = { Text("폴더") },
                    placeholder = { Text(worktree.defaultWorktreePath("<브랜치>")) },
                    singleLine = true,
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
                Text("확인")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag(TerminalNewWorktreeCancelTestTag),
            ) {
                Text("취소")
            }
        },
        modifier = Modifier.testTag(TerminalNewWorktreeDialogTestTag),
    )
}

/** 로컬 브랜치는 `local`, 원격 추적 브랜치는 그 원격 이름. 둘은 바탕색으로도 갈린다. */
@Composable
private fun GitBranchKindLabel(remote: String?, modifier: Modifier = Modifier) {
    val colors = JarvisTheme.colorScheme
    Text(
        text = remote ?: "local",
        style = JarvisTheme.typography.labelSmall,
        color = if (remote == null) colors.onPrimaryContainer else colors.onTertiaryContainer,
        maxLines = 1,
        modifier = modifier
            .background(if (remote == null) colors.primaryContainer else colors.tertiaryContainer, CircleShape)
            .padding(horizontal = JarvisTheme.dimens.spacing.s, vertical = JarvisTheme.dimens.spacing.xxs),
    )
}
