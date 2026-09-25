package io.github.taetae98coding.jarvis.ui.app

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.click
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.test.withKeyDown
import io.github.taetae98coding.jarvis.domain.terminal.FileContent
import io.github.taetae98coding.jarvis.domain.terminal.FileEntry
import io.github.taetae98coding.jarvis.domain.terminal.GitCommitFile
import io.github.taetae98coding.jarvis.domain.terminal.GitFileDiff
import io.github.taetae98coding.jarvis.domain.terminal.TerminalProgram
import io.github.taetae98coding.jarvis.domain.terminal.TerminalWorkspace
import io.github.taetae98coding.jarvis.ui.terminal.TerminalFileViewerCloseEditorTestTag
import io.github.taetae98coding.jarvis.ui.terminal.TerminalFileViewerDirtyTestTag
import io.github.taetae98coding.jarvis.ui.terminal.TerminalFileViewerDiscardTestTag
import io.github.taetae98coding.jarvis.ui.terminal.TerminalFileViewerDiskChangedTestTag
import io.github.taetae98coding.jarvis.ui.terminal.TerminalFileViewerEditErrorTestTag
import io.github.taetae98coding.jarvis.ui.terminal.TerminalFileViewerEditTestTag
import io.github.taetae98coding.jarvis.ui.terminal.TerminalFileViewerEditorTestTag
import io.github.taetae98coding.jarvis.ui.terminal.TerminalFileViewerKeepEditingTestTag
import io.github.taetae98coding.jarvis.ui.terminal.TerminalFileViewerMarkdownTestTag
import io.github.taetae98coding.jarvis.ui.terminal.TerminalFileViewerPreviewTestTag
import io.github.taetae98coding.jarvis.ui.terminal.TerminalFileViewerSaveTestTag
import io.github.taetae98coding.jarvis.ui.terminal.TerminalFileViewerScrollerTestTag
import io.github.taetae98coding.jarvis.ui.terminal.TerminalFileViewerSourceTestTag
import io.github.taetae98coding.jarvis.ui.terminal.TerminalFileViewerWebPreviewTestTag
import io.github.taetae98coding.jarvis.ui.terminal.TerminalFilesRootTestTag
import io.github.taetae98coding.jarvis.ui.terminal.TerminalFilesScrollerTestTag
import io.github.taetae98coding.jarvis.ui.terminal.TerminalTestTag
import io.github.taetae98coding.jarvis.ui.terminal.terminalFileEntryTestTag
import io.github.taetae98coding.jarvis.ui.terminal.terminalFileViewerGutterTestTag
import io.github.taetae98coding.jarvis.ui.terminal.terminalFileViewerTestTag
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** docs/common/terminal-file-editor.html */
@OptIn(ExperimentalTestApi::class)
class JarvisAppTerminalFileEditorTest {
    private val main = FileEntry("Main.kt", "$Root/Main.kt", isDirectory = false)
    private val readme = FileEntry("README.md", "$Root/README.md", isDirectory = false)
    private val guide = FileEntry("guide.md", "$Root/docs/guide.md", isDirectory = false)
    private val page = FileEntry("index.html", "$Root/index.html", isDirectory = false)

    private fun ComposeUiTest.count(tag: String) = onAllNodesWithTag(tag).fetchSemanticsNodes().size

    private fun ComposeUiTest.awaitTag(tag: String, count: Int = 1) {
        waitUntil(timeoutMillis = FrameTimeoutMillis) { count(tag) == count }
    }

    private fun ComposeUiTest.editorText(): String =
        onNodeWithTag(TerminalFileViewerEditorTestTag).fetchSemanticsNode().config[SemanticsProperties.EditableText].text

    /** 선택된 "Jarvis"(/work/jarvis) 패널에 셸 탭 하나와, 그 뒤에 고른 [path] 파일 탭. */
    private fun workspace(path: String = main.path) =
        FakeTerminalWorkspaceRepository(TerminalWorkspace.initial().addPanel(name = "Jarvis", directory = Root).addTab().openFile(path))

    private fun ComposeUiTest.openTerminal(
        files: FakeFileRepository,
        workspace: FakeTerminalWorkspaceRepository = workspace(),
        git: FakeGitChangesRepository = FakeGitChangesRepository(),
        uriHandler: RecordingUriHandler = RecordingUriHandler(),
        terminal: FakeTerminalRepository = FakeTerminalRepository(),
    ) {
        setContent { TestJarvisApp(terminal = terminal, terminalWorkspace = workspace, files = files, gitChanges = git, uriHandler = uriHandler) }
        onNodeWithTag(TerminalTestTag).performClick()
        awaitTag(terminalFileViewerTestTag(workspace.workspace.value.focusedTab!!.id))
    }

    private fun files(vararg contents: Pair<String, FileContent>, entries: List<FileEntry> = listOf(main, readme)) =
        FakeFileRepository(directories = mapOf(Root to entries), files = contents.toMap())

    private fun text(value: String) = FileContent.Text(value, truncated = false)

    // S1, S3
    @Test
    fun longTreesAndFilesGetAFastScrollerThatJumpsToTheEnd() = runComposeUiTest {
        val many = List(200) { FileEntry("f$it.txt", "$Root/f$it.txt", isDirectory = false) }
        val long = (1..500).joinToString("\n") { "line $it" }
        openTerminal(files(main.path to text(long), entries = many + main))
        awaitTag(TerminalFilesRootTestTag)

        awaitTag(TerminalFilesScrollerTestTag)
        awaitTag(TerminalFileViewerScrollerTestTag)
        assertEquals(0, count(terminalFileViewerGutterTestTag(500)))

        onNodeWithTag(TerminalFileViewerScrollerTestTag).performTouchInput {
            down(Offset(centerX, 1f))
            moveTo(Offset(centerX, height - 1f))
            up()
        }
        awaitTag(terminalFileViewerGutterTestTag(500))
        assertEquals(0, count(terminalFileViewerGutterTestTag(1)))

        // 막대의 빈 곳을 누르면 손잡이 가운데가 그 자리로 간다.
        onNodeWithTag(TerminalFileViewerScrollerTestTag).performTouchInput {
            down(Offset(centerX, 1f))
            up()
        }
        awaitTag(terminalFileViewerGutterTestTag(1))
    }

    // S1
    @Test
    fun shortFilesHaveNoScroller() = runComposeUiTest {
        openTerminal(files(main.path to text("fun main() {}")))
        onNodeWithText("fun main() {}").assertIsDisplayed()

        assertEquals(0, count(TerminalFileViewerScrollerTestTag))
        assertEquals(0, count(TerminalFilesScrollerTestTag))
    }

    // E1, E2, E3, E8
    @Test
    fun editingSavesWithTheButtonAndTheShortcut() = runComposeUiTest {
        val files = files(main.path to text("fun main() {}\n"))
        openTerminal(files)

        onNodeWithTag(TerminalFileViewerEditTestTag).performClick()
        awaitTag(TerminalFileViewerEditorTestTag)
        assertEquals("fun main() {}\n", editorText())
        onNodeWithTag(TerminalFileViewerSaveTestTag).assertIsNotEnabled()
        assertEquals(0, count(TerminalFileViewerDirtyTestTag))

        onNodeWithTag(TerminalFileViewerEditorTestTag).performTextReplacement("fun main() = Unit\n")
        awaitTag(TerminalFileViewerDirtyTestTag)
        onNodeWithTag(TerminalFileViewerSaveTestTag).assertIsEnabled().performClick()

        waitUntil(timeoutMillis = FrameTimeoutMillis) { files.written == listOf(main.path to "fun main() = Unit\n") }
        awaitTag(TerminalFileViewerDirtyTestTag, count = 0)
        onNodeWithTag(TerminalFileViewerEditorTestTag).assertIsDisplayed()

        onNodeWithTag(TerminalFileViewerEditorTestTag).performTextReplacement("fun main() = println()\n")
        awaitTag(TerminalFileViewerDirtyTestTag)
        onNodeWithTag(TerminalFileViewerEditorTestTag).performKeyInput { withKeyDown(Key.CtrlLeft) { pressKey(Key.S) } }
        waitUntil(timeoutMillis = FrameTimeoutMillis) { files.written.size == 2 }
        assertEquals(main.path to "fun main() = println()\n", files.written.last())

        onNodeWithTag(TerminalFileViewerCloseEditorTestTag).performClick()
        awaitTag(TerminalFileViewerEditorTestTag, count = 0)
        onNodeWithText("fun main() = println()").assertIsDisplayed()
    }

    // E4
    @Test
    fun aFailedSaveKeepsTheTextAndSaysWhy() = runComposeUiTest {
        val files = files(main.path to text("a")).apply { writeFailure = "권한 없음" }
        openTerminal(files)
        onNodeWithTag(TerminalFileViewerEditTestTag).performClick()
        awaitTag(TerminalFileViewerEditorTestTag)

        onNodeWithTag(TerminalFileViewerEditorTestTag).performTextReplacement("b")
        onNodeWithTag(TerminalFileViewerSaveTestTag).performClick()

        awaitTag(TerminalFileViewerEditErrorTestTag)
        onNodeWithTag(TerminalFileViewerEditErrorTestTag).assertTextEquals("저장하지 못했습니다: 권한 없음")
        onNodeWithTag(TerminalFileViewerDirtyTestTag).assertIsDisplayed()
        assertEquals("b", editorText())

        onNodeWithTag(TerminalFileViewerEditorTestTag).performTextReplacement("bc")
        awaitTag(TerminalFileViewerEditErrorTestTag, count = 0)
    }

    // E5
    @Test
    fun closingWithChangesAsksBeforeDiscarding() = runComposeUiTest {
        val files = files(main.path to text("원래"))
        openTerminal(files)

        onNodeWithTag(TerminalFileViewerEditTestTag).performClick()
        awaitTag(TerminalFileViewerEditorTestTag)
        onNodeWithTag(TerminalFileViewerCloseEditorTestTag).performClick()
        awaitTag(TerminalFileViewerEditorTestTag, count = 0)

        onNodeWithTag(TerminalFileViewerEditTestTag).performClick()
        awaitTag(TerminalFileViewerEditorTestTag)
        onNodeWithTag(TerminalFileViewerEditorTestTag).performTextReplacement("바꿈")
        onNodeWithTag(TerminalFileViewerCloseEditorTestTag).performClick()
        awaitTag(TerminalFileViewerKeepEditingTestTag)
        onNodeWithText("저장하지 않은 변경을 버릴까요?").assertIsDisplayed()

        onNodeWithTag(TerminalFileViewerKeepEditingTestTag).performClick()
        awaitTag(TerminalFileViewerKeepEditingTestTag, count = 0)
        assertEquals("바꿈", editorText())

        onNodeWithTag(TerminalFileViewerEditorTestTag).performKeyInput { pressKey(Key.Escape) }
        awaitTag(TerminalFileViewerDiscardTestTag)
        onNodeWithTag(TerminalFileViewerDiscardTestTag).performClick()
        awaitTag(TerminalFileViewerEditorTestTag, count = 0)
        onNodeWithText("원래").assertIsDisplayed()
        assertTrue(files.written.isEmpty())
    }

    // E6
    @Test
    fun diskChangesReplaceACleanBufferAndWarnOverADirtyOne() = runComposeUiTest {
        val files = files(main.path to text("v1"))
        openTerminal(files)
        onNodeWithTag(TerminalFileViewerEditTestTag).performClick()
        awaitTag(TerminalFileViewerEditorTestTag)

        files.files.value = mapOf(main.path to text("v2"))
        waitUntil(timeoutMillis = FrameTimeoutMillis) { editorText() == "v2" }
        assertEquals(0, count(TerminalFileViewerDiskChangedTestTag))

        onNodeWithTag(TerminalFileViewerEditorTestTag).performTextReplacement("mine")
        files.files.value = mapOf(main.path to text("v3"))
        awaitTag(TerminalFileViewerDiskChangedTestTag)
        assertEquals("mine", editorText())

        // 자기 저장 결과가 돌아와도 바뀐 것으로 치지 않는다.
        onNodeWithTag(TerminalFileViewerSaveTestTag).performClick()
        awaitTag(TerminalFileViewerDiskChangedTestTag, count = 0)
        awaitTag(TerminalFileViewerDirtyTestTag, count = 0)
        assertEquals("mine", editorText())
    }

    // E7
    @Test
    fun theDraftSurvivesSwitchingTabsAndIsDroppedWhenTheTabCloses() = runComposeUiTest {
        val workspace = workspace()
        openTerminal(files(main.path to text("a")), workspace = workspace)
        val fileTab = workspace.workspace.value.focusedTab!!
        val shellTab = workspace.workspace.value.selectedPanel!!.tabs.first { it.program != TerminalProgram.File }
        onNodeWithTag(TerminalFileViewerEditTestTag).performClick()
        awaitTag(TerminalFileViewerEditorTestTag)
        onNodeWithTag(TerminalFileViewerEditorTestTag).performTextReplacement("초안")

        workspace.workspace.value = workspace.workspace.value.selectTab(shellTab.id)
        awaitTag(TerminalFileViewerEditorTestTag, count = 0)
        workspace.workspace.value = workspace.workspace.value.selectTab(fileTab.id)
        awaitTag(TerminalFileViewerEditorTestTag)
        assertEquals("초안", editorText())
        onNodeWithTag(TerminalFileViewerDirtyTestTag).assertIsDisplayed()

        workspace.workspace.value = workspace.workspace.value.closeTab(fileTab.id).openFile(main.path)
        awaitTag(TerminalFileViewerEditTestTag)
        assertEquals(0, count(TerminalFileViewerEditorTestTag))
    }

    // E1
    @Test
    fun truncatedFilesAndCommitFileTabsCannotBeEdited() = runComposeUiTest {
        val git = FakeGitChangesRepository(
            commitFileContents = mapOf((main.path to "h1") to GitCommitFile(text("old"), GitFileDiff(emptyList()))),
        )
        val workspace = workspace()
        openTerminal(files(main.path to FileContent.Text("앞부분", truncated = true)), workspace = workspace, git = git)
        onNodeWithText("앞부분").assertIsDisplayed()
        assertEquals(0, count(TerminalFileViewerEditTestTag))

        workspace.workspace.value = workspace.workspace.value.openCommitFile(main.path, "h1")
        waitUntil(timeoutMillis = FrameTimeoutMillis) { onAllNodesWithText("old").fetchSemanticsNodes().size == 1 }
        assertEquals(0, count(TerminalFileViewerEditTestTag))
    }

    // M1, M3, M5
    @Test
    fun markdownOpensAsAPreviewWhoseLinksOpenBrowsersAndFiles() = runComposeUiTest {
        val uriHandler = RecordingUriHandler()
        val workspace = workspace(readme.path)
        val files = files(
            readme.path to text("# 제목\n\n[사이트](https://x.io) 와 [가이드](docs/guide.md)\n"),
            guide.path to text("가이드 본문"),
        )
        openTerminal(files, workspace = workspace, uriHandler = uriHandler)

        // 미리보기는 기본 디스패처에서 해석한 뒤 그린다.
        waitUntil(timeoutMillis = FrameTimeoutMillis) { onAllNodesWithText("제목", useUnmergedTree = true).fetchSemanticsNodes().size == 1 }
        assertEquals(0, onAllNodesWithText("# 제목").fetchSemanticsNodes().size)

        onNodeWithTag(TerminalFileViewerSourceTestTag).performClick()
        awaitTag(TerminalFileViewerMarkdownTestTag, count = 0)
        onNodeWithText("# 제목").assertIsDisplayed()

        // 편집을 마치면 들어가기 전의 보기로 돌아간다.
        onNodeWithTag(TerminalFileViewerPreviewTestTag).performClick()
        awaitTag(TerminalFileViewerMarkdownTestTag)
        waitUntil(timeoutMillis = FrameTimeoutMillis) { onAllNodesWithText("제목", useUnmergedTree = true).fetchSemanticsNodes().size == 1 }
        onNodeWithTag(TerminalFileViewerEditTestTag).performClick()
        awaitTag(TerminalFileViewerEditorTestTag)
        onNodeWithTag(TerminalFileViewerCloseEditorTestTag).performClick()
        awaitTag(TerminalFileViewerMarkdownTestTag)
        waitUntil(timeoutMillis = FrameTimeoutMillis) { onAllNodesWithText("사이트", substring = true, useUnmergedTree = true).fetchSemanticsNodes().size == 1 }

        onNodeWithText("사이트", substring = true, useUnmergedTree = true).performTouchInput { click(Offset(1f, centerY)) }
        waitUntil(timeoutMillis = FrameTimeoutMillis) { uriHandler.opened == listOf("https://x.io") }

        onNodeWithText("가이드", substring = true, useUnmergedTree = true).performTouchInput { click(Offset(width - 1f, centerY)) }
        waitUntil(timeoutMillis = FrameTimeoutMillis) { workspace.workspace.value.focusedTab?.filePath == guide.path }
    }

    // V3
    @Test
    fun theChosenViewSurvivesSwitchingTabs() = runComposeUiTest {
        val workspace = workspace(readme.path)
        openTerminal(files(readme.path to text("# 제목\n")), workspace = workspace)
        val fileTab = workspace.workspace.value.focusedTab!!
        val shellTab = workspace.workspace.value.selectedPanel!!.tabs.first { it.program != TerminalProgram.File }
        awaitTag(TerminalFileViewerMarkdownTestTag)
        onNodeWithTag(TerminalFileViewerSourceTestTag).performClick()
        awaitTag(TerminalFileViewerMarkdownTestTag, count = 0)

        workspace.workspace.value = workspace.workspace.value.selectTab(shellTab.id)
        awaitTag(terminalFileViewerTestTag(fileTab.id), count = 0)
        workspace.workspace.value = workspace.workspace.value.selectTab(fileTab.id)
        awaitTag(terminalFileViewerTestTag(fileTab.id))
        onNodeWithText("# 제목").assertIsDisplayed()
        assertEquals(0, count(TerminalFileViewerMarkdownTestTag))
    }

    // V1, V2, V8
    @Test
    fun htmlIsSourceOnlyWhereWebPagesCannotBeShownAndCodeHasNoViewButtons() = runComposeUiTest {
        val workspace = workspace(page.path)
        openTerminal(
            files(page.path to text("<h1>제목</h1>"), main.path to text("fun main() {}"), entries = listOf(main, page)),
            workspace = workspace,
            terminal = FakeTerminalRepository(isBrowserSupported = false),
        )
        onNodeWithText("<h1>제목</h1>").assertIsDisplayed()
        assertEquals(0, count(TerminalFileViewerPreviewTestTag))
        assertEquals(0, count(TerminalFileViewerSourceTestTag))
        assertEquals(0, count(TerminalFileViewerWebPreviewTestTag))
        onNodeWithTag(TerminalFileViewerEditTestTag).assertIsDisplayed()

        workspace.workspace.value = workspace.workspace.value.openFile(main.path)
        waitUntil(timeoutMillis = FrameTimeoutMillis) { onAllNodesWithText("fun main() {}").fetchSemanticsNodes().size == 1 }
        assertEquals(0, count(TerminalFileViewerPreviewTestTag))
        assertEquals(0, count(TerminalFileViewerSourceTestTag))
    }

    private companion object {
        const val FrameTimeoutMillis = 10_000L
        const val Root = "/work/jarvis"
    }
}
