package io.github.taetae98coding.jarvis.ui.terminal

import io.github.taetae98coding.jarvis.domain.terminal.DiffedLine
import io.github.taetae98coding.jarvis.domain.terminal.LineComment
import io.github.taetae98coding.jarvis.domain.terminal.TerminalProgram
import io.github.taetae98coding.jarvis.domain.terminal.TerminalSize
import io.github.taetae98coding.jarvis.domain.terminal.UpdateTerminalWorkspaceUseCase
import io.github.taetae98coding.jarvis.domain.terminal.lineCommentsPrompt
import io.github.taetae98coding.jarvis.domain.terminal.newClaudeSessionId
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

internal val ClaudeReadyTimeout: Duration = 30.seconds

// 붙여넣기 끝과 같은 읽기에 들어간 CR 은 Claude Code 가 붙여넣기에 이어진 입력으로 볼 수 있어 따로 보낸다.
internal val ClaudeSubmitDelay: Duration = 300.milliseconds

/** 한 패널에 모인 코멘트와 보내기 상태(docs/common/terminal-line-comment.html). */
internal data class PanelLineComments(
    val comments: List<LineComment> = emptyList(),
    val sending: Boolean = false,
    val failed: Boolean = false,
)

/** 코멘트를 보낼 곳. [tabId] 가 null 이면 [groupId] 그룹에 새 Claude 탭을 연다. */
internal data class LineCommentTarget(val tabId: Long?, val groupId: Long)

/**
 * 패널마다 모인 코멘트를 들고 Claude 탭에 보낸다. 사용자가 적은 초안이라 터미널 화면을 떠나도 남아야 하고, 보내는
 * 도중에 화면을 떠나도 끝까지 보내야 해서 [TerminalPaneHost] 처럼 앱 수명 스코프를 쓴다
 * (docs/common/terminal-line-comment.html#implementation).
 */
internal class LineCommentHost(
    private val paneHost: TerminalPaneHost,
    private val updateWorkspace: UpdateTerminalWorkspaceUseCase,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var nextId = 0L

    private val _panels = MutableStateFlow<Map<Long, PanelLineComments>>(emptyMap())
    val panels: StateFlow<Map<Long, PanelLineComments>> = _panels.asStateFlow()

    fun add(panelId: Long, path: String, lines: List<DiffedLine>, body: String) {
        if (lines.isEmpty() || body.isBlank()) return

        val comment = LineComment(nextId++, path, lines, body.trim())
        edit(panelId) { it.copy(comments = it.comments + comment, failed = false) }
    }

    fun remove(panelId: Long, commentId: Long) = edit(panelId) { panel ->
        panel.copy(comments = panel.comments.filterNot { it.id == commentId }, failed = false)
    }

    fun clear(panelId: Long) = edit(panelId) { it.copy(comments = emptyList(), failed = false) }

    /** 닫힌 패널의 코멘트를 버린다. */
    fun retain(panelIds: Set<Long>) {
        _panels.update { panels -> panels.filterKeys { it in panelIds } }
    }

    fun send(panelId: Long, target: LineCommentTarget) {
        val current = _panels.value[panelId] ?: return
        if (current.sending || current.comments.isEmpty()) return

        val sending = current.comments
        edit(panelId) { it.copy(sending = true, failed = false) }

        scope.launch {
            val sent = deliver(sending, target)
            edit(panelId) { panel ->
                val sentIds = if (sent) sending.map { it.id }.toSet() else emptySet()
                panel.copy(comments = panel.comments.filterNot { it.id in sentIds }, sending = false, failed = !sent)
            }
        }
    }

    fun close() {
        scope.cancel()
    }

    private suspend fun deliver(comments: List<LineComment>, target: LineCommentTarget): Boolean {
        val tabId = target.tabId
        val change = updateWorkspace { workspace ->
            if (tabId != null) {
                workspace.selectTab(tabId)
            } else {
                val directory = workspace.panels.firstOrNull { panel -> panel.groups.any { it.id == target.groupId } }?.directory
                workspace.addTab(target.groupId, TerminalProgram.Claude, directory, newClaudeSessionId())
            }
        }
        paneHost.release(change.removedTabs.map { it.id })

        val openedId = tabId ?: change.before.nextId
        val tab = change.after.tabs.firstOrNull { it.id == openedId && it.program == TerminalProgram.Claude } ?: return false
        // 보이게 된 탭은 화면도 곧 창을 연다. 같은 탭이면 같은 창이다.
        val pane = paneHost.acquire(tab, TerminalSize.Default)

        val ready = withTimeoutOrNull(ClaudeReadyTimeout) { pane.revision.first { pane.readyForClaudeInput } } != null
        if (!ready) return false

        pane.paste(lineCommentsPrompt(comments, tab.directory))
        delay(ClaudeSubmitDelay)
        pane.input(byteArrayOf('\r'.code.toByte()))

        return true
    }

    private fun edit(panelId: Long, transform: (PanelLineComments) -> PanelLineComments) {
        _panels.update { panels ->
            val next = transform(panels[panelId] ?: PanelLineComments())
            if (next == PanelLineComments()) panels - panelId else panels + (panelId to next)
        }
    }
}
