package io.github.taetae98coding.jarvis.ui.app

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.click
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.test.withKeyDown
import io.github.taetae98coding.jarvis.domain.terminal.FileContent
import io.github.taetae98coding.jarvis.domain.terminal.FileEntry
import io.github.taetae98coding.jarvis.domain.terminal.GitDiffHunk
import io.github.taetae98coding.jarvis.domain.terminal.GitFileDiff
import io.github.taetae98coding.jarvis.domain.terminal.TerminalProgram
import io.github.taetae98coding.jarvis.domain.terminal.TerminalWorkspace
import io.github.taetae98coding.jarvis.ui.terminal.TerminalFileViewerCommentAddTestTag
import io.github.taetae98coding.jarvis.ui.terminal.TerminalFileViewerCommentBarTestTag
import io.github.taetae98coding.jarvis.ui.terminal.TerminalFileViewerCommentCancelTestTag
import io.github.taetae98coding.jarvis.ui.terminal.TerminalFileViewerCommentClearTestTag
import io.github.taetae98coding.jarvis.ui.terminal.TerminalFileViewerCommentFieldTestTag
import io.github.taetae98coding.jarvis.ui.terminal.TerminalFileViewerCommentSendTestTag
import io.github.taetae98coding.jarvis.ui.terminal.TerminalTestTag
import io.github.taetae98coding.jarvis.ui.terminal.terminalFileEntryTestTag
import io.github.taetae98coding.jarvis.ui.terminal.terminalFileViewerCommentDeleteTestTag
import io.github.taetae98coding.jarvis.ui.terminal.terminalFileViewerCommentTargetTestTag
import io.github.taetae98coding.jarvis.ui.terminal.terminalFileViewerCommentTestTag
import io.github.taetae98coding.jarvis.ui.terminal.terminalFileViewerGutterTestTag
import io.github.taetae98coding.jarvis.ui.terminal.terminalFileViewerRemovedGutterTestTag
import io.github.taetae98coding.jarvis.ui.terminal.terminalTabCloseTestTag
import io.github.taetae98coding.jarvis.ui.terminal.terminalTabTestTag
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** docs/common/terminal-line-comment.html */
@OptIn(ExperimentalTestApi::class)
class JarvisAppTerminalLineCommentTest {
    private val readme = FileEntry("README.txt", "$Root/README.txt", isDirectory = false)
    private val notes = FileEntry("NOTES.txt", "$Root/NOTES.txt", isDirectory = false)

    private fun files() = FakeFileRepository(
        directories = mapOf(Root to listOf(notes, readme)),
        files = mapOf(
            readme.path to FileContent.Text("a\nB\nc\nd\ne", truncated = false),
            notes.path to FileContent.Text("n1\nn2", truncated = false),
        ),
    )

    // "a b c d" → "a B c d e"
    private fun git() = FakeGitChangesRepository().apply {
        diffs.value = mapOf(
            readme.path to GitFileDiff(
                listOf(
                    GitDiffHunk(oldStart = 2, oldCount = 1, newStart = 2, newCount = 1, removed = listOf("b")),
                    GitDiffHunk(oldStart = 4, oldCount = 0, newStart = 5, newCount = 1, removed = emptyList()),
                ),
            ),
        )
    }

    /** 선택된 "Jarvis"(/work/jarvis) 패널에 [claudeTabs] 개의 Claude 탭과, 그 뒤에 고른 README 파일 탭. */
    private fun workspace(claudeTabs: Int = 1): FakeTerminalWorkspaceRepository {
        var workspace = TerminalWorkspace.initial().addPanel(name = "Jarvis", directory = Root)
        repeat(claudeTabs) { workspace = workspace.addTab(program = TerminalProgram.Claude, directory = Root, claudeSessionId = "claude-$it") }
        if (claudeTabs == 0) workspace = workspace.addTab(directory = Root)

        return FakeTerminalWorkspaceRepository(workspace.openFile(readme.path))
    }

    private fun ComposeUiTest.count(tag: String) = onAllNodesWithTag(tag).fetchSemanticsNodes().size

    private fun ComposeUiTest.awaitTag(tag: String, count: Int = 1) {
        waitUntil(timeoutMillis = FrameTimeoutMillis) { count(tag) == count }
    }

    private fun ComposeUiTest.openTerminal(
        workspace: FakeTerminalWorkspaceRepository = workspace(),
        terminal: FakeTerminalRepository = FakeTerminalRepository(),
    ) {
        setContent { TestJarvisApp(terminal = terminal, terminalWorkspace = workspace, files = files(), gitChanges = git()) }
        onNodeWithTag(TerminalTestTag).performClick()
        awaitTag(terminalFileViewerGutterTestTag(1))
    }

    private fun ComposeUiTest.addComment(gutterTag: String, text: String) {
        onNodeWithTag(gutterTag).performClick()
        awaitTag(TerminalFileViewerCommentFieldTestTag)
        onNodeWithTag(TerminalFileViewerCommentFieldTestTag).performTextInput(text)
        onNodeWithTag(TerminalFileViewerCommentAddTestTag).performClick()
        awaitTag(TerminalFileViewerCommentFieldTestTag, count = 0)
    }

    private fun FakeTerminalSession.writtenText(): String = written.joinToString("") { it.decodeToString() }

    // C1, C3, C4, C5, C7, C8
    @Test
    // 보내기가 붙여넣은 뒤 실제 시간(ClaudeSubmitDelay)을 기다려 Enter 를 친다.
    @IgnoreOnWasm
    fun commentsOnLineNumbersAreSentToTheClaudeTabOnceItIsReady() = runComposeUiTest {
        val terminal = FakeTerminalRepository()
        val workspace = workspace()
        openTerminal(workspace, terminal)
        val claudeTab = workspace.workspace.value.tabs.single { it.program == TerminalProgram.Claude }
        val fileTab = workspace.workspace.value.focusedTab!!

        assertEquals(0, count(TerminalFileViewerCommentBarTestTag))
        onNodeWithTag(terminalFileViewerGutterTestTag(2)).performClick()
        awaitTag(TerminalFileViewerCommentFieldTestTag)
        onNodeWithTag(TerminalFileViewerCommentAddTestTag).assertIsNotEnabled()
        onNodeWithTag(TerminalFileViewerCommentFieldTestTag).performTextInput("   ")
        onNodeWithTag(TerminalFileViewerCommentAddTestTag).assertIsNotEnabled()
        onNodeWithTag(TerminalFileViewerCommentFieldTestTag).performTextInput("이름이 짧다")
        onNodeWithTag(TerminalFileViewerCommentAddTestTag).assertIsEnabled().performClick()
        awaitTag(TerminalFileViewerCommentFieldTestTag, count = 0)

        onNodeWithText("이름이 짧다").assertIsDisplayed()
        onNodeWithText("2행").assertIsDisplayed()
        onNodeWithText("코멘트 1개").assertIsDisplayed()

        addComment(terminalFileViewerGutterTestTag(4), "여기 null 이면?")
        onNodeWithText("코멘트 2개").assertIsDisplayed()
        onNodeWithTag(TerminalFileViewerCommentSendTestTag).assertTextEquals("Claude 에 보내기").performClick()

        // 보낼 곳 탭이 보이고 그 창이 열린다. 아직 Claude 가 아니라 셸만 붙여넣기를 켰으면 쓰지 않는다.
        waitUntil(timeoutMillis = FrameTimeoutMillis) { terminal.sessions.any { it.tab.id == claudeTab.id } }
        assertEquals(claudeTab.id, workspace.workspace.value.focusedTab?.id)
        val session = terminal.sessions.single { it.tab.id == claudeTab.id }
        session.emit("\u001b[?2004h")
        mainClock.advanceTimeBy(1_000)
        waitForIdle()
        assertTrue("[200~" !in session.writtenText(), session.writtenText())

        session.emit("\u001b[?2004h\u001b[?2031h\u001b[?1004h")
        waitUntil(timeoutMillis = FrameTimeoutMillis) { session.writtenText().endsWith("\r") }

        val text = session.writtenText()
        val pasted = text.substringAfter("\u001b[200~").substringBefore("\u001b[201~")
        assertTrue(pasted.startsWith("아래 코드 줄에 남긴 코멘트 2개를 반영해 주세요."), pasted)
        assertTrue("1. README.txt:2\r" in pasted, pasted)
        assertTrue("```\r2 + B\r```" in pasted, pasted)
        assertTrue("이름이 짧다" in pasted && "2. README.txt:4\r" in pasted && "여기 null 이면?" in pasted, pasted)
        assertTrue('\n' !in pasted, pasted)
        assertTrue(text.substringAfter("\u001b[201~") == "\r", text)

        // 보낸 코멘트는 사라진다.
        onNodeWithTag(terminalTabTestTag(fileTab.id)).performClick()
        awaitTag(terminalFileViewerGutterTestTag(1))
        assertEquals(0, count(TerminalFileViewerCommentBarTestTag))
        assertEquals(0, onAllNodesWithText("이름이 짧다").fetchSemanticsNodes().size)
    }

    // C1, C2, C3, C4, C5
    @Test
    fun shiftExtendsTheSelectionAndCardsCanBeRemovedOrCleared() = runComposeUiTest {
        openTerminal()

        onNodeWithTag(terminalFileViewerRemovedGutterTestTag(2)).performClick()
        awaitTag(TerminalFileViewerCommentFieldTestTag)
        onNodeWithTag(terminalFileViewerGutterTestTag(3)).performKeyInput { keyDown(Key.ShiftLeft) }
        onNodeWithTag(terminalFileViewerGutterTestTag(3)).performMouseInput { click() }
        onNodeWithTag(terminalFileViewerGutterTestTag(3)).performKeyInput { keyUp(Key.ShiftLeft) }
        onNodeWithTag(TerminalFileViewerCommentFieldTestTag).performTextInput("세 줄")
        onNodeWithTag(TerminalFileViewerCommentAddTestTag).performClick()
        awaitTag(TerminalFileViewerCommentFieldTestTag, count = 0)
        onNodeWithText("2–3행").assertIsDisplayed()

        // 입력 칸은 Esc 로 닫힌다.
        onNodeWithTag(terminalFileViewerRemovedGutterTestTag(2)).performClick()
        awaitTag(TerminalFileViewerCommentFieldTestTag)
        onNodeWithTag(TerminalFileViewerCommentFieldTestTag).performKeyInput { pressKey(Key.Escape) }
        awaitTag(TerminalFileViewerCommentFieldTestTag, count = 0)

        onNodeWithTag(terminalFileViewerRemovedGutterTestTag(2)).performClick()
        awaitTag(TerminalFileViewerCommentFieldTestTag)
        onNodeWithTag(TerminalFileViewerCommentFieldTestTag).performTextInput("지운 줄")
        onNodeWithTag(TerminalFileViewerCommentFieldTestTag).performKeyInput { withKeyDown(Key.MetaLeft) { pressKey(Key.Enter) } }
        awaitTag(TerminalFileViewerCommentFieldTestTag, count = 0)
        onNodeWithText("HEAD 2행 (지운 줄)").assertIsDisplayed()
        onNodeWithText("코멘트 2개").assertIsDisplayed()

        onNodeWithTag(terminalFileViewerCommentDeleteTestTag(0)).performClick()
        awaitTag(terminalFileViewerCommentTestTag(0), count = 0)
        onNodeWithText("코멘트 1개").assertIsDisplayed()

        onNodeWithTag(TerminalFileViewerCommentClearTestTag).performClick()
        awaitTag(TerminalFileViewerCommentBarTestTag, count = 0)
    }

    // C1, C3
    @Test
    fun cancelClosesTheEditorWithoutAComment() = runComposeUiTest {
        openTerminal()

        onNodeWithTag(terminalFileViewerGutterTestTag(1)).performClick()
        awaitTag(TerminalFileViewerCommentFieldTestTag)
        onNodeWithTag(TerminalFileViewerCommentFieldTestTag).performTextInput("버림")
        onNodeWithTag(TerminalFileViewerCommentCancelTestTag).performClick()
        awaitTag(TerminalFileViewerCommentFieldTestTag, count = 0)
        assertEquals(0, count(TerminalFileViewerCommentBarTestTag))
    }

    // C5, C10
    @Test
    fun commentsGatherPerPanelAndSurviveClosingTheFileTab() = runComposeUiTest {
        val workspace = workspace()
        openTerminal(workspace)
        val readmeTab = workspace.workspace.value.focusedTab!!

        addComment(terminalFileViewerGutterTestTag(1), "readme")

        onNodeWithTag(terminalFileEntryTestTag(notes.path)).performClick()
        awaitTag(terminalFileViewerGutterTestTag(2))
        onNodeWithText("코멘트 1개").assertIsDisplayed()
        assertEquals(0, onAllNodesWithText("readme").fetchSemanticsNodes().size)
        addComment(terminalFileViewerGutterTestTag(2), "notes")
        onNodeWithText("코멘트 2개").assertIsDisplayed()

        onNodeWithTag(terminalTabCloseTestTag(readmeTab.id)).performClick()
        waitUntil(timeoutMillis = FrameTimeoutMillis) { workspace.workspace.value.tabs.none { it.id == readmeTab.id } }
        onNodeWithTag(terminalFileEntryTestTag(readme.path)).performClick()
        waitUntil(timeoutMillis = FrameTimeoutMillis) { onAllNodesWithText("readme").fetchSemanticsNodes().size == 1 }
        onNodeWithText("코멘트 2개").assertIsDisplayed()
    }

    // C6
    @Test
    fun severalClaudeTabsAreOfferedInAMenu() = runComposeUiTest {
        val terminal = FakeTerminalRepository()
        val workspace = workspace(claudeTabs = 2)
        openTerminal(workspace, terminal)
        val second = workspace.workspace.value.tabs.filter { it.program == TerminalProgram.Claude }[1]

        addComment(terminalFileViewerGutterTestTag(1), "둘째로")
        onNodeWithTag(TerminalFileViewerCommentSendTestTag).performClick()
        awaitTag(terminalFileViewerCommentTargetTestTag(second.id))
        onNodeWithTag(terminalFileViewerCommentTargetTestTag(second.id)).assertTextContains("Claude 2").performClick()

        waitUntil(timeoutMillis = FrameTimeoutMillis) { terminal.sessions.any { it.tab.id == second.id } }
        assertEquals(second.id, workspace.workspace.value.focusedTab?.id)
    }

    // C6
    @Test
    // 보내기가 붙여넣은 뒤 실제 시간(ClaudeSubmitDelay)을 기다려 Enter 를 친다.
    @IgnoreOnWasm
    fun withoutAClaudeTabANewOneIsOpenedInTheFileTabsGroup() = runComposeUiTest {
        val terminal = FakeTerminalRepository()
        val workspace = workspace(claudeTabs = 0)
        openTerminal(workspace, terminal)
        val group = workspace.workspace.value.focusedGroup!!

        addComment(terminalFileViewerGutterTestTag(1), "새 탭으로")
        onNodeWithTag(TerminalFileViewerCommentSendTestTag).assertTextEquals("새 Claude 탭에 보내기").performClick()

        waitUntil(timeoutMillis = FrameTimeoutMillis) { terminal.sessions.any { it.program == TerminalProgram.Claude } }
        val opened = workspace.workspace.value.focusedTab!!
        assertEquals(TerminalProgram.Claude, opened.program)
        assertEquals(Root, opened.directory)
        assertTrue(workspace.workspace.value.focusedGroup!!.id == group.id)

        val session = terminal.sessions.single { it.program == TerminalProgram.Claude }
        session.emit("\u001b[?2004h\u001b[?1004h")
        waitUntil(timeoutMillis = FrameTimeoutMillis) { session.writtenText().endsWith("\r") }
        assertTrue("새 탭으로" in session.writtenText())
    }

    // C12
    @Test
    fun lineNumbersDoNothingWhereClaudeIsNotSupported() = runComposeUiTest {
        openTerminal(terminal = FakeTerminalRepository(isClaudeSupported = false))

        onNodeWithTag(terminalFileViewerGutterTestTag(2)).performClick()
        waitForIdle()
        assertEquals(0, count(TerminalFileViewerCommentFieldTestTag))
        assertEquals(0, count(TerminalFileViewerCommentBarTestTag))
    }

    private companion object {
        const val FrameTimeoutMillis = 10_000L
        const val Root = "/work/jarvis"
    }
}
