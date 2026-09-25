package io.github.taetae98coding.jarvis.ui.terminal

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.taetae98coding.jarvis.designsystem.icon.JarvisIcons
import io.github.taetae98coding.jarvis.designsystem.theme.JarvisTheme
import io.github.taetae98coding.jarvis.domain.terminal.ProjectKind
import io.github.taetae98coding.jarvis.domain.terminal.TerminalCommand
import io.github.taetae98coding.jarvis.ui.device.DeviceScreens

const val TerminalRunMenuAndroidTestTag = "terminal:run-menu:android"
const val TerminalRunMenuIosTestTag = "terminal:run-menu:ios"
const val TerminalRunMenuAddCommandTestTag = "terminal:run-menu:add-command"

/** 그룹의 실행(▶) 버튼. null 은 그룹이 없는 빈 패널의 버튼이다. */
fun terminalRunTestTag(groupId: Long?): String = "terminal:run:${groupId ?: "none"}"

fun terminalRunCommandTestTag(id: Long): String = "terminal:run-menu:command:$id"

fun terminalRunCommandEditTestTag(id: Long): String = "terminal:run-menu:command-edit:$id"

fun terminalRunCommandDeleteTestTag(id: Long): String = "terminal:run-menu:command-delete:$id"

private sealed interface RunDialog {
    data class Android(val directory: String) : RunDialog

    data class Ios(val directory: String) : RunDialog

    data object AddCommand : RunDialog

    data class EditCommand(val command: TerminalCommand) : RunDialog
}

/**
 * + 옆의 ▶ 버튼과 실행 메뉴, 메뉴에서 여는 창(docs/common/terminal-run.html R1–R2·R6·R12·R15–R16). 실행은 [groupId] 그룹
 * (null 이면 포커스된 그룹, 없으면 새 그룹)에 탭을 연다.
 */
@Composable
internal fun TerminalRunButton(
    groupId: Long?,
    viewModel: TerminalViewModel,
    devices: DeviceScreens?,
    onExpandedChange: (Boolean) -> Unit = {},
) {
    val workspace = viewModel.workspace.collectAsStateWithLifecycle().value ?: return
    val panelId = workspace.selectedPanelId
    val directory = workspace.sideBarDirectory
    val commands = panelId?.let(workspace::commandsOf).orEmpty()
    val owner = panelId?.let(workspace::runOwner)

    var expanded by remember { mutableStateOf(false) }
    var dialog by remember { mutableStateOf<RunDialog?>(null) }

    fun setExpanded(value: Boolean) {
        expanded = value
        onExpandedChange(value)
    }

    fun open(value: RunDialog) {
        setExpanded(false)
        dialog = value
    }

    // 메뉴가 뜬 채로 버튼이 사라져도(그룹이 닫히는 등) 닫힌 것으로 알린다.
    val currentOnExpandedChange by rememberUpdatedState(onExpandedChange)
    DisposableEffect(Unit) { onDispose { if (expanded) currentOnExpandedChange(false) } }

    Box {
        TerminalNewTabButton(
            onClick = { setExpanded(true) },
            icon = JarvisIcons.Play,
            contentDescription = "실행",
            modifier = Modifier.testTag(terminalRunTestTag(groupId)),
        )

        DropdownMenu(expanded = expanded, onDismissRequest = { setExpanded(false) }) {
            if (viewModel.isProjectRunSupported && directory != null) {
                // 메뉴 내용은 메뉴가 떠 있을 때만 컴포즈되므로 판정도 그동안만 한다(R5).
                val kinds by remember(directory) { viewModel.projectKinds(directory) }.collectAsStateWithLifecycle(emptySet())

                if (ProjectKind.Android in kinds) {
                    DropdownMenuItem(
                        text = { Text("Android 앱 실행…") },
                        leadingIcon = { Icon(imageVector = JarvisIcons.Android, contentDescription = null) },
                        onClick = { open(RunDialog.Android(directory)) },
                        modifier = Modifier.testTag(TerminalRunMenuAndroidTestTag),
                    )
                }
                if (ProjectKind.IOS in kinds) {
                    DropdownMenuItem(
                        text = { Text("iOS 앱 실행…") },
                        leadingIcon = { Icon(imageVector = JarvisIcons.Apple, contentDescription = null) },
                        onClick = { open(RunDialog.Ios(directory)) },
                        modifier = Modifier.testTag(TerminalRunMenuIosTestTag),
                    )
                }
                if (kinds.isNotEmpty()) HorizontalDivider()
            }

            Text(
                text = "명령",
                style = JarvisTheme.typography.labelMedium,
                color = JarvisTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = JarvisTheme.dimens.spacing.m, vertical = JarvisTheme.dimens.spacing.s),
            )

            commands.forEach { command ->
                CommandItem(
                    command = command,
                    onRun = {
                        setExpanded(false)
                        viewModel.runCommand(groupId, command)
                    },
                    onEdit = { open(RunDialog.EditCommand(command)) },
                    onDelete = { viewModel.removeCommand(command.id) },
                )
            }

            DropdownMenuItem(
                text = { Text("명령 추가…") },
                leadingIcon = { Icon(imageVector = JarvisIcons.Add, contentDescription = null) },
                onClick = { open(RunDialog.AddCommand) },
                modifier = Modifier.testTag(TerminalRunMenuAddCommandTestTag),
            )
        }
    }

    when (val current = dialog) {
        is RunDialog.Android -> AndroidRunDialog(
            directory = current.directory,
            load = { viewModel.androidProject(it) },
            devices = devices,
            remembered = owner?.androidRun,
            onRun = { request ->
                dialog = null
                viewModel.runAndroid(groupId, request)
            },
            onDismiss = { dialog = null },
        )

        is RunDialog.Ios -> IosRunDialog(
            directory = current.directory,
            load = { viewModel.iosProject(it) },
            devices = devices,
            remembered = owner?.iosRun,
            onRun = { request ->
                dialog = null
                viewModel.runIos(groupId, request)
            },
            onDismiss = { dialog = null },
        )

        RunDialog.AddCommand -> TerminalCommandDialog(
            initial = null,
            onSave = { title, command ->
                dialog = null
                viewModel.addCommand(title, command)
            },
            onDismiss = { dialog = null },
        )

        is RunDialog.EditCommand -> TerminalCommandDialog(
            initial = current.command,
            onSave = { title, command ->
                dialog = null
                viewModel.editCommand(current.command.id, title, command)
            },
            onDismiss = { dialog = null },
        )

        null -> Unit
    }
}

@Composable
private fun CommandItem(command: TerminalCommand, onRun: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit) {
    DropdownMenuItem(
        text = {
            Column {
                Text(text = command.label, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (command.title != null) {
                    Text(
                        text = command.command,
                        style = JarvisTheme.typography.bodySmall,
                        color = JarvisTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        },
        leadingIcon = { Icon(imageVector = JarvisIcons.Terminal, contentDescription = null) },
        trailingIcon = {
            Row {
                IconButton(onClick = onEdit, modifier = Modifier.testTag(terminalRunCommandEditTestTag(command.id))) {
                    Icon(imageVector = JarvisIcons.Edit, contentDescription = "편집", modifier = Modifier.size(JarvisTheme.dimens.iconSize.small))
                }
                IconButton(onClick = onDelete, modifier = Modifier.testTag(terminalRunCommandDeleteTestTag(command.id))) {
                    Icon(imageVector = JarvisIcons.Close, contentDescription = "삭제", modifier = Modifier.size(JarvisTheme.dimens.iconSize.small))
                }
            }
        },
        onClick = onRun,
        modifier = Modifier.testTag(terminalRunCommandTestTag(command.id)),
    )
}
