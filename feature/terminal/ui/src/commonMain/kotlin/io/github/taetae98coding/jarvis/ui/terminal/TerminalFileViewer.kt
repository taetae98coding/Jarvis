package io.github.taetae98coding.jarvis.ui.terminal

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.DisableSelection
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import io.github.taetae98coding.jarvis.designsystem.theme.JarvisTheme
import io.github.taetae98coding.jarvis.domain.terminal.DiffedLine
import io.github.taetae98coding.jarvis.domain.terminal.FileContent
import io.github.taetae98coding.jarvis.domain.terminal.GitFileDiff
import io.github.taetae98coding.jarvis.domain.terminal.TerminalTab
import io.github.taetae98coding.jarvis.domain.terminal.diffedLines

const val TerminalFileViewerNoticeTestTag = "terminal:file-viewer:notice"

const val TerminalFileViewerDiffSummaryTestTag = "terminal:file-viewer:diff-summary"

fun terminalFileViewerTestTag(tabId: Long): String = "terminal:file-viewer:${tabId}"

fun terminalFileViewerAddedTestTag(number: Int): String = "terminal:file-viewer:added:${number}"

fun terminalFileViewerRemovedTestTag(oldNumber: Int): String = "terminal:file-viewer:removed:${oldNumber}"

/**
 * 파일 탭. 읽기 전용이고 줄바꿈 없이 가로·세로로 스크롤한다. [content] 가 null 이면 아직 읽는 중이다. [diff] 가 있으면
 * 더한·지운 줄을 내용에 겹친다(docs/common/terminal-file-diff.html).
 */
@Composable
internal fun TerminalFileViewer(
    tab: TerminalTab,
    content: FileContent?,
    diff: GitFileDiff?,
    onFocus: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentOnFocus by rememberUpdatedState(onFocus)
    val changes = diff?.takeIf { it.hunks.isNotEmpty() }

    Box(
        modifier = modifier
            .testTag(terminalFileViewerTestTag(tab.id))
            // 글자 고르기가 누름을 먼저 소비하므로 Initial 단계에서 소비하지 않고 누름만 본다.
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                    currentOnFocus()
                }
            },
    ) {
        when {
            content == null -> Unit
            content == FileContent.Binary -> FileViewerNotice("텍스트가 아닌 파일이라 보일 수 없습니다", Modifier.align(Alignment.Center))
            // 디스크에서 지운 파일은 HEAD 의 줄을 모두 지운 줄로 보인다.
            content == FileContent.Unreadable && changes != null -> Column {
                DiffSummary(changes)
                FileText(lines = emptyList(), diff = changes, modifier = Modifier.fillMaxWidth().weight(1f))
            }
            content == FileContent.Unreadable -> FileViewerNotice("파일을 읽을 수 없습니다", Modifier.align(Alignment.Center))
            content is FileContent.Text -> Column {
                if (content.truncated) FileViewerNotice("앞 512 KiB 만 보입니다", Modifier.padding(JarvisTheme.dimens.spacing.s))
                if (changes != null) DiffSummary(changes)
                val lines = remember(content.text) { content.text.lines() }
                FileText(lines = lines, diff = changes, modifier = Modifier.fillMaxWidth().weight(1f))
            }
        }
    }
}

@Composable
private fun FileViewerNotice(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = JarvisTheme.typography.bodyMedium,
        color = JarvisTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.testTag(TerminalFileViewerNoticeTestTag),
    )
}

@Composable
private fun DiffSummary(diff: GitFileDiff) {
    Text(
        text = "HEAD 대비 +${diff.added} −${diff.removed}",
        style = JarvisTheme.typography.labelMedium,
        color = JarvisTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = JarvisTheme.dimens.spacing.s, vertical = JarvisTheme.dimens.spacing.xs)
            .testTag(TerminalFileViewerDiffSummaryTestTag),
    )
}

@Composable
private fun FileText(lines: List<String>, diff: GitFileDiff?, modifier: Modifier = Modifier) {
    val rows = remember(lines, diff) { diffedLines(lines, diff) }
    val numberWidth = lines.size.toString().length
    val showMarkers = diff != null

    SelectionContainer(modifier = modifier) {
        BoxWithConstraints {
            // 가로 스크롤 안에서는 fillMaxWidth 가 먹지 않는다. 바탕이 보이는 폭 끝까지 칠해지게 최소 폭을 준다.
            val minRowWidth = maxWidth
            LazyColumn(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                contentPadding = PaddingValues(vertical = JarvisTheme.dimens.spacing.s),
            ) {
                items(rows) { row ->
                    FileLine(row = row, numberWidth = numberWidth, showMarker = showMarkers, modifier = Modifier.widthIn(min = minRowWidth))
                }
            }
        }
    }
}

@Composable
private fun FileLine(row: DiffedLine, numberWidth: Int, showMarker: Boolean, modifier: Modifier = Modifier) {
    val style = JarvisTheme.codeTextStyle
    val (number, marker, text) = when (row) {
        is DiffedLine.Current -> Triple(row.number.toString(), if (row.added) "+" else " ", row.text)
        is DiffedLine.Removed -> Triple("", "−", row.text)
    }
    val background = when {
        row is DiffedLine.Removed -> JarvisTheme.colorScheme.errorContainer
        row is DiffedLine.Current && row.added -> JarvisTheme.colors.successContainer
        else -> Color.Transparent
    }
    val tag = when {
        row is DiffedLine.Removed -> terminalFileViewerRemovedTestTag(row.oldNumber)
        row is DiffedLine.Current && row.added -> terminalFileViewerAddedTestTag(row.number)
        else -> null
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(JarvisTheme.dimens.spacing.m),
        modifier = modifier
            .background(background)
            .padding(horizontal = JarvisTheme.dimens.spacing.s)
            .then(if (tag != null) Modifier.testTag(tag) else Modifier),
    ) {
        DisableSelection {
            Row(horizontalArrangement = Arrangement.spacedBy(JarvisTheme.dimens.spacing.s)) {
                Text(text = number.padStart(numberWidth), style = style, color = JarvisTheme.colorScheme.onSurfaceVariant, softWrap = false)
                if (showMarker) Text(text = marker, style = style, color = JarvisTheme.colorScheme.onSurfaceVariant, softWrap = false)
            }
        }
        // 탭 글자는 글꼴마다 폭이 달라 칸이 어긋나므로 공백 네 칸으로 편다.
        Text(text = text.replace("\t", "    "), style = style, softWrap = false)
    }
}
