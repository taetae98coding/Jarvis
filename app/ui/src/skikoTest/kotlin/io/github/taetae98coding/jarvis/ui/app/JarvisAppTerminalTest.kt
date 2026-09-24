package io.github.taetae98coding.jarvis.ui.app

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.v2.runComposeUiTest
import io.github.taetae98coding.jarvis.ui.terminal.TerminalCloseTestTag
import io.github.taetae98coding.jarvis.ui.terminal.TerminalNewTabTestTag
import io.github.taetae98coding.jarvis.ui.terminal.TerminalScreenTestTag
import io.github.taetae98coding.jarvis.ui.terminal.TerminalSplitSideTestTag
import io.github.taetae98coding.jarvis.ui.terminal.TerminalSplitStackedTestTag
import io.github.taetae98coding.jarvis.ui.terminal.TerminalTestTag
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class JarvisAppTerminalTest {
    private fun tagPrefix(prefix: String) = SemanticsMatcher("testTag starts with $prefix") {
        it.config.getOrNull(SemanticsProperties.TestTag)?.startsWith(prefix) == true
    }

    private val pane = tagPrefix("terminal:pane:")
    private val tab = tagPrefix("terminal:tab:")

    private fun ComposeUiTest.paneCount() = onAllNodes(pane).fetchSemanticsNodes().size

    private fun ComposeUiTest.openTerminal(terminal: FakeTerminalRepository) {
        setContent { TestJarvisApp(terminal = terminal) }
        onNodeWithTag(TerminalTestTag).performClick()
        waitUntil(timeoutMillis = FrameTimeoutMillis) { terminal.sessions.size == 1 }
    }

    @Test
    fun terminalCardOpensOneTabWithOneShell() = runComposeUiTest {
        val terminal = FakeTerminalRepository()
        openTerminal(terminal)

        onNodeWithTag(TerminalScreenTestTag).assertIsDisplayed()
        assertEquals(1, paneCount())
        assertEquals(1, onAllNodes(tab).fetchSemanticsNodes().size)
        onNodeWithText("셸 1").assertIsDisplayed()
    }

    @Test
    fun terminalCardIsLockedWhereItIsNotSupported() = runComposeUiTest {
        val terminal = FakeTerminalRepository(isSupported = false)
        setContent { TestJarvisApp(terminal = terminal) }

        onNodeWithTag(TerminalTestTag).assertIsNotEnabled().performClick()

        onNodeWithText("이 플랫폼에서는 셸을 실행할 수 없습니다.").assertIsDisplayed()
        assertEquals(0, onAllNodesWithTag(TerminalScreenTestTag).fetchSemanticsNodes().size)
        assertTrue(terminal.sessions.isEmpty())
    }

    @Test
    fun sideBySideSplitOpensASecondShell() = runComposeUiTest {
        val terminal = FakeTerminalRepository()
        openTerminal(terminal)

        onNodeWithTag(TerminalSplitSideTestTag).performClick()

        waitUntil(timeoutMillis = FrameTimeoutMillis) { terminal.sessions.size == 2 }
        assertEquals(2, paneCount())
    }

    @Test
    fun stackedSplitOpensASecondShell() = runComposeUiTest {
        val terminal = FakeTerminalRepository()
        openTerminal(terminal)

        onNodeWithTag(TerminalSplitStackedTestTag).performClick()

        waitUntil(timeoutMillis = FrameTimeoutMillis) { terminal.sessions.size == 2 }
        assertEquals(2, paneCount())
    }

    @Test
    fun newTabShowsOnlyItsOwnPane() = runComposeUiTest {
        val terminal = FakeTerminalRepository()
        openTerminal(terminal)
        onNodeWithTag(TerminalSplitSideTestTag).performClick()

        onNodeWithTag(TerminalNewTabTestTag).performClick()

        waitUntil(timeoutMillis = FrameTimeoutMillis) { terminal.sessions.size == 3 }
        assertEquals(2, onAllNodes(tab).fetchSemanticsNodes().size)
        onNodeWithText("셸 2").assertIsDisplayed()
        assertEquals(1, paneCount())
    }

    @Test
    fun closeEndsTheFocusedShell() = runComposeUiTest {
        val terminal = FakeTerminalRepository()
        openTerminal(terminal)
        onNodeWithTag(TerminalSplitSideTestTag).performClick()
        waitUntil(timeoutMillis = FrameTimeoutMillis) { terminal.sessions.size == 2 }

        onNodeWithTag(TerminalCloseTestTag).performClick()

        waitUntil(timeoutMillis = FrameTimeoutMillis) { paneCount() == 1 }
        assertTrue(terminal.sessions[1].closed)
        assertFalse(terminal.sessions[0].closed)
    }

    @Test
    fun closingTheLastPaneReturnsHome() = runComposeUiTest {
        val terminal = FakeTerminalRepository()
        openTerminal(terminal)

        onNodeWithTag(TerminalCloseTestTag).performClick()

        waitUntil(timeoutMillis = FrameTimeoutMillis) {
            onAllNodesWithTag(TerminalScreenTestTag).fetchSemanticsNodes().isEmpty()
        }
        onNodeWithTag(TerminalTestTag).assertIsDisplayed()
        assertTrue(terminal.sessions.single().closed)
    }

    @Test
    fun exitedShellClosesItsPane() = runComposeUiTest {
        val terminal = FakeTerminalRepository()
        openTerminal(terminal)
        onNodeWithTag(TerminalSplitStackedTestTag).performClick()
        waitUntil(timeoutMillis = FrameTimeoutMillis) { terminal.sessions.size == 2 }

        terminal.sessions[1].exit()

        waitUntil(timeoutMillis = FrameTimeoutMillis) { paneCount() == 1 }
    }

    @Test
    fun leavingTheScreenEndsEveryShell() = runComposeUiTest {
        val terminal = FakeTerminalRepository()
        openTerminal(terminal)
        onNodeWithTag(TerminalNewTabTestTag).performClick()
        waitUntil(timeoutMillis = FrameTimeoutMillis) { terminal.sessions.size == 2 }

        onNodeWithText("← 뒤로").performClick()

        waitUntil(timeoutMillis = FrameTimeoutMillis) { terminal.sessions.all { it.closed } }
    }

    @Test
    fun tabIsNamedAfterTheShellTitle() = runComposeUiTest {
        val terminal = FakeTerminalRepository()
        openTerminal(terminal)

        terminal.sessions.single().emit("\u001b]0;build-server\u0007")

        waitUntil(timeoutMillis = FrameTimeoutMillis) {
            onAllNodesWithText("build-server").fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun typedTextGoesToTheFocusedShell() = runComposeUiTest {
        val terminal = FakeTerminalRepository()
        openTerminal(terminal)

        onNode(hasSetTextAction() and hasAnyAncestor(pane)).performTextInput("ls")

        waitUntil(timeoutMillis = FrameTimeoutMillis) {
            terminal.sessions.single().written.joinToString("") { it.decodeToString() } == "ls"
        }
    }

    // 에뮬레이터가 셸의 커서 위치 질의에 답해야 zsh 같은 셸이 프롬프트를 제자리에 그린다.
    @Test
    fun cursorPositionQueriesAreAnswered() = runComposeUiTest {
        val terminal = FakeTerminalRepository()
        openTerminal(terminal)

        terminal.sessions.single().emit("ab\u001b[6n")

        waitUntil(timeoutMillis = FrameTimeoutMillis) {
            terminal.sessions.single().written.any { it.decodeToString() == "\u001b[1;3R" }
        }
    }

    private companion object {
        const val FrameTimeoutMillis = 10_000L
    }
}
