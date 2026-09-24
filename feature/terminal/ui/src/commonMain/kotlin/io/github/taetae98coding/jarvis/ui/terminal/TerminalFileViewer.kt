package io.github.taetae98coding.jarvis.ui.terminal

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import io.github.taetae98coding.jarvis.designsystem.theme.JarvisTheme
import io.github.taetae98coding.jarvis.domain.terminal.FileContent
import io.github.taetae98coding.jarvis.domain.terminal.TerminalTab

const val TerminalFileViewerNoticeTestTag = "terminal:file-viewer:notice"

fun terminalFileViewerTestTag(tabId: Long): String = "terminal:file-viewer:${tabId}"

/** 파일 탭. 읽기 전용이고 줄바꿈 없이 가로·세로로 스크롤한다. [content] 가 null 이면 아직 읽는 중이다. */
@Composable
internal fun TerminalFileViewer(
    tab: TerminalTab,
    content: FileContent?,
    onFocus: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentOnFocus by rememberUpdatedState(onFocus)

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
        when (content) {
            null -> Unit
            FileContent.Binary -> FileViewerNotice("텍스트가 아닌 파일이라 보일 수 없습니다", Modifier.align(Alignment.Center))
            FileContent.Unreadable -> FileViewerNotice("파일을 읽을 수 없습니다", Modifier.align(Alignment.Center))
            is FileContent.Text -> Column {
                if (content.truncated) FileViewerNotice("앞 512 KiB 만 보입니다", Modifier.padding(JarvisTheme.dimens.spacing.s))
                FileText(content.text, Modifier.fillMaxWidth().weight(1f))
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
private fun FileText(text: String, modifier: Modifier = Modifier) {
    // 탭 글자는 글꼴마다 폭이 달라 칸이 어긋나므로 공백 네 칸으로 편다.
    val lines = remember(text) { text.replace("\t", "    ").lines() }
    val numberWidth = lines.size.toString().length
    val style = JarvisTheme.codeTextStyle

    SelectionContainer(modifier = modifier) {
        LazyColumn(modifier = Modifier.horizontalScroll(rememberScrollState()).padding(JarvisTheme.dimens.spacing.s)) {
            itemsIndexed(lines) { index, line ->
                Row(horizontalArrangement = Arrangement.spacedBy(JarvisTheme.dimens.spacing.m)) {
                    DisableSelection {
                        Text(
                            text = (index + 1).toString().padStart(numberWidth),
                            style = style,
                            color = JarvisTheme.colorScheme.onSurfaceVariant,
                            softWrap = false,
                        )
                    }
                    Text(text = line, style = style, softWrap = false)
                }
            }
        }
    }
}
