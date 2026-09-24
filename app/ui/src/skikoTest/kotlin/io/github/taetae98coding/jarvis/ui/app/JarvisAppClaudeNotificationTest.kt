package io.github.taetae98coding.jarvis.ui.app

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import io.github.taetae98coding.jarvis.domain.terminal.ClaudeActivity
import io.github.taetae98coding.jarvis.domain.terminal.ClaudeNotification
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
    private fun ComposeUiTest.finishTurn(claude: FakeClaudeActivityRepository, summary: String) {
        claude.activities.value = mapOf(Session to ClaudeActivity.Working)
        waitForIdle()
        claude.activities.value = mapOf(Session to ClaudeActivity.Finished(at = ++finishedAt, summary = summary))
        waitForIdle()
    }

    private var finishedAt = 0L

    @Test
    fun aFinishedTurnIsNotifiedWithoutOpeningTheTerminal() = runComposeUiTest {
        val terminal = FakeTerminalRepository()
        val claudeActivity = FakeClaudeActivityRepository()
        setContent { TestJarvisApp(terminal = terminal, terminalWorkspace = workspace(), claudeActivity = claudeActivity) }
        waitForIdle()

        finishTurn(claudeActivity, "tests green")

        assertEquals(listOf(ClaudeNotification("Claude 작업 완료", "패널 1 — tests green")), terminal.notifications)
    }

    @Test
    fun theClaudeTabOnScreenIsNotNotified() = runComposeUiTest {
        val terminal = FakeTerminalRepository()
        val claudeActivity = FakeClaudeActivityRepository()
        val workspace = workspace()
        setContent { TestJarvisApp(terminal = terminal, terminalWorkspace = workspace, claudeActivity = claudeActivity) }
        onNodeWithTag(TerminalTestTag).performClick()
        val (shell, claude) = workspace.workspace.value.tabs

        onNodeWithTag(terminalTabTestTag(claude.id)).performClick()
        waitUntil(timeoutMillis = FrameTimeoutMillis) { workspace.workspace.value.focusedTab?.id == claude.id }
        finishTurn(claudeActivity, "seen")
        assertEquals(emptyList(), terminal.notifications)

        onNodeWithTag(terminalTabTestTag(shell.id)).performClick()
        waitUntil(timeoutMillis = FrameTimeoutMillis) { workspace.workspace.value.focusedTab?.id == shell.id }
        finishTurn(claudeActivity, "hidden")
        assertEquals(listOf(ClaudeNotification("Claude 작업 완료", "패널 1 — hidden")), terminal.notifications)
    }

    private companion object {
        const val Session = "session"
        const val FrameTimeoutMillis = 10_000L
    }
}
