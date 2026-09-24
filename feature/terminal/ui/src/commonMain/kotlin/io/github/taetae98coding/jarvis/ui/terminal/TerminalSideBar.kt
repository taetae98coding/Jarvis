package io.github.taetae98coding.jarvis.ui.terminal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.taetae98coding.jarvis.designsystem.component.JarvisIconButton
import io.github.taetae98coding.jarvis.designsystem.component.JarvisIconButtonDefaults
import io.github.taetae98coding.jarvis.designsystem.icon.JarvisIcons
import io.github.taetae98coding.jarvis.designsystem.theme.JarvisTheme

const val TerminalSideBarTestTag = "terminal:side-bar"
const val TerminalSideBarToggleTestTag = "terminal:side-bar:toggle"
const val TerminalSideBarContentTestTag = "terminal:side-bar:content"
const val TerminalSideBarFilesTestTag = "terminal:side-bar:section:files"
const val TerminalSideBarGitTestTag = "terminal:side-bar:section:git"
const val TerminalSideBarNoFolderTestTag = "terminal:side-bar:no-folder"

internal enum class TerminalSideBarSection {
    Files,
    Git,
}

/**
 * 탭 영역 오른쪽의 사이드 바. 오른쪽 끝의 레일은 늘 보이고, 접으면 그 왼쪽의 내용만 사라진다.
 * 접힘·구획은 화면에 있는 동안만 기억한다(docs/common/terminal-side-bar.html R4).
 */
@Composable
internal fun TerminalSideBar(
    viewModel: TerminalSideBarViewModel,
    directory: String?,
    onOpenFile: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by rememberSaveable { mutableStateOf(true) }
    var section by rememberSaveable(stateSaver = SectionSaver) { mutableStateOf(TerminalSideBarSection.Files) }
    LaunchedEffect(viewModel, directory) { viewModel.setDirectory(directory) }

    Row(
        modifier = modifier.fillMaxHeight().testTag(TerminalSideBarTestTag),
        horizontalArrangement = Arrangement.spacedBy(JarvisTheme.dimens.spacing.xs),
    ) {
        if (expanded) {
            Column(modifier = Modifier.width(TerminalSideBarDefaults.width).fillMaxHeight().testTag(TerminalSideBarContentTestTag)) {
                when (section) {
                    TerminalSideBarSection.Files -> {
                        val tree by viewModel.fileTree.collectAsStateWithLifecycle()
                        TerminalFileTree(
                            state = tree,
                            onToggle = viewModel::toggle,
                            onOpen = onOpenFile,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }

                    TerminalSideBarSection.Git -> {
                        val status by viewModel.gitStatus.collectAsStateWithLifecycle()
                        val graph by viewModel.gitGraph.collectAsStateWithLifecycle()
                        val error by viewModel.gitError.collectAsStateWithLifecycle()
                        val pushing by viewModel.pushing.collectAsStateWithLifecycle()
                        TerminalGitPanel(
                            state = status,
                            graph = graph,
                            error = error,
                            pushing = pushing,
                            onStage = viewModel::stage,
                            onUnstage = viewModel::unstage,
                            onPush = viewModel::push,
                            onOpen = onOpenFile,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }
        }

        Column(modifier = Modifier.fillMaxHeight()) {
            JarvisIconButton(
                icon = if (expanded) JarvisIcons.ChevronRight else JarvisIcons.ChevronLeft,
                contentDescription = if (expanded) "사이드 바 접기" else "사이드 바 펼치기",
                onClick = { expanded = !expanded },
                modifier = Modifier.testTag(TerminalSideBarToggleTestTag),
            )

            TerminalSideBarSection.entries.forEach { entry ->
                val selected = expanded && section == entry
                JarvisIconButton(
                    icon = TerminalSideBarDefaults.icon(entry),
                    contentDescription = TerminalSideBarDefaults.label(entry),
                    // 펼쳐 보고 있는 구획을 다시 누르면 접는다.
                    onClick = {
                        expanded = !selected
                        section = entry
                    },
                    colors = JarvisIconButtonDefaults.colors(
                        contentColor = if (selected) JarvisTheme.colorScheme.primary else JarvisTheme.colorScheme.onSurfaceVariant,
                    ),
                    modifier = Modifier.testTag(TerminalSideBarDefaults.testTag(entry)),
                )
            }
        }
    }
}

private val SectionSaver = Saver<TerminalSideBarSection, String>(save = { it.name }, restore = { TerminalSideBarSection.valueOf(it) })

internal object TerminalSideBarDefaults {
    val width: Dp = 280.dp

    fun icon(section: TerminalSideBarSection) =
        when (section) {
            TerminalSideBarSection.Files -> JarvisIcons.Folder
            TerminalSideBarSection.Git -> JarvisIcons.GitBranch
        }

    fun label(section: TerminalSideBarSection): String =
        when (section) {
            TerminalSideBarSection.Files -> "파일"
            TerminalSideBarSection.Git -> "Git"
        }

    fun testTag(section: TerminalSideBarSection): String =
        when (section) {
            TerminalSideBarSection.Files -> TerminalSideBarFilesTestTag
            TerminalSideBarSection.Git -> TerminalSideBarGitTestTag
        }
}
