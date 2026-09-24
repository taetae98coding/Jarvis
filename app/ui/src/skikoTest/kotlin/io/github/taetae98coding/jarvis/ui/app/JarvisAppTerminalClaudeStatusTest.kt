package io.github.taetae98coding.jarvis.ui.app

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import io.github.taetae98coding.jarvis.domain.terminal.ClaudeActivity
import io.github.taetae98coding.jarvis.domain.terminal.TerminalPanel
import io.github.taetae98coding.jarvis.domain.terminal.TerminalProgram
import io.github.taetae98coding.jarvis.domain.terminal.TerminalWorkspace
import io.github.taetae98coding.jarvis.ui.terminal.TerminalTestTag
import io.github.taetae98coding.jarvis.ui.terminal.terminalPanelClaudeStatusTestTag
import io.github.taetae98coding.jarvis.ui.terminal.terminalPanelRenameTestTag
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class JarvisAppTerminalClaudeStatusTest {
    private fun ComposeUiTest.openTerminal() {
        onNodeWithTag(TerminalTestTag).performClick()
    }

    private fun ComposeUiTest.statusNodes(panelId: Long): List<SemanticsNode> =
        onAllNodes(
            SemanticsMatcher("panel $panelId Claude status") {
                it.config.getOrNull(SemanticsProperties.TestTag)?.startsWith(terminalPanelClaudeStatusTestTag(panelId, tabId = 0).dropLast(1)) == true
            },
            useUnmergedTree = true,
        ).fetchSemanticsNodes()

    /** 줄의 표시들, 그려진 순서대로. */
    private fun ComposeUiTest.statuses(panelId: Long): List<String> =
        statusNodes(panelId).map { it.config[SemanticsProperties.ContentDescription].single() }

    private fun ComposeUiTest.status(panelId: Long): String? = statuses(panelId).singleOrNull()

    private fun TerminalWorkspace.tabOf(sessionId: String): Long = tabs.single { it.claudeSessionId == sessionId }.id

    private fun ComposeUiTest.awaitStatus(panelId: Long, expected: String?) {
        waitUntil(timeoutMillis = FrameTimeoutMillis) { status(panelId) == expected }
    }

    private fun TerminalWorkspace.addClaude(sessionId: String): TerminalWorkspace =
        addTab(program = TerminalProgram.Claude, claudeSessionId = sessionId)

    // "Jarvis"(셸 탭, Claude "main") 아래 워크트리 "login"(Claude "login"), 그리고 Claude 가 없는 "API". Jarvis 가 선택돼 있다.
    private val jarvisOnly: TerminalWorkspace = TerminalWorkspace.initial().renamePanel(1, "Jarvis").addClaude("main")
    private val jarvis: TerminalPanel = jarvisOnly.panels.single()
    private val withWorktree: TerminalWorkspace = jarvisOnly.addWorktreePanel(jarvis.id, name = "login", directory = "/w/login").addClaude("login")
    private val login: TerminalPanel = withWorktree.panels.last()
    private val initial: TerminalWorkspace = withWorktree.addPanel(name = "API").addTab().selectPanel(jarvis.id).let { it.selectTab(it.panels.first().tabs.first().id) }
    private val api: TerminalPanel = initial.panels.last()

    @Test
    fun theMainWorktreeAndItsWorktreeShowTheirOwnClaude() = runComposeUiTest {
        val claude = FakeClaudeActivityRepository(mapOf("main" to ClaudeActivity.Working, "login" to ClaudeActivity.Monitoring))
        setContent { TestJarvisApp(terminalWorkspace = FakeTerminalWorkspaceRepository(initial), claudeActivity = claude) }
        openTerminal()

        awaitStatus(jarvis.id, "Claude 작업 중")
        awaitStatus(login.id, "Claude 모니터링 중")
        assertNull(status(api.id))
    }

    @Test
    fun statusSitsOnTheButtonLineBelowTheName() = runComposeUiTest {
        val claude = FakeClaudeActivityRepository(mapOf("main" to ClaudeActivity.Working))
        setContent { TestJarvisApp(terminalWorkspace = FakeTerminalWorkspaceRepository(initial), claudeActivity = claude) }
        openTerminal()
        awaitStatus(jarvis.id, "Claude 작업 중")

        val title = onNodeWithText("Jarvis").getBoundsInRoot()
        val status = onNodeWithTag(terminalPanelClaudeStatusTestTag(jarvis.id, initial.tabOf("main")), useUnmergedTree = true).getBoundsInRoot()
        val rename = onNodeWithTag(terminalPanelRenameTestTag(jarvis.id), useUnmergedTree = true).getBoundsInRoot()
        assertTrue(status.top >= title.bottom, "상태 표시가 이름 아래 줄에 있어야 한다: $title / $status")
        assertTrue(status.right <= rename.left, "상태 표시가 버튼 줄 왼쪽에 있어야 한다: $status / $rename")
        assertEquals(rename.top, status.top)
    }

    @Test
    fun aFinishedClaudeAwaitsUntilItsTabIsSeenAndAwaitsAgainForANewResult() = runComposeUiTest {
        val workspace = FakeTerminalWorkspaceRepository(initial)
        val claude = FakeClaudeActivityRepository(mapOf("login" to ClaudeActivity.Finished(at = 10)))
        setContent { TestJarvisApp(terminalWorkspace = workspace, claudeActivity = claude, windowFocused = mutableStateOf(true)) }
        openTerminal()
        awaitStatus(login.id, "Claude 응답 대기")

        onNodeWithTag(terminalPanelClaudeStatusTestTag(login.id, initial.tabOf("login")), useUnmergedTree = true).performClick()

        awaitStatus(login.id, "Claude 확인함")
        waitUntil(timeoutMillis = FrameTimeoutMillis) { workspace.workspace.value.tabs.any { it.claudeCheckedAt == 10L } }

        // 보고 있는 동안 끝난 결과는 곧바로 확인함이다.
        claude.activities.value = mapOf("login" to ClaudeActivity.Working)
        awaitStatus(login.id, "Claude 작업 중")
        claude.activities.value = mapOf("login" to ClaudeActivity.Finished(at = 20))
        awaitStatus(login.id, "Claude 확인함")

        onNodeWithText("Jarvis").performClick()
        claude.activities.value = mapOf("login" to ClaudeActivity.Finished(at = 30))

        awaitStatus(login.id, "Claude 응답 대기")
    }

    @Test
    fun aVisibleTabIsNotSeenWhileTheWindowIsNotFocused() = runComposeUiTest {
        val workspace = FakeTerminalWorkspaceRepository(initial.selectPanel(login.id))
        val claude = FakeClaudeActivityRepository(mapOf("login" to ClaudeActivity.Finished(at = 10)))
        val focused = mutableStateOf(false)
        setContent { TestJarvisApp(terminalWorkspace = workspace, claudeActivity = claude, windowFocused = focused) }
        openTerminal()

        awaitStatus(login.id, "Claude 응답 대기")
        assertNull(workspace.workspace.value.tabs.single { it.claudeSessionId == "login" }.claudeCheckedAt)

        focused.value = true

        awaitStatus(login.id, "Claude 확인함")
    }

    @Test
    fun everyClaudeOfAPanelShowsInTabOrder() = runComposeUiTest {
        val two = initial.selectPanel(api.id).addClaude("api 1").addClaude("api 2").addClaude("api 3")
        val claude = FakeClaudeActivityRepository(mapOf("api 1" to ClaudeActivity.Working, "api 3" to ClaudeActivity.Monitoring))
        setContent { TestJarvisApp(terminalWorkspace = FakeTerminalWorkspaceRepository(two.selectPanel(jarvis.id)), claudeActivity = claude) }
        openTerminal()

        // 상태를 모르는 "api 2" 는 빠진다.
        waitUntil(timeoutMillis = FrameTimeoutMillis) { statuses(api.id) == listOf("Claude 작업 중", "Claude 모니터링 중") }

        claude.activities.value = claude.activities.value + ("api 2" to ClaudeActivity.Finished(at = 1))

        // 급한 상태가 생겨도 자리는 탭 순서 그대로다.
        waitUntil(timeoutMillis = FrameTimeoutMillis) {
            statuses(api.id) == listOf("Claude 작업 중", "Claude 응답 대기", "Claude 모니터링 중")
        }
    }

    @Test
    fun clickingAStatusSelectsItsTab() = runComposeUiTest {
        val two = initial.selectPanel(api.id).addClaude("api 1").addClaude("api 2")
        val workspace = FakeTerminalWorkspaceRepository(two.selectPanel(jarvis.id))
        val claude = FakeClaudeActivityRepository(mapOf("api 1" to ClaudeActivity.Working, "api 2" to ClaudeActivity.Finished(at = 5)))
        setContent { TestJarvisApp(terminalWorkspace = workspace, claudeActivity = claude, windowFocused = mutableStateOf(true)) }
        openTerminal()
        waitUntil(timeoutMillis = FrameTimeoutMillis) { statuses(api.id) == listOf("Claude 작업 중", "Claude 응답 대기") }

        onNodeWithTag(terminalPanelClaudeStatusTestTag(api.id, two.tabOf("api 2")), useUnmergedTree = true).performClick()

        waitUntil(timeoutMillis = FrameTimeoutMillis) { statuses(api.id) == listOf("Claude 작업 중", "Claude 확인함") }
        assertEquals(api.id, workspace.workspace.value.selectedPanelId)
        assertEquals(two.tabOf("api 2"), workspace.workspace.value.focusedTab?.id)
    }

    @Test
    fun statusesThatDoNotFitWrapBelowAndTheButtonsStayOnTheFirstLine() = runComposeUiTest {
        val sessions = (1..8).map { "api $it" }
        val many = sessions.fold(initial.selectPanel(api.id)) { acc, id -> acc.addClaude(id) }
        val claude = FakeClaudeActivityRepository(sessions.associateWith { ClaudeActivity.Monitoring })
        setContent { TestJarvisApp(terminalWorkspace = FakeTerminalWorkspaceRepository(many.selectPanel(jarvis.id)), claudeActivity = claude) }
        openTerminal()
        waitUntil(timeoutMillis = FrameTimeoutMillis) { statuses(api.id).size == sessions.size }

        val bounds = sessions.map { onNodeWithTag(terminalPanelClaudeStatusTestTag(api.id, many.tabOf(it)), useUnmergedTree = true).getBoundsInRoot() }
        val rename = onNodeWithTag(terminalPanelRenameTestTag(api.id), useUnmergedTree = true).getBoundsInRoot()
        assertEquals(rename.top, bounds.first().top, "버튼과 첫 표시가 같은 줄이어야 한다")
        assertTrue(bounds.last().top > bounds.first().top, "넘친 표시는 다음 줄로 가야 한다: $bounds")
        assertTrue(bounds.all { it.right <= rename.left }, "표시가 버튼을 가리면 안 된다: $bounds / $rename")
    }

    @Test
    fun thereIsNoStatusWhereClaudeCannotRun() = runComposeUiTest {
        val claude = FakeClaudeActivityRepository(mapOf("main" to ClaudeActivity.Working, "login" to ClaudeActivity.Working))
        setContent {
            TestJarvisApp(
                terminal = FakeTerminalRepository(isClaudeSupported = false),
                terminalWorkspace = FakeTerminalWorkspaceRepository(initial),
                claudeActivity = claude,
            )
        }
        openTerminal()
        waitForIdle()

        assertEquals(listOf(null, null, null), listOf(jarvis.id, login.id, api.id).map { status(it) })
    }

    companion object {
        const val FrameTimeoutMillis = 10_000L
    }
}
