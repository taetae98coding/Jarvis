package io.github.taetae98coding.jarvis.ui.terminal

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import io.github.taetae98coding.jarvis.domain.terminal.PaneNode
import io.github.taetae98coding.jarvis.domain.terminal.SplitDirection
import io.github.taetae98coding.jarvis.domain.terminal.TerminalTab
import io.github.taetae98coding.jarvis.domain.terminal.TerminalWorkspace
import kotlinx.coroutines.flow.StateFlow

const val TerminalScreenTestTag = "terminal:screen"
const val TerminalSplitSideTestTag = "terminal:split-side"
const val TerminalSplitStackedTestTag = "terminal:split-stacked"
const val TerminalNewTabTestTag = "terminal:new-tab"
const val TerminalCloseTestTag = "terminal:close"

fun terminalTabTestTag(id: Long): String = "terminal:tab:$id"

fun terminalTabCloseTestTag(id: Long): String = "terminal:tab-close:$id"

@Composable
internal fun TerminalScreen(
    viewModel: TerminalViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val workspace by viewModel.workspace.collectAsState()

    // iTerm 은 마지막 탭이 닫히면 창을 닫는다. 이 앱에서 창에 해당하는 것이 이 화면이다.
    LaunchedEffect(workspace.tabs.isEmpty()) {
        if (workspace.tabs.isEmpty()) onBack()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag(TerminalScreenTestTag)
            // 루트에서 먼저 가로채야 포커스된 패널의 입력 필드보다 앞선다.
            .onPreviewKeyEvent { event -> onShortcut(event, viewModel) },
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        TerminalTopBar(
            onBack = onBack,
            onSplitSideBySide = viewModel::splitSideBySide,
            onSplitStacked = viewModel::splitStacked,
            onNewTab = viewModel::addTab,
            onClose = viewModel::closeFocusedPane,
        )

        TerminalTabRow(
            workspace = workspace,
            titleOf = { paneId -> viewModel.pane(paneId)?.title },
            onSelect = viewModel::selectTab,
            onClose = viewModel::closeTab,
        )

        val tab = workspace.selectedTab ?: return@Column

        key(tab.id) {
            PaneTree(
                node = tab.root,
                tab = tab,
                viewModel = viewModel,
                modifier = Modifier.fillMaxWidth().weight(1f),
            )
        }
    }
}

private fun onShortcut(event: KeyEvent, viewModel: TerminalViewModel): Boolean {
    if (event.type != KeyEventType.KeyDown || !event.isMetaPressed) return false

    when (event.key) {
        Key.D -> if (event.isShiftPressed) viewModel.splitStacked() else viewModel.splitSideBySide()
        Key.T -> viewModel.addTab()
        Key.W -> viewModel.closeFocusedPane()
        Key.RightBracket -> if (event.isShiftPressed) viewModel.selectAdjacentTab(1) else viewModel.focusAdjacentPane(1)
        Key.LeftBracket -> if (event.isShiftPressed) viewModel.selectAdjacentTab(-1) else viewModel.focusAdjacentPane(-1)
        else -> {
            val index = TabNumberKeys.indexOf(event.key)
            if (index < 0) return false

            viewModel.selectTabAt(index)
        }
    }

    return true
}

private val TabNumberKeys = listOf(Key.One, Key.Two, Key.Three, Key.Four, Key.Five, Key.Six, Key.Seven, Key.Eight, Key.Nine)

// Material3 의 TopAppBar 는 실험 API 라 버전을 올릴 때마다 시그니처가 흔들린다. 버튼만 필요해서 직접 놓는다.
@Composable
private fun TerminalTopBar(
    onBack: () -> Unit,
    onSplitSideBySide: () -> Unit,
    onSplitStacked: () -> Unit,
    onNewTab: () -> Unit,
    onClose: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextButton(onClick = onBack) { Text(text = "← 뒤로") }

        Text(
            text = "터미널",
            style = MaterialTheme.typography.titleLarge,
        )

        // 좁은 화면에서는 버튼 넷이 한 줄에 들어가지 않는다. 줄을 늘리는 대신 옆으로 민다.
        Row(
            modifier = Modifier.weight(1f).horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.End,
        ) {
            TextButton(onClick = onSplitSideBySide, modifier = Modifier.testTag(TerminalSplitSideTestTag)) {
                Text(text = "좌우 분할")
            }
            TextButton(onClick = onSplitStacked, modifier = Modifier.testTag(TerminalSplitStackedTestTag)) {
                Text(text = "상하 분할")
            }
            TextButton(onClick = onNewTab, modifier = Modifier.testTag(TerminalNewTabTestTag)) {
                Text(text = "새 탭")
            }
            TextButton(onClick = onClose, modifier = Modifier.testTag(TerminalCloseTestTag)) {
                Text(text = "닫기")
            }
        }
    }
}

@Composable
private fun TerminalTabRow(
    workspace: TerminalWorkspace,
    titleOf: (Long) -> StateFlow<String?>?,
    onSelect: (Long) -> Unit,
    onClose: (Long) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        workspace.tabs.forEachIndexed { index, tab ->
            TerminalTabChip(
                title = tabTitle(titleOf(tab.focusedPaneId), index),
                selected = tab.id == workspace.selectedTabId,
                onSelect = { onSelect(tab.id) },
                onClose = { onClose(tab.id) },
                modifier = Modifier.testTag(terminalTabTestTag(tab.id)),
                closeModifier = Modifier.testTag(terminalTabCloseTestTag(tab.id)),
            )
        }
    }
}

@Composable
private fun tabTitle(source: StateFlow<String?>?, index: Int): String {
    val title = source?.collectAsState()?.value

    return title?.takeIf { it.isNotBlank() } ?: "셸 ${index + 1}"
}

@Composable
private fun TerminalTabChip(
    title: String,
    selected: Boolean,
    onSelect: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    closeModifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onSelect)
                    .padding(start = 12.dp, end = 4.dp, top = 8.dp, bottom = 8.dp)
                    .width(120.dp),
            )

            Text(
                text = "×",
                style = MaterialTheme.typography.labelLarge,
                modifier = closeModifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onClose)
                    .padding(horizontal = 8.dp, vertical = 8.dp),
            )
        }
    }
}

@Composable
private fun PaneTree(
    node: PaneNode,
    tab: TerminalTab,
    viewModel: TerminalViewModel,
    modifier: Modifier = Modifier,
) {
    when (node) {
        is PaneNode.Leaf -> {
            val pane = viewModel.pane(node.paneId) ?: return

            key(node.paneId) {
                TerminalPane(
                    state = pane,
                    focused = node.paneId == tab.focusedPaneId,
                    showFocusBorder = tab.root is PaneNode.Split,
                    onFocus = { viewModel.focusPane(node.paneId) },
                    modifier = modifier,
                )
            }
        }

        is PaneNode.Split -> SplitPane(node, tab, viewModel, modifier)
    }
}

@Composable
private fun SplitPane(
    node: PaneNode.Split,
    tab: TerminalTab,
    viewModel: TerminalViewModel,
    modifier: Modifier = Modifier,
) {
    var size by remember { mutableStateOf(IntSize.Zero) }
    val sideBySide = node.direction == SplitDirection.SideBySide

    val divider = @Composable {
        val length = if (sideBySide) size.width else size.height
        val dragState = rememberDraggableState { delta ->
            if (length > 0) viewModel.resizeSplit(node.id, delta / length)
        }

        Box(
            modifier = Modifier
                .then(if (sideBySide) Modifier.width(DividerThickness).fillMaxHeight() else Modifier.height(DividerThickness).fillMaxWidth())
                .background(MaterialTheme.colorScheme.outlineVariant)
                .draggable(
                    state = dragState,
                    orientation = if (sideBySide) Orientation.Horizontal else Orientation.Vertical,
                ),
        )
    }

    if (sideBySide) {
        Row(modifier = modifier.onSizeChanged { size = it }) {
            PaneTree(node.first, tab, viewModel, Modifier.weight(node.ratio).fillMaxHeight())
            divider()
            PaneTree(node.second, tab, viewModel, Modifier.weight(1f - node.ratio).fillMaxHeight())
        }
    } else {
        Column(modifier = modifier.onSizeChanged { size = it }) {
            PaneTree(node.first, tab, viewModel, Modifier.weight(node.ratio).fillMaxWidth())
            divider()
            PaneTree(node.second, tab, viewModel, Modifier.weight(1f - node.ratio).fillMaxWidth())
        }
    }
}

private val DividerThickness = 4.dp
