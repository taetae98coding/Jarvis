package io.github.taetae98coding.jarvis.ui.terminal

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.DisableSelection
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.isShiftPressed
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import io.github.taetae98coding.jarvis.designsystem.component.JarvisIconButton
import io.github.taetae98coding.jarvis.designsystem.icon.JarvisIcons
import io.github.taetae98coding.jarvis.designsystem.theme.JarvisTheme
import io.github.taetae98coding.jarvis.domain.terminal.DiffedLine
import io.github.taetae98coding.jarvis.domain.terminal.FileContent
import io.github.taetae98coding.jarvis.domain.terminal.GitFileDiff
import io.github.taetae98coding.jarvis.domain.terminal.LineComment
import io.github.taetae98coding.jarvis.domain.terminal.TerminalTab
import io.github.taetae98coding.jarvis.domain.terminal.diffedLines
import io.github.taetae98coding.jarvis.domain.terminal.sameLine

const val TerminalFileViewerNoticeTestTag = "terminal:file-viewer:notice"

const val TerminalFileViewerDiffSummaryTestTag = "terminal:file-viewer:diff-summary"

const val TerminalFileViewerCommentFieldTestTag = "terminal:file-viewer:comment-field"

const val TerminalFileViewerCommentAddTestTag = "terminal:file-viewer:comment-add"

const val TerminalFileViewerCommentCancelTestTag = "terminal:file-viewer:comment-cancel"

const val TerminalFileViewerCommentBarTestTag = "terminal:file-viewer:comment-bar"

const val TerminalFileViewerCommentClearTestTag = "terminal:file-viewer:comment-clear"

const val TerminalFileViewerCommentSendTestTag = "terminal:file-viewer:comment-send"

fun terminalFileViewerTestTag(tabId: Long): String = "terminal:file-viewer:${tabId}"

fun terminalFileViewerAddedTestTag(number: Int): String = "terminal:file-viewer:added:${number}"

fun terminalFileViewerRemovedTestTag(oldNumber: Int): String = "terminal:file-viewer:removed:${oldNumber}"

fun terminalFileViewerGutterTestTag(number: Int): String = "terminal:file-viewer:gutter:${number}"

fun terminalFileViewerRemovedGutterTestTag(oldNumber: Int): String = "terminal:file-viewer:gutter:removed:${oldNumber}"

fun terminalFileViewerCommentTestTag(id: Long): String = "terminal:file-viewer:comment:${id}"

fun terminalFileViewerCommentDeleteTestTag(id: Long): String = "terminal:file-viewer:comment-delete:${id}"

fun terminalFileViewerCommentTargetTestTag(tabId: Long): String = "terminal:file-viewer:comment-target:${tabId}"

/** 코멘트를 보낼 수 있는 Claude 탭 하나. */
internal data class ClaudeTabChoice(val tabId: Long, val name: String)

/**
 * 파일 탭의 줄 코멘트(docs/common/terminal-line-comment.html). [comments] 는 이 파일의 것, [total] 은 패널 전체 수다.
 * [onSend] 의 null 은 새 Claude 탭이다.
 */
internal class FileLineComments(
    val comments: List<LineComment>,
    val total: Int,
    val sending: Boolean,
    val failed: Boolean,
    val targets: List<ClaudeTabChoice>,
    val onAdd: (List<DiffedLine>, String) -> Unit,
    val onRemove: (Long) -> Unit,
    val onClear: () -> Unit,
    val onSend: (Long?) -> Unit,
)

/**
 * 파일 탭. 읽기 전용이고 줄바꿈 없이 가로·세로로 스크롤한다. [content] 가 null 이면 아직 읽는 중이다. [diff] 가 있으면
 * 더한·지운 줄을 내용에 겹치고 요약을 "[diffLabel] +n −m" 으로 붙인다(docs/common/terminal-file-diff.html, 커밋 파일 탭은
 * docs/common/terminal-commit-file.html). [lineComments] 가 null 이면 줄 코멘트를 남길 수 없다.
 */
@Composable
internal fun TerminalFileViewer(
    tab: TerminalTab,
    content: FileContent?,
    diff: GitFileDiff?,
    diffLabel: String,
    lineComments: FileLineComments?,
    onFocus: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentOnFocus by rememberUpdatedState(onFocus)
    val changes = diff?.takeIf { it.hunks.isNotEmpty() }

    Column(
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
        Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
            when {
                content == null -> Unit
                content == FileContent.Binary -> FileViewerNotice("텍스트가 아닌 파일이라 보일 수 없습니다", Modifier.align(Alignment.Center))
                // 디스크에서 지운 파일은 HEAD 의 줄을 모두 지운 줄로 보인다.
                content == FileContent.Unreadable && changes != null -> Column {
                    DiffSummary(changes, diffLabel)
                    FileText(lines = emptyList(), diff = changes, lineComments = lineComments, modifier = Modifier.fillMaxWidth().weight(1f))
                }
                content == FileContent.Unreadable -> FileViewerNotice("파일을 읽을 수 없습니다", Modifier.align(Alignment.Center))
                content is FileContent.Text -> Column {
                    if (content.truncated) FileViewerNotice("앞 512 KiB 만 보입니다", Modifier.padding(JarvisTheme.dimens.spacing.s))
                    if (changes != null) DiffSummary(changes, diffLabel)
                    val lines = remember(content.text) { content.text.lines() }
                    FileText(lines = lines, diff = changes, lineComments = lineComments, modifier = Modifier.fillMaxWidth().weight(1f))
                }
            }
        }

        if (lineComments != null && lineComments.total > 0) LineCommentBar(lineComments)
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
private fun DiffSummary(diff: GitFileDiff, label: String) {
    Text(
        text = "$label +${diff.added} −${diff.removed}",
        style = JarvisTheme.typography.labelMedium,
        color = JarvisTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = JarvisTheme.dimens.spacing.s, vertical = JarvisTheme.dimens.spacing.xs)
            .testTag(TerminalFileViewerDiffSummaryTestTag),
    )
}

/** 고르는 중인 줄. [anchor] 는 처음 누른 줄, [extent] 는 Shift 로 넓힌 끝이다. */
private data class LineSelection(val anchor: DiffedLine, val extent: DiffedLine)

private sealed interface FileItem {
    val key: String

    data class Line(val index: Int, val row: DiffedLine, val selected: Boolean, val commented: Boolean) : FileItem {
        override val key: String get() = "line:$index"
    }

    data class Comment(val comment: LineComment) : FileItem {
        override val key: String get() = "comment:${comment.id}"
    }

    data object Draft : FileItem {
        override val key: String get() = "draft"
    }
}

@Composable
private fun FileText(lines: List<String>, diff: GitFileDiff?, lineComments: FileLineComments?, modifier: Modifier = Modifier) {
    val rows = remember(lines, diff) { diffedLines(lines, diff) }
    val numberWidth = lines.size.toString().length
    val showMarkers = diff != null
    val horizontalScroll = rememberScrollState()

    var selection by remember { mutableStateOf<LineSelection?>(null) }
    var draft by remember { mutableStateOf("") }
    // 파일이 바뀌어 고른 줄이 사라졌으면 고르지 않은 것으로 본다.
    val selectedRange = selection?.let { rows.rangeOf(it.anchor, it.extent) }

    val items = remember(rows, selectedRange, lineComments?.comments) { fileItems(rows, selectedRange, lineComments?.comments.orEmpty()) }

    val onGutter: ((DiffedLine, Boolean) -> Unit)? = lineComments?.let {
        { row, shift ->
            val current = selection
            selection = if (shift && current != null) current.copy(extent = row) else LineSelection(row, row)
        }
    }

    SelectionContainer(modifier = modifier) {
        BoxWithConstraints {
            // 가로 스크롤 안에서는 fillMaxWidth 가 먹지 않는다. 바탕이 보이는 폭 끝까지 칠해지게 최소 폭을 준다.
            val visibleWidth = maxWidth
            LazyColumn(
                modifier = Modifier.horizontalScroll(horizontalScroll),
                contentPadding = PaddingValues(vertical = JarvisTheme.dimens.spacing.s),
            ) {
                items(count = items.size, key = { items[it].key }) { index ->
                    when (val item = items[index]) {
                        is FileItem.Line -> FileLine(
                            row = item.row,
                            numberWidth = numberWidth,
                            showMarker = showMarkers,
                            selected = item.selected,
                            commented = item.commented,
                            onGutter = onGutter?.let { { shift: Boolean -> it(item.row, shift) } },
                            modifier = Modifier.widthIn(min = visibleWidth),
                        )

                        is FileItem.Comment -> PinnedToViewport(horizontalScroll, visibleWidth) {
                            LineCommentCard(comment = item.comment, onRemove = { lineComments?.onRemove(item.comment.id) })
                        }

                        FileItem.Draft -> PinnedToViewport(horizontalScroll, visibleWidth) {
                            LineCommentEditor(
                                text = draft,
                                onTextChange = { draft = it },
                                onAdd = {
                                    val range = selectedRange ?: return@LineCommentEditor
                                    lineComments?.onAdd(rows.subList(range.first, range.last + 1).toList(), draft)
                                    selection = null
                                    draft = ""
                                },
                                onCancel = {
                                    selection = null
                                    draft = ""
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

/** [anchor] 에서 [extent] 까지(순서 무관) 지금 줄 목록의 위치. 둘 중 하나라도 없으면 null. */
private fun List<DiffedLine>.rangeOf(anchor: DiffedLine, extent: DiffedLine): IntRange? {
    val start = indexOfFirst { it.sameLine(anchor) }.takeIf { it >= 0 } ?: return null
    val end = indexOfFirst { it.sameLine(extent) }.takeIf { it >= 0 } ?: return null

    return minOf(start, end)..maxOf(start, end)
}

/** 줄 사이에 카드·입력 칸을 끼운다. 붙을 줄을 찾지 못한 카드는 끝에 모은다(C11). */
private fun fileItems(rows: List<DiffedLine>, selected: IntRange?, comments: List<LineComment>): List<FileItem> {
    val after = mutableMapOf<Int, MutableList<LineComment>>()
    val orphans = mutableListOf<LineComment>()
    val commented = mutableSetOf<Int>()

    comments.forEach { comment ->
        val end = rows.indexOfFirst { it.sameLine(comment.lines.last()) }
        if (end < 0) {
            orphans += comment
            return@forEach
        }

        val start = rows.indexOfFirst { it.sameLine(comment.lines.first()) }.takeIf { it in 0..end } ?: end
        commented += start..end
        after.getOrPut(end) { mutableListOf() } += comment
    }

    return buildList {
        rows.forEachIndexed { index, row ->
            add(FileItem.Line(index, row, selected = selected != null && index in selected, commented = index in commented))
            after[index]?.forEach { add(FileItem.Comment(it)) }
            if (selected?.last == index) add(FileItem.Draft)
        }
        orphans.forEach { add(FileItem.Comment(it)) }
    }
}

/** 가로로 스크롤해도 보이는 영역 왼쪽에 붙어 있게 스크롤한 만큼 되민다. */
@Composable
private fun PinnedToViewport(scroll: ScrollState, width: Dp, content: @Composable () -> Unit) {
    DisableSelection {
        Box(
            modifier = Modifier
                .offset { IntOffset(scroll.value, 0) }
                .width(width)
                .padding(horizontal = JarvisTheme.dimens.spacing.s, vertical = JarvisTheme.dimens.spacing.xs),
        ) {
            content()
        }
    }
}

@Composable
private fun FileLine(
    row: DiffedLine,
    numberWidth: Int,
    showMarker: Boolean,
    selected: Boolean,
    commented: Boolean,
    onGutter: ((shift: Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val style = JarvisTheme.codeTextStyle
    val (number, marker, text) = when (row) {
        is DiffedLine.Current -> Triple(row.number.toString(), if (row.added) "+" else " ", row.text)
        is DiffedLine.Removed -> Triple("", "−", row.text)
    }
    val background = when {
        selected -> JarvisTheme.colorScheme.primaryContainer
        row is DiffedLine.Removed -> JarvisTheme.colorScheme.errorContainer
        row is DiffedLine.Current && row.added -> JarvisTheme.colors.successContainer
        else -> Color.Transparent
    }
    val tag = when {
        row is DiffedLine.Removed -> terminalFileViewerRemovedTestTag(row.oldNumber)
        row is DiffedLine.Current && row.added -> terminalFileViewerAddedTestTag(row.number)
        else -> null
    }
    val gutterTag = when (row) {
        is DiffedLine.Current -> terminalFileViewerGutterTestTag(row.number)
        is DiffedLine.Removed -> terminalFileViewerRemovedGutterTestTag(row.oldNumber)
    }
    val numberColor = if (commented) JarvisTheme.colorScheme.primary else JarvisTheme.colorScheme.onSurfaceVariant

    Row(
        horizontalArrangement = Arrangement.spacedBy(JarvisTheme.dimens.spacing.m),
        modifier = modifier
            .background(background)
            .padding(horizontal = JarvisTheme.dimens.spacing.s)
            .then(if (tag != null) Modifier.testTag(tag) else Modifier),
    ) {
        DisableSelection {
            Row(
                horizontalArrangement = Arrangement.spacedBy(JarvisTheme.dimens.spacing.s),
                modifier = Modifier.testTag(gutterTag).then(if (onGutter != null) Modifier.gutterClick(onGutter) else Modifier),
            ) {
                Text(text = number.padStart(numberWidth), style = style, color = numberColor, softWrap = false)
                if (showMarker) Text(text = marker, style = style, color = numberColor, softWrap = false)
            }
        }
        // 탭 글자는 글꼴마다 폭이 달라 칸이 어긋나므로 공백 네 칸으로 편다.
        Text(text = text.replace("\t", "    "), style = style, softWrap = false)
    }
}

// clickable 은 누를 때의 Shift 를 알려 주지 않는다. 누름을 소비해 줄 번호 칸에서 글자 고르기가 시작되지 않게도 한다.
private fun Modifier.gutterClick(onClick: (shift: Boolean) -> Unit): Modifier =
    pointerHoverIcon(PointerIcon.Hand).pointerInput(onClick) {
        awaitEachGesture {
            val down = awaitFirstDown()
            down.consume()
            val shift = currentEvent.keyboardModifiers.isShiftPressed
            val up = waitForUpOrCancellation() ?: return@awaitEachGesture
            up.consume()
            onClick(shift)
        }
    }

@Composable
private fun LineCommentEditor(text: String, onTextChange: (String) -> Unit, onAdd: () -> Unit, onCancel: () -> Unit) {
    val focusRequester = remember { FocusRequester() }
    val canAdd = text.isNotBlank()

    LaunchedEffect(focusRequester) { focusRequester.requestFocus() }

    Column(verticalArrangement = Arrangement.spacedBy(JarvisTheme.dimens.spacing.xs)) {
        OutlinedTextField(
            value = text,
            onValueChange = onTextChange,
            placeholder = { Text("코멘트") },
            minLines = 2,
            textStyle = JarvisTheme.typography.bodyMedium,
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester)
                .onPreviewKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                    when {
                        event.key == Key.Escape -> onCancel()
                        event.key == Key.Enter && (event.isMetaPressed || event.isCtrlPressed) -> if (canAdd) onAdd()
                        else -> return@onPreviewKeyEvent false
                    }
                    true
                }
                .testTag(TerminalFileViewerCommentFieldTestTag),
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(JarvisTheme.dimens.spacing.s, Alignment.End)) {
            TextButton(onClick = onCancel, modifier = Modifier.testTag(TerminalFileViewerCommentCancelTestTag)) { Text("취소") }
            Button(onClick = onAdd, enabled = canAdd, modifier = Modifier.testTag(TerminalFileViewerCommentAddTestTag)) { Text("코멘트 추가") }
        }
    }
}

@Composable
private fun LineCommentCard(comment: LineComment, onRemove: () -> Unit) {
    Surface(
        color = JarvisTheme.colorScheme.surfaceContainerHigh,
        shape = JarvisTheme.shapes.small,
        modifier = Modifier.fillMaxWidth().testTag(terminalFileViewerCommentTestTag(comment.id)),
    ) {
        Row(verticalAlignment = Alignment.Top, modifier = Modifier.padding(start = JarvisTheme.dimens.spacing.m)) {
            Column(
                modifier = Modifier.weight(1f).padding(vertical = JarvisTheme.dimens.spacing.s),
                verticalArrangement = Arrangement.spacedBy(JarvisTheme.dimens.spacing.xs),
            ) {
                Text(text = comment.rangeLabel, style = JarvisTheme.typography.labelMedium, color = JarvisTheme.colorScheme.onSurfaceVariant)
                Text(text = comment.body, style = JarvisTheme.typography.bodyMedium)
            }
            JarvisIconButton(
                icon = JarvisIcons.Close,
                contentDescription = "코멘트 지우기",
                onClick = onRemove,
                modifier = Modifier.testTag(terminalFileViewerCommentDeleteTestTag(comment.id)),
            )
        }
    }
}

@Composable
private fun LineCommentBar(lineComments: FileLineComments) {
    var menuExpanded by remember { mutableStateOf(false) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(JarvisTheme.dimens.spacing.s),
        modifier = Modifier
            .fillMaxWidth()
            .background(JarvisTheme.colorScheme.surfaceContainer)
            .padding(horizontal = JarvisTheme.dimens.spacing.m, vertical = JarvisTheme.dimens.spacing.xs)
            .testTag(TerminalFileViewerCommentBarTestTag),
    ) {
        Text(text = "코멘트 ${lineComments.total}개", style = JarvisTheme.typography.labelLarge)
        if (lineComments.failed) {
            Text(
                text = "Claude 가 준비되지 않아 보내지 못했습니다",
                style = JarvisTheme.typography.bodySmall,
                color = JarvisTheme.colorScheme.error,
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        TextButton(
            onClick = lineComments.onClear,
            enabled = !lineComments.sending,
            modifier = Modifier.testTag(TerminalFileViewerCommentClearTestTag),
        ) {
            Text("모두 지우기")
        }
        Box {
            Button(
                onClick = {
                    if (lineComments.targets.size > 1) menuExpanded = true else lineComments.onSend(lineComments.targets.firstOrNull()?.tabId)
                },
                enabled = !lineComments.sending,
                modifier = Modifier.testTag(TerminalFileViewerCommentSendTestTag),
            ) {
                Text(
                    when {
                        lineComments.sending -> "보내는 중…"
                        lineComments.targets.isEmpty() -> "새 Claude 탭에 보내기"
                        else -> "Claude 에 보내기"
                    },
                )
            }
            DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                lineComments.targets.forEach { target ->
                    DropdownMenuItem(
                        text = { Text(target.name) },
                        leadingIcon = { Icon(imageVector = JarvisIcons.Claude, contentDescription = null) },
                        onClick = {
                            menuExpanded = false
                            lineComments.onSend(target.tabId)
                        },
                        modifier = Modifier.testTag(terminalFileViewerCommentTargetTestTag(target.tabId)),
                    )
                }
            }
        }
    }
}
