package io.github.taetae98coding.jarvis.ui.app

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onAllNodesWithTag
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
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@OptIn(ExperimentalTestApi::class)
class JarvisAppTerminalClaudeStatusTest {
    private fun ComposeUiTest.openTerminal() {
        onNodeWithTag(TerminalTestTag).performClick()
    }

    private fun ComposeUiTest.status(panelId: Long): String? =
        onAllNodesWithTag(terminalPanelClaudeStatusTestTag(panelId), useUnmergedTree = true)
            .fetchSemanticsNodes()
            .singleOrNull()
            ?.config
            ?.get(SemanticsProperties.ContentDescription)
            ?.single()

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
    fun aFinishedClaudeAwaitsUntilItsTabIsSeenAndAwaitsAgainForANewResult() = runComposeUiTest {
        val workspace = FakeTerminalWorkspaceRepository(initial)
        val claude = FakeClaudeActivityRepository(mapOf("login" to ClaudeActivity.Finished(at = 10)))
        setContent { TestJarvisApp(terminalWorkspace = workspace, claudeActivity = claude, windowFocused = mutableStateOf(true)) }
        openTerminal()
        awaitStatus(login.id, "Claude 응답 대기")

        onNodeWithTag(terminalPanelClaudeStatusTestTag(login.id), useUnmergedTree = true).performClick()

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
    fun theMostUrgentTabOfAPanelWins() = runComposeUiTest {
        val two = initial.selectPanel(api.id).addClaude("api 1").addClaude("api 2")
        val claude = FakeClaudeActivityRepository(mapOf("api 1" to ClaudeActivity.Working, "api 2" to ClaudeActivity.Monitoring))
        setContent { TestJarvisApp(terminalWorkspace = FakeTerminalWorkspaceRepository(two.selectPanel(jarvis.id)), claudeActivity = claude) }
        openTerminal()
        awaitStatus(api.id, "Claude 작업 중")

        claude.activities.value = claude.activities.value + ("api 2" to ClaudeActivity.Finished(at = 1))

        awaitStatus(api.id, "Claude 응답 대기")
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
