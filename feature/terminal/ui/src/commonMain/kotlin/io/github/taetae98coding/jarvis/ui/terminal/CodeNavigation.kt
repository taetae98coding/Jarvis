package io.github.taetae98coding.jarvis.ui.terminal

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import io.github.taetae98coding.jarvis.designsystem.theme.JarvisTheme
import io.github.taetae98coding.jarvis.domain.terminal.CodeAnalysisStatus
import io.github.taetae98coding.jarvis.domain.terminal.CodeCompletion
import io.github.taetae98coding.jarvis.domain.terminal.CodeCompletionKind
import io.github.taetae98coding.jarvis.domain.terminal.CodeCompletions
import io.github.taetae98coding.jarvis.domain.terminal.CodeEdit
import io.github.taetae98coding.jarvis.domain.terminal.CodeLanguage
import io.github.taetae98coding.jarvis.domain.terminal.CodeLocation
import io.github.taetae98coding.jarvis.domain.terminal.CodeNavigation
import io.github.taetae98coding.jarvis.domain.terminal.CodeNavigationKind
import io.github.taetae98coding.jarvis.domain.terminal.CodeSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

const val TerminalFileViewerCodeStatusTestTag = "terminal:file-viewer:code-status"

const val TerminalFileViewerCompletionTestTag = "terminal:file-viewer:completion"

const val TerminalFileViewerLocationsTestTag = "terminal:file-viewer:locations"

const val TerminalFileViewerRevealedTestTag = "terminal:file-viewer:revealed"

fun terminalFileViewerCompletionTestTag(index: Int): String = "terminal:file-viewer:completion:${index}"

fun terminalFileViewerLocationTestTag(index: Int): String = "terminal:file-viewer:location:${index}"

internal val CompletionDebounce: Duration = 120.milliseconds

internal val RevealHighlight: Duration = 1.5.seconds

private val NoticeDuration: Duration = 3.seconds

private val CompletionRowHeight = 24.dp

private const val CompletionVisibleRows = 8

private const val LocationVisibleRows = 12

/** 이동 요청(G4). 같은 줄로 두 번 가도 다시 움직이게 [id] 로 가른다. */
internal data class CodeReveal(val id: Long, val line: Int, val column: Int)

/**
 * 코드 파일 탭의 자동완성·선언·사용처(docs/common/terminal-code-navigation.html). 코드 파일이 아니거나 커밋 파일 탭이면 이
 * 값이 없다. 요청은 모두 지금 글과 위치를 받는다.
 */
internal class FileCode(
    val language: CodeLanguage,
    val status: CodeAnalysisStatus?,
    val complete: suspend (text: String, offset: Int) -> CodeCompletions,
    val applyCompletion: suspend (text: String, offset: Int, item: CodeCompletion) -> CodeEdit,
    val goToDeclaration: suspend (text: String, offset: Int) -> CodeNavigation?,
    val findUsages: suspend (text: String, offset: Int) -> CodeNavigation?,
    val onOpen: (CodeLocation) -> Unit,
)

/** 도구 줄의 분석 상태(N4). 준비됐거나 코드 분석이 없는 플랫폼이면 null. */
internal fun codeStatusText(language: CodeLanguage, status: CodeAnalysisStatus?): String? =
    when (status) {
        is CodeAnalysisStatus.Starting -> "${language.label} 분석 준비 중…" + (status.percent?.let { " $it%" } ?: "")
        is CodeAnalysisStatus.Failed -> "텍스트 검색만 — ${status.reason}"
        is CodeAnalysisStatus.Unavailable -> status.reason?.let { "텍스트 검색만 — $it" }
        CodeAnalysisStatus.Ready, null -> null
    }

/** 선언·사용처 찾기(G1–G7). 새로 부르면 앞의 찾기를 버린다. */
@Stable
internal class CodeNavigator(private val scope: CoroutineScope) {
    /** 화면이 다시 그려질 때마다 최신 값으로 바꾼다. */
    var code: FileCode? = null

    var searching by mutableStateOf(false)
        private set

    var notice by mutableStateOf<String?>(null)
        private set

    var results by mutableStateOf<CodeNavigation?>(null)
        private set

    private var job: Job? = null

    private var noticeJob: Job? = null

    fun goToDeclaration(text: String, offset: Int) = run { code?.goToDeclaration(text, offset) }

    fun findUsages(text: String, offset: Int) = run { code?.findUsages(text, offset) }

    fun open(location: CodeLocation) {
        results = null
        code?.onOpen(location)
    }

    fun dismiss() {
        results = null
    }

    private fun run(request: suspend () -> CodeNavigation?) {
        job?.cancel()
        results = null
        job = scope.launch {
            searching = true
            val navigation = try {
                request()
            } finally {
                searching = false
            }
            navigation ?: return@launch
            val locations = navigation.result.locations
            when {
                locations.isEmpty() -> show(
                    when {
                        navigation.result.libraryOnly -> "라이브러리 안의 선언이라 열 수 없습니다"
                        navigation.kind == CodeNavigationKind.Declaration -> "선언을 찾지 못했습니다"
                        else -> "사용하는 곳이 없습니다"
                    },
                )
                locations.size == 1 -> code?.onOpen(locations.single())
                else -> results = navigation
            }
        }
    }

    private fun show(text: String) {
        noticeJob?.cancel()
        notice = text
        noticeJob = scope.launch {
            delay(NoticeDuration)
            notice = null
        }
    }
}

@Composable
internal fun rememberCodeNavigator(code: FileCode): CodeNavigator {
    val scope = rememberCoroutineScope()

    return remember(scope) { CodeNavigator(scope) }.also { it.code = code }
}

/** 도구 줄에 보일 글. 찾기 알림이 분석 상태보다 먼저다. */
internal fun CodeNavigator.statusText(code: FileCode): String? =
    notice ?: "찾는 중…".takeIf { searching } ?: codeStatusText(code.language, code.status)

/** 자동완성 목록의 상태(C1–C5). [start] 는 바꿀 접두사의 시작이다. */
internal data class CompletionState(
    val start: Int,
    val items: List<CodeCompletion>,
    val source: CodeSource,
    val selected: Int = 0,
)

/** 커서 아래의 자동완성 목록(C2). 입력 칸의 포커스를 뺏지 않는다. */
@Composable
internal fun CompletionPopup(state: CompletionState, offset: IntOffset, onPick: (CodeCompletion) -> Unit, onDismiss: () -> Unit) {
    val listState = rememberLazyListState()
    LaunchedEffect(state.selected) {
        val visible = listState.layoutInfo.visibleItemsInfo
        if (visible.isNotEmpty() && (state.selected < visible.first().index || state.selected > visible.last().index)) {
            listState.scrollToItem((state.selected - CompletionVisibleRows + 1).coerceAtLeast(0).coerceAtMost(state.selected))
        }
    }

    Popup(offset = offset, onDismissRequest = onDismiss, properties = PopupProperties(focusable = false)) {
        Surface(
            color = JarvisTheme.colorScheme.surfaceContainerHigh,
            shape = JarvisTheme.shapes.small,
            shadowElevation = 4.dp,
            modifier = Modifier.widthIn(min = 320.dp, max = 560.dp).testTag(TerminalFileViewerCompletionTestTag),
        ) {
            Column {
                if (state.source == CodeSource.TextSearch) {
                    Text(
                        text = "텍스트",
                        style = JarvisTheme.typography.labelSmall,
                        color = JarvisTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = JarvisTheme.dimens.spacing.s, vertical = JarvisTheme.dimens.spacing.xs),
                    )
                }
                LazyColumn(state = listState, modifier = Modifier.heightIn(max = CompletionRowHeight * CompletionVisibleRows)) {
                    items(count = state.items.size) { index ->
                        CompletionRow(item = state.items[index], selected = index == state.selected, onClick = { onPick(state.items[index]) }, modifier = Modifier.testTag(terminalFileViewerCompletionTestTag(index)))
                    }
                }
            }
        }
    }
}

@Composable
private fun CompletionRow(item: CodeCompletion, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val style = JarvisTheme.codeTextStyle
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(JarvisTheme.dimens.spacing.s),
        modifier = modifier
            .fillMaxWidth()
            .height(CompletionRowHeight)
            .background(if (selected) JarvisTheme.colorScheme.primaryContainer else JarvisTheme.colorScheme.surfaceContainerHigh)
            .clickable(onClick = onClick)
            .padding(horizontal = JarvisTheme.dimens.spacing.s),
    ) {
        Text(
            text = kindGlyph(item.kind),
            style = style,
            color = JarvisTheme.colorScheme.primary,
            modifier = Modifier.width(12.dp),
        )
        Text(text = item.label, style = style, maxLines = 1)
        item.signature?.let {
            Text(text = it, style = style, color = JarvisTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
        }
        Spacer(modifier = Modifier.weight(1f))
        item.detail?.let {
            Text(text = it, style = style, color = JarvisTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.widthIn(max = 200.dp))
        }
    }
}

private fun kindGlyph(kind: CodeCompletionKind): String =
    when (kind) {
        CodeCompletionKind.Function -> "f"
        CodeCompletionKind.Variable -> "v"
        CodeCompletionKind.Type -> "T"
        CodeCompletionKind.Keyword -> "k"
        CodeCompletionKind.Other -> "·"
    }

/** 여러 곳으로 갈 수 있을 때의 위치 목록 창(G3). 포커스를 가져가 ↑·↓·Enter·Esc 를 받는다. */
@Composable
internal fun CodeLocationsPopup(navigation: CodeNavigation, onOpen: (CodeLocation) -> Unit, onDismiss: () -> Unit) {
    val locations = navigation.result.locations
    var selected by remember(navigation) { mutableStateOf(0) }
    val focusRequester = remember { FocusRequester() }
    val listState = rememberLazyListState()
    val title = when (navigation.kind) {
        CodeNavigationKind.Declaration -> "선언 ${locations.size}곳"
        CodeNavigationKind.Usages -> "사용하는 곳 ${locations.size}곳"
    } + if (navigation.result.source == CodeSource.TextSearch) " · 텍스트 검색" else ""

    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    LaunchedEffect(selected) { listState.animateScrollToItem((selected - LocationVisibleRows / 2).coerceAtLeast(0)) }

    Popup(alignment = Alignment.TopCenter, offset = IntOffset(0, 48), onDismissRequest = onDismiss, properties = PopupProperties(focusable = true)) {
        Surface(
            color = JarvisTheme.colorScheme.surfaceContainerHigh,
            shape = JarvisTheme.shapes.small,
            shadowElevation = 6.dp,
            modifier = Modifier
                .width(560.dp)
                .focusRequester(focusRequester)
                .focusable()
                .onPreviewKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                    when (event.key) {
                        Key.DirectionDown -> selected = (selected + 1).coerceAtMost(locations.lastIndex)
                        Key.DirectionUp -> selected = (selected - 1).coerceAtLeast(0)
                        Key.Enter, Key.NumPadEnter -> onOpen(locations[selected])
                        Key.Escape -> onDismiss()
                        else -> return@onPreviewKeyEvent false
                    }
                    true
                }
                .testTag(TerminalFileViewerLocationsTestTag),
        ) {
            Column(modifier = Modifier.padding(vertical = JarvisTheme.dimens.spacing.xs)) {
                Text(
                    text = title,
                    style = JarvisTheme.typography.titleSmall,
                    modifier = Modifier.padding(horizontal = JarvisTheme.dimens.spacing.m, vertical = JarvisTheme.dimens.spacing.s),
                )
                LazyColumn(state = listState, modifier = Modifier.heightIn(max = CompletionRowHeight * LocationVisibleRows)) {
                    items(count = locations.size) { index ->
                        val location = locations[index]
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(JarvisTheme.dimens.spacing.m),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(CompletionRowHeight)
                                .background(if (index == selected) JarvisTheme.colorScheme.primaryContainer else JarvisTheme.colorScheme.surfaceContainerHigh)
                                .clickable { onOpen(location) }
                                .padding(horizontal = JarvisTheme.dimens.spacing.m)
                                .testTag(terminalFileViewerLocationTestTag(index)),
                        ) {
                            Text(
                                text = "${location.path.substringAfterLast('/')}:${location.line + 1}",
                                style = JarvisTheme.typography.labelMedium,
                                color = JarvisTheme.colorScheme.primary,
                                maxLines = 1,
                            )
                            Text(
                                text = location.lineText.trim(),
                                style = JarvisTheme.codeTextStyle,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }
    }
}

/** 읽기 보기는 탭을 공백 네 칸으로 펴서 그린다(highlightedLine). 그려진 위치를 원래 줄의 칸으로 되돌린다. */
internal fun sourceColumn(line: String, displayOffset: Int): Int {
    var display = 0
    line.forEachIndexed { index, char ->
        val width = if (char == '\t') 4 else 1
        if (displayOffset < display + width) return index
        display += width
    }

    return line.length
}
