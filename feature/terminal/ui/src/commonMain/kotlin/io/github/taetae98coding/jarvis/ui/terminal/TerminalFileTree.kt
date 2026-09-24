package io.github.taetae98coding.jarvis.ui.terminal

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import io.github.taetae98coding.jarvis.designsystem.icon.JarvisIcons
import io.github.taetae98coding.jarvis.designsystem.theme.JarvisTheme

const val TerminalFilesRootTestTag = "terminal:files:root"
const val TerminalFilesUnreadableTestTag = "terminal:files:unreadable"

fun terminalFileEntryTestTag(path: String): String = "terminal:files:entry:$path"

@Composable
internal fun TerminalFileTree(
    state: FileTreeState,
    onToggle: (String) -> Unit,
    onOpen: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (state) {
        FileTreeState.Loading -> Box(modifier = modifier)

        FileTreeState.NoFolder -> TerminalSideBarMessage(
            text = "폴더가 없는 패널입니다",
            modifier = modifier.testTag(TerminalSideBarNoFolderTestTag),
        )

        is FileTreeState.Unreadable -> Column(modifier = modifier) {
            FileTreeRoot(state.root)
            TerminalSideBarMessage(text = "폴더를 읽을 수 없습니다", modifier = Modifier.testTag(TerminalFilesUnreadableTestTag))
        }

        is FileTreeState.Loaded -> Column(modifier = modifier) {
            FileTreeRoot(state.root)
            LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f)) {
                items(state.rows, key = { it.first.path }) { row ->
                    FileTreeItem(
                        row = row,
                        onClick = { if (row.last.isDirectory) onToggle(row.toggleTarget) else onOpen(row.last.path) },
                        modifier = Modifier.fillMaxWidth().testTag(terminalFileEntryTestTag(row.first.path)),
                    )
                }
            }
        }
    }
}

@Composable
private fun FileTreeRoot(root: String) {
    Text(
        text = root,
        style = JarvisTheme.typography.labelMedium,
        color = JarvisTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.StartEllipsis,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = JarvisTheme.dimens.spacing.s, vertical = JarvisTheme.dimens.spacing.xs)
            .testTag(TerminalFilesRootTestTag),
    )
}

@Composable
private fun FileTreeItem(row: FileTreeRow, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val spacing = JarvisTheme.dimens.spacing
    val iconSize = JarvisTheme.dimens.iconSize.small
    val color = JarvisTheme.colorScheme.onSurfaceVariant

    Row(
        modifier = modifier
            .clip(JarvisTheme.shapes.small)
            .clickable(onClick = onClick)
            .padding(start = spacing.s + spacing.l * row.depth, end = spacing.s, top = spacing.xs, bottom = spacing.xs),
        horizontalArrangement = Arrangement.spacedBy(spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (row.last.isDirectory) {
            Icon(
                imageVector = if (row.expanded) JarvisIcons.ChevronDown else JarvisIcons.ChevronRight,
                contentDescription = null,
                modifier = Modifier.size(iconSize),
                tint = color,
            )
        } else {
            Spacer(modifier = Modifier.size(iconSize))
        }
        Icon(
            imageVector = if (row.last.isDirectory) JarvisIcons.Folder else JarvisIcons.File,
            contentDescription = null,
            modifier = Modifier.size(iconSize),
            tint = color,
        )
        Text(
            text = row.name,
            style = JarvisTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
internal fun TerminalSideBarMessage(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = JarvisTheme.typography.bodySmall,
        color = JarvisTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(JarvisTheme.dimens.spacing.s),
    )
}
