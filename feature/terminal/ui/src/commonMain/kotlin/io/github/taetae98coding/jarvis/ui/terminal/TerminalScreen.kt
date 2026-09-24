package io.github.taetae98coding.jarvis.ui.terminal

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.style.rememberUpdatedStyleState
import androidx.compose.foundation.style.styleable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
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
import androidx.compose.ui.unit.IntSize
import io.github.taetae98coding.jarvis.designsystem.component.JarvisIconButton
import io.github.taetae98coding.jarvis.designsystem.component.JarvisTopBar
import io.github.taetae98coding.jarvis.designsystem.icon.JarvisIcons
import io.github.taetae98coding.jarvis.designsystem.theme.JarvisTheme
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
        verticalArrangement = Arrangement.spacedBy(JarvisTheme.dimens.spacing.s),
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

@Composable
private fun TerminalTopBar(
    onBack: () -> Unit,
    onSplitSideBySide: () -> Unit,
    onSplitStacked: () -> Unit,
    onNewTab: () -> Unit,
    onClose: () -> Unit,
) {
    JarvisTopBar(title = "터미널", onBack = onBack) {
        JarvisIconButton(
            icon = JarvisIcons.SplitSideBySide,
            contentDescription = "좌우 분할",
            onClick = onSplitSideBySide,
            modifier = Modifier.testTag(TerminalSplitSideTestTag),
        )
        JarvisIconButton(
            icon = JarvisIcons.SplitStacked,
            contentDescription = "상하 분할",
            onClick = onSplitStacked,
            modifier = Modifier.testTag(TerminalSplitStackedTestTag),
        )
        JarvisIconButton(
            icon = JarvisIcons.Add,
            contentDescription = "새 탭",
            onClick = onNewTab,
            modifier = Modifier.testTag(TerminalNewTabTestTag),
        )
        JarvisIconButton(
            icon = JarvisIcons.Close,
            contentDescription = "닫기",
            onClick = onClose,
            modifier = Modifier.testTag(TerminalCloseTestTag),
        )
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
        horizontalArrangement = Arrangement.spacedBy(JarvisTheme.dimens.spacing.xs),
    ) {
        workspace.tabs.forEachIndexed { index, tab ->
            TerminalTab(
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
        val thickness = JarvisTheme.dimens.stroke.thick
        val interactionSource = remember { MutableInteractionSource() }
        val styleState = rememberUpdatedStyleState(interactionSource)

        Box(
            modifier = Modifier
                .then(if (sideBySide) Modifier.width(thickness).fillMaxHeight() else Modifier.height(thickness).fillMaxWidth())
                .hoverable(interactionSource)
                .styleable(styleState, TerminalPaneDefaults.dividerStyle)
                .draggable(
                    state = dragState,
                    orientation = if (sideBySide) Orientation.Horizontal else Orientation.Vertical,
                    interactionSource = interactionSource,
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

