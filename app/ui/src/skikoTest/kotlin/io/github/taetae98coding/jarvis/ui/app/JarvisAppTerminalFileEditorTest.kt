package io.github.taetae98coding.jarvis.ui.app

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
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
import io.github.taetae98coding.jarvis.ui.terminal.TerminalFileViewerDirtyTestTag
import io.github.taetae98coding.jarvis.ui.terminal.TerminalFileViewerDiskChangedTestTag
import io.github.taetae98coding.jarvis.ui.terminal.TerminalFileViewerEditErrorTestTag
import io.github.taetae98coding.jarvis.ui.terminal.TerminalFileViewerEditTestTag
import io.github.taetae98coding.jarvis.ui.terminal.TerminalFileViewerEditorTestTag
import io.github.taetae98coding.jarvis.ui.terminal.TerminalFileViewerMarkdownTestTag
import io.github.taetae98coding.jarvis.ui.terminal.TerminalFileViewerPreviewTestTag
import io.github.taetae98coding.jarvis.ui.terminal.TerminalFileViewerReadTestTag
import io.github.taetae98coding.jarvis.ui.terminal.TerminalFileViewerRevertTestTag
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
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeSource

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
        // 줄 번호 칸이 따로 있는 읽기 보기에서 어느 줄이 보이는지 센다.
        awaitTag(TerminalFileViewerReadTestTag)
        onNodeWithTag(TerminalFileViewerReadTestTag).performClick()

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

    // E1, E2, E3, E5, E8
    @Test
    // 자동 저장이 실제 시간(FileAutoSaveDelay)을 기다린다.
    @IgnoreOnWasm
    fun filesOpenInTheEditorAndSaveByThemselves() = runComposeUiTest {
        val files = files(main.path to text("fun main() {}\n"))
        openTerminal(files)

        awaitTag(TerminalFileViewerEditorTestTag)
        assertEquals("fun main() {}\n", editorText())
        assertEquals(0, count(TerminalFileViewerEditTestTag))
        assertEquals(0, count(TerminalFileViewerSaveTestTag))
        assertEquals(0, count(TerminalFileViewerDirtyTestTag))

        onNodeWithTag(TerminalFileViewerEditorTestTag).performTextReplacement("fun main() = Unit\n")
        awaitTag(TerminalFileViewerDirtyTestTag)
        assertTrue(files.written.isEmpty())

        waitUntil(timeoutMillis = FrameTimeoutMillis) { files.written == listOf(main.path to "fun main() = Unit\n") }
        awaitTag(TerminalFileViewerDirtyTestTag, count = 0)
        onNodeWithTag(TerminalFileViewerEditorTestTag).assertIsDisplayed()

        // 단축키는 기다리지 않는다.
        onNodeWithTag(TerminalFileViewerEditorTestTag).performTextReplacement("fun main() = println()\n")
        onNodeWithTag(TerminalFileViewerEditorTestTag).performKeyInput { withKeyDown(Key.CtrlLeft) { pressKey(Key.S) } }
        waitForIdle()
        assertEquals(listOf(main.path to "fun main() = Unit\n", main.path to "fun main() = println()\n"), files.written)

        // 읽기 보기로 바꾸면 기다리던 변경을 곧장 저장한다.
        onNodeWithTag(TerminalFileViewerEditorTestTag).performTextReplacement("fun main() = TODO()\n")
        onNodeWithTag(TerminalFileViewerReadTestTag).performClick()
        waitForIdle()
        assertEquals(main.path to "fun main() = TODO()\n", files.written.last())
        awaitTag(TerminalFileViewerEditorTestTag, count = 0)
        onNodeWithText("fun main() = TODO()").assertIsDisplayed()

        onNodeWithTag(TerminalFileViewerEditTestTag).performClick()
        awaitTag(TerminalFileViewerEditorTestTag)
        assertEquals("fun main() = TODO()\n", editorText())
        assertEquals(3, files.written.size)
    }

    // E4
    @Test
    // 자동 저장이 실제 시간(FileAutoSaveDelay)을 기다린다.
    @IgnoreOnWasm
    fun aFailedSaveKeepsTheTextSaysWhyAndCanBeRetried() = runComposeUiTest {
        val files = files(main.path to text("a")).apply { writeFailure = "권한 없음" }
        openTerminal(files)
        awaitTag(TerminalFileViewerEditorTestTag)

        onNodeWithTag(TerminalFileViewerEditorTestTag).performTextReplacement("b")
        awaitTag(TerminalFileViewerEditErrorTestTag)
        onNodeWithTag(TerminalFileViewerEditErrorTestTag).assertTextEquals("저장하지 못했습니다: 권한 없음")
        onNodeWithTag(TerminalFileViewerDirtyTestTag).assertIsDisplayed()
        assertEquals("b", editorText())

        // 다음 편집이 문구를 지우고 다시 자동 저장한다.
        onNodeWithTag(TerminalFileViewerEditorTestTag).performTextReplacement("bc")
        awaitTag(TerminalFileViewerEditErrorTestTag, count = 0)
        awaitTag(TerminalFileViewerEditErrorTestTag)
        assertEquals(listOf(main.path to "b", main.path to "bc"), files.written)

        files.writeFailure = null
        onNodeWithTag(TerminalFileViewerSaveTestTag).performClick()
        awaitTag(TerminalFileViewerDirtyTestTag, count = 0)
        assertEquals(0, count(TerminalFileViewerEditErrorTestTag))
        assertEquals(0, count(TerminalFileViewerSaveTestTag))
        assertEquals(main.path to "bc", files.written.last())
    }

    // E6
    @Test
    fun diskChangesReplaceACleanBufferAndStopAutoSaveOverADirtyOne() = runComposeUiTest {
        val files = files(main.path to text("v1"))
        openTerminal(files)
        awaitTag(TerminalFileViewerEditorTestTag)

        files.files.value = mapOf(main.path to text("v2"))
        waitUntil(timeoutMillis = FrameTimeoutMillis) { editorText() == "v2" }
        assertEquals(0, count(TerminalFileViewerDiskChangedTestTag))

        onNodeWithTag(TerminalFileViewerEditorTestTag).performTextReplacement("mine")
        files.files.value = mapOf(main.path to text("v3"))
        awaitTag(TerminalFileViewerDiskChangedTestTag)
        assertEquals("mine", editorText())
        waitPastAutoSave()
        assertTrue(files.written.isEmpty())

        onNodeWithTag(TerminalFileViewerRevertTestTag).performClick()
        waitUntil(timeoutMillis = FrameTimeoutMillis) { editorText() == "v3" }
        assertEquals(0, count(TerminalFileViewerDiskChangedTestTag))
        assertEquals(0, count(TerminalFileViewerDirtyTestTag))

        onNodeWithTag(TerminalFileViewerEditorTestTag).performTextReplacement("mine again")
        files.files.value = mapOf(main.path to text("v4"))
        awaitTag(TerminalFileViewerDiskChangedTestTag)

        // 자기 저장 결과가 돌아와도 바뀐 것으로 치지 않는다.
        onNodeWithTag(TerminalFileViewerSaveTestTag).performClick()
        awaitTag(TerminalFileViewerDiskChangedTestTag, count = 0)
        awaitTag(TerminalFileViewerDirtyTestTag, count = 0)
        assertEquals("mine again", editorText())
        assertEquals(listOf(main.path to "mine again"), files.written)
    }

    // E7
    @Test
    fun theDraftSurvivesSwitchingTabsAndIsSavedWhenTheTabCloses() = runComposeUiTest {
        val workspace = workspace()
        val files = files(main.path to text("a"))
        openTerminal(files, workspace = workspace)
        val fileTab = workspace.workspace.value.focusedTab!!
        val shellTab = workspace.workspace.value.selectedPanel!!.tabs.first { it.program != TerminalProgram.File }
        awaitTag(TerminalFileViewerEditorTestTag)
        onNodeWithTag(TerminalFileViewerEditorTestTag).performTextReplacement("초안")

        workspace.workspace.value = workspace.workspace.value.selectTab(shellTab.id)
        awaitTag(TerminalFileViewerEditorTestTag, count = 0)
        workspace.workspace.value = workspace.workspace.value.selectTab(fileTab.id)
        awaitTag(TerminalFileViewerEditorTestTag)
        assertEquals("초안", editorText())

        onNodeWithTag(TerminalFileViewerEditorTestTag).performTextReplacement("초안 둘")
        workspace.workspace.value = workspace.workspace.value.closeTab(fileTab.id)
        waitUntil(timeoutMillis = FrameTimeoutMillis) { files.written.lastOrNull() == main.path to "초안 둘" }

        workspace.workspace.value = workspace.workspace.value.openFile(main.path)
        awaitTag(TerminalFileViewerEditorTestTag)
        assertEquals("초안 둘", editorText())
        assertEquals(0, count(TerminalFileViewerDirtyTestTag))
    }

    // E1
    @Test
    fun truncatedFilesAndCommitFileTabsOnlyHaveTheReadView() = runComposeUiTest {
        val git = FakeGitChangesRepository(
            commitFileContents = mapOf((main.path to "h1") to GitCommitFile(text("old"), GitFileDiff(emptyList()))),
        )
        val workspace = workspace()
        openTerminal(files(main.path to FileContent.Text("앞부분", truncated = true)), workspace = workspace, git = git)
        onNodeWithText("앞부분").assertIsDisplayed()
        assertEquals(0, count(TerminalFileViewerEditorTestTag))
        assertEquals(0, count(TerminalFileViewerEditTestTag))

        workspace.workspace.value = workspace.workspace.value.openCommitFile(main.path, "h1")
        waitUntil(timeoutMillis = FrameTimeoutMillis) { onAllNodesWithText("old").fetchSemanticsNodes().size == 1 }
        assertEquals(0, count(TerminalFileViewerEditorTestTag))
        assertEquals(0, count(TerminalFileViewerEditTestTag))
    }

    // M1, M3, M5
    @Test
    // 마크다운 파싱이 Dispatchers.Default 에서 돈다. Wasm 에서는 waitUntil 이 막은 이벤트 루프와 같다.
    @IgnoreOnWasm
    fun markdownOpensAsAPreviewAndItsSourceIsTheEditor() = runComposeUiTest {
        val uriHandler = RecordingUriHandler()
        val workspace = workspace(readme.path)
        val source = "# 제목\n\n[사이트](https://x.io) 와 [가이드](docs/guide.md)\n"
        val files = files(readme.path to text(source), guide.path to text("가이드 본문"))
        openTerminal(files, workspace = workspace, uriHandler = uriHandler)

        // 미리보기는 기본 디스패처에서 해석한 뒤 그린다.
        waitUntil(timeoutMillis = FrameTimeoutMillis) { onAllNodesWithText("제목", useUnmergedTree = true).fetchSemanticsNodes().size == 1 }
        assertEquals(0, onAllNodesWithText("# 제목").fetchSemanticsNodes().size)
        assertEquals(0, count(TerminalFileViewerEditTestTag))
        assertEquals(0, count(TerminalFileViewerEditorTestTag))

        onNodeWithTag(TerminalFileViewerSourceTestTag).performClick()
        awaitTag(TerminalFileViewerEditorTestTag)
        assertEquals(source, editorText())

        // 미리보기로 돌아가면 기다리던 변경을 곧장 저장한다.
        onNodeWithTag(TerminalFileViewerEditorTestTag).performTextReplacement("# 새 제목\n\n[사이트](https://x.io) 와 [가이드](docs/guide.md)\n")
        onNodeWithTag(TerminalFileViewerPreviewTestTag).performClick()
        waitForIdle()
        assertEquals(1, files.written.size)
        awaitTag(TerminalFileViewerMarkdownTestTag)
        waitUntil(timeoutMillis = FrameTimeoutMillis) { onAllNodesWithText("새 제목", useUnmergedTree = true).fetchSemanticsNodes().size == 1 }

        onNodeWithText("사이트", substring = true, useUnmergedTree = true).performTouchInput { click(Offset(1f, centerY)) }
        waitUntil(timeoutMillis = FrameTimeoutMillis) { uriHandler.opened == listOf("https://x.io") }

        onNodeWithText("가이드", substring = true, useUnmergedTree = true).performTouchInput { click(Offset(width - 1f, centerY)) }
        waitUntil(timeoutMillis = FrameTimeoutMillis) { workspace.workspace.value.focusedTab?.filePath == guide.path }
    }

    /** 자동 저장이 돌았을 만큼 기다린다. 앱의 FileAutoSaveDelay(1초)보다 넉넉하게 잡는다. */
    // V3
    @Test
    fun theChosenViewSurvivesSwitchingTabs() = runComposeUiTest {
        val workspace = workspace(readme.path)
        openTerminal(files(readme.path to text("# 제목\n")), workspace = workspace)
        val fileTab = workspace.workspace.value.focusedTab!!
        val shellTab = workspace.workspace.value.selectedPanel!!.tabs.first { it.program != TerminalProgram.File }
        awaitTag(TerminalFileViewerMarkdownTestTag)
        onNodeWithTag(TerminalFileViewerSourceTestTag).performClick()
        awaitTag(TerminalFileViewerEditorTestTag)

        workspace.workspace.value = workspace.workspace.value.selectTab(shellTab.id)
        awaitTag(terminalFileViewerTestTag(fileTab.id), count = 0)
        workspace.workspace.value = workspace.workspace.value.selectTab(fileTab.id)
        awaitTag(TerminalFileViewerEditorTestTag)
        assertEquals("# 제목\n", editorText())
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
        awaitTag(TerminalFileViewerEditorTestTag)
        assertEquals("<h1>제목</h1>", editorText())
        assertEquals(0, count(TerminalFileViewerPreviewTestTag))
        assertEquals(0, count(TerminalFileViewerSourceTestTag))
        assertEquals(0, count(TerminalFileViewerWebPreviewTestTag))

        workspace.workspace.value = workspace.workspace.value.openFile(main.path)
        waitUntil(timeoutMillis = FrameTimeoutMillis) { count(TerminalFileViewerEditorTestTag) == 1 && editorText() == "fun main() {}" }
        assertEquals(0, count(TerminalFileViewerPreviewTestTag))
        assertEquals(0, count(TerminalFileViewerSourceTestTag))
    }

    private fun ComposeUiTest.waitPastAutoSave() {
        val until = TimeSource.Monotonic.markNow() + 1_500.milliseconds
        waitUntil(timeoutMillis = FrameTimeoutMillis) { until.hasPassedNow() }
    }

    private companion object {
        const val FrameTimeoutMillis = 10_000L
        const val Root = "/work/jarvis"
    }
}
