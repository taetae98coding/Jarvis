package io.github.taetae98coding.jarvis.ui.app

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import io.github.taetae98coding.jarvis.domain.terminal.ClaudeActivity
import io.github.taetae98coding.jarvis.domain.terminal.ClaudeNotification
import io.github.taetae98coding.jarvis.domain.terminal.ClaudeStatus
import io.github.taetae98coding.jarvis.domain.terminal.TerminalProgram
import io.github.taetae98coding.jarvis.domain.terminal.TerminalWorkspace
import io.github.taetae98coding.jarvis.ui.terminal.TerminalTestTag
import io.github.taetae98coding.jarvis.ui.terminal.terminalTabTestTag
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class JarvisAppClaudeNotificationTest {
    // 패널 "패널 1" 의 그룹 하나에 셸 탭(선택)과 Claude 탭.
    private fun workspace(): FakeTerminalWorkspaceRepository =
        FakeTerminalWorkspaceRepository(
            TerminalWorkspace.initial()
                .addTab(program = TerminalProgram.Claude, claudeSessionId = Session)
                .let { it.selectTab(it.tabs.first().id) },
        )

    /** 한 턴을 돌린다. 상태를 한 번에 바꾸면 StateFlow 가 합쳐 Working 을 건너뛴다. */
    private fun ComposeUiTest.finishTurn(terminal: FakeTerminalRepository, summary: String) {
        terminal.claudeStatuses.value = mapOf(Session to ClaudeStatus(ClaudeActivity.Working))
        waitForIdle()
        terminal.claudeStatuses.value = mapOf(Session to ClaudeStatus(ClaudeActivity.Finished, summary))
        waitForIdle()
    }

    @Test
    fun aFinishedTurnIsNotifiedWithoutOpeningTheTerminal() = runComposeUiTest {
        val terminal = FakeTerminalRepository()
        setContent { TestJarvisApp(terminal = terminal, terminalWorkspace = workspace()) }
        waitForIdle()

        finishTurn(terminal, "tests green")

        assertEquals(listOf(ClaudeNotification("Claude 작업 완료", "패널 1 — tests green")), terminal.notifications)
    }

    @Test
    fun theClaudeTabOnScreenIsNotNotified() = runComposeUiTest {
        val terminal = FakeTerminalRepository()
        val workspace = workspace()
        setContent { TestJarvisApp(terminal = terminal, terminalWorkspace = workspace) }
        onNodeWithTag(TerminalTestTag).performClick()
        val (shell, claude) = workspace.workspace.value.tabs

        onNodeWithTag(terminalTabTestTag(claude.id)).performClick()
        waitUntil(timeoutMillis = FrameTimeoutMillis) { workspace.workspace.value.focusedTab?.id == claude.id }
        finishTurn(terminal, "seen")
        assertEquals(emptyList(), terminal.notifications)

        onNodeWithTag(terminalTabTestTag(shell.id)).performClick()
        waitUntil(timeoutMillis = FrameTimeoutMillis) { workspace.workspace.value.focusedTab?.id == shell.id }
        finishTurn(terminal, "hidden")
        assertEquals(listOf(ClaudeNotification("Claude 작업 완료", "패널 1 — hidden")), terminal.notifications)
    }

    private companion object {
        const val Session = "session"
        const val FrameTimeoutMillis = 10_000L
    }
}
