package io.github.taetae98coding.jarvis.ui.app

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import io.github.taetae98coding.jarvis.domain.terminal.FileContent
import io.github.taetae98coding.jarvis.domain.terminal.FileEntry
import io.github.taetae98coding.jarvis.domain.terminal.GitChange
import io.github.taetae98coding.jarvis.domain.terminal.GitChangeKind
import io.github.taetae98coding.jarvis.domain.terminal.GitCommit
import io.github.taetae98coding.jarvis.domain.terminal.GitDiffHunk
import io.github.taetae98coding.jarvis.domain.terminal.GitFileDiff
import io.github.taetae98coding.jarvis.domain.terminal.GitGraphLine
import io.github.taetae98coding.jarvis.domain.terminal.GitPushTarget
import io.github.taetae98coding.jarvis.domain.terminal.GitStatus
import io.github.taetae98coding.jarvis.domain.terminal.TerminalProgram
import io.github.taetae98coding.jarvis.domain.terminal.TerminalWorkspace
import io.github.taetae98coding.jarvis.ui.terminal.TerminalFileViewerDiffSummaryTestTag
import io.github.taetae98coding.jarvis.ui.terminal.TerminalFileViewerNoticeTestTag
import io.github.taetae98coding.jarvis.ui.terminal.TerminalFilesRootTestTag
import io.github.taetae98coding.jarvis.ui.terminal.TerminalGitBranchTestTag
import io.github.taetae98coding.jarvis.ui.terminal.TerminalGitCommitFilesEmptyTestTag
import io.github.taetae98coding.jarvis.ui.terminal.TerminalGitCommitFilesTestTag
import io.github.taetae98coding.jarvis.ui.terminal.TerminalGitCommitFilesUnreadableTestTag
import io.github.taetae98coding.jarvis.ui.terminal.TerminalGitErrorTestTag
import io.github.taetae98coding.jarvis.ui.terminal.TerminalGitNoCommitsTestTag
import io.github.taetae98coding.jarvis.ui.terminal.TerminalGitNotRepositoryTestTag
import io.github.taetae98coding.jarvis.ui.terminal.TerminalGitPushTargetTestTag
import io.github.taetae98coding.jarvis.ui.terminal.TerminalGitPushTestTag
import io.github.taetae98coding.jarvis.ui.terminal.TerminalGitStageAllTestTag
import io.github.taetae98coding.jarvis.ui.terminal.TerminalGitUnstageAllTestTag
import io.github.taetae98coding.jarvis.ui.terminal.TerminalSideBarContentTestTag
import io.github.taetae98coding.jarvis.ui.terminal.TerminalSideBarFilesTestTag
import io.github.taetae98coding.jarvis.ui.terminal.TerminalSideBarGitTestTag
import io.github.taetae98coding.jarvis.ui.terminal.TerminalSideBarNoFolderTestTag
import io.github.taetae98coding.jarvis.ui.terminal.TerminalSideBarTestTag
import io.github.taetae98coding.jarvis.ui.terminal.TerminalSideBarToggleTestTag
import io.github.taetae98coding.jarvis.ui.terminal.TerminalTestTag
import io.github.taetae98coding.jarvis.ui.terminal.terminalFileEntryTestTag
import io.github.taetae98coding.jarvis.ui.terminal.terminalFileViewerAddedTestTag
import io.github.taetae98coding.jarvis.ui.terminal.terminalFileViewerRemovedTestTag
import io.github.taetae98coding.jarvis.ui.terminal.terminalFileViewerTestTag
import io.github.taetae98coding.jarvis.ui.terminal.terminalGitCommitFileTestTag
import io.github.taetae98coding.jarvis.ui.terminal.terminalGitCommitTestTag
import io.github.taetae98coding.jarvis.ui.terminal.terminalGitStageTestTag
import io.github.taetae98coding.jarvis.ui.terminal.terminalGitStagedTestTag
import io.github.taetae98coding.jarvis.ui.terminal.terminalGitUnstageTestTag
import io.github.taetae98coding.jarvis.ui.terminal.terminalGitUnstagedTestTag
import io.github.taetae98coding.jarvis.ui.terminal.terminalGroupTestTag
import io.github.taetae98coding.jarvis.ui.terminal.terminalTabTestTag
import io.github.taetae98coding.jarvis.ui.terminal.terminalTabTitleTestTag
import kotlinx.coroutines.CompletableDeferred
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** docs/common/terminal-side-bar.html */
@OptIn(ExperimentalTestApi::class)
class JarvisAppTerminalSideBarTest {
    // "패널 1"(폴더 없음)과 선택된 "Jarvis"(/work/jarvis, 셸 탭 하나).
    private val initial = TerminalWorkspace.initial()
        .addPanel(name = "Jarvis", directory = Root)
        .addTab()

    private val readme = FileEntry("README.md", "$Root/README.md", isDirectory = false)
    private val src = FileEntry("src", "$Root/src", isDirectory = true)
    private val main = FileEntry("Main.kt", "$Root/src/Main.kt", isDirectory = false)

    private fun files(vararg contents: Pair<String, FileContent>) = FakeFileRepository(
        directories = mapOf(Root to listOf(src, readme), src.path to listOf(main)),
        files = contents.toMap(),
    )

    private val foo = GitChange("app/Foo.kt", GitChangeKind.Modified)
    private val tmp = GitChange("tmp.txt", GitChangeKind.Untracked)
    private val added = GitChange("new.kt", GitChangeKind.Added)
    private val head = GitCommit("h1", "a1b2c3d", listOf("HEAD -> main"), "dev", "2026-09-25 10:00", "사이드 바 추가")
    private val first = GitCommit("h2", "e4f5a6b", emptyList(), "dev", "2026-09-20 09:00", "처음")

    private val renamed = GitChange("app/New.kt", GitChangeKind.Renamed, "app/Old.kt")

    private fun git() = FakeGitChangesRepository(
        statuses = mapOf(Root to GitStatus(root = Root, branch = "main", staged = listOf(added), unstaged = listOf(foo, tmp))),
        graphs = mapOf(
            Root to listOf(
                GitGraphLine("*", head),
                GitGraphLine("|", head, isDetail = true),
                GitGraphLine("*", first),
                GitGraphLine("", first, isDetail = true),
            ),
        ),
        commitFiles = mapOf(head.hash to listOf(foo, renamed), first.hash to emptyList()),
    )

    private fun ComposeUiTest.count(tag: String) = onAllNodesWithTag(tag).fetchSemanticsNodes().size

    private fun ComposeUiTest.awaitTag(tag: String, count: Int = 1) {
        waitUntil(timeoutMillis = FrameTimeoutMillis) { count(tag) == count }
    }

    private fun ComposeUiTest.openTerminal(
        workspace: FakeTerminalWorkspaceRepository = FakeTerminalWorkspaceRepository(initial),
        terminal: FakeTerminalRepository = FakeTerminalRepository(),
        files: FakeFileRepository = files(),
        git: FakeGitChangesRepository = git(),
    ) {
        setContent { TestJarvisApp(terminal = terminal, terminalWorkspace = workspace, files = files, gitChanges = git) }
        onNodeWithTag(TerminalTestTag).performClick()
        waitUntil(timeoutMillis = FrameTimeoutMillis) { terminal.sessions.size == 1 }
    }

    @Test
    fun theSideBarStartsOpenOnFilesAndCollapsesAndExpandsWithoutTouchingTabs() = runComposeUiTest {
        val terminal = FakeTerminalRepository()
        val workspace = FakeTerminalWorkspaceRepository(initial)
        openTerminal(workspace = workspace, terminal = terminal)
        val tabId = workspace.workspace.value.focusedTab!!.id

        onNodeWithTag(TerminalSideBarTestTag).assertIsDisplayed()
        awaitTag(TerminalFilesRootTestTag)
        val groupTag = terminalGroupTestTag(workspace.workspace.value.focusedGroup!!.id)
        val openWidth = onNodeWithTag(groupTag).fetchSemanticsNode().boundsInRoot.width

        onNodeWithTag(TerminalSideBarToggleTestTag).performClick()
        awaitTag(TerminalSideBarContentTestTag, count = 0)
        onNodeWithTag(terminalTabTestTag(tabId)).assertIsDisplayed()
        assertFalse(terminal.sessions.single().closed)
        assertTrue(onNodeWithTag(groupTag).fetchSemanticsNode().boundsInRoot.width > openWidth)

        onNodeWithTag(TerminalSideBarToggleTestTag).performClick()
        awaitTag(TerminalFilesRootTestTag)
    }

    @Test
    fun sectionIconsSwitchTheSectionAndTheOpenOneCollapses() = runComposeUiTest {
        openTerminal()
        awaitTag(TerminalFilesRootTestTag)

        onNodeWithTag(TerminalSideBarGitTestTag).performClick()
        awaitTag(TerminalGitBranchTestTag)
        assertEquals(0, count(TerminalFilesRootTestTag))

        onNodeWithTag(TerminalSideBarGitTestTag).performClick()
        awaitTag(TerminalSideBarContentTestTag, count = 0)

        onNodeWithTag(TerminalSideBarFilesTestTag).performClick()
        awaitTag(TerminalFilesRootTestTag)
    }

    @Test
    fun aPanelWithoutAFolderSaysSo() = runComposeUiTest {
        openTerminal(workspace = FakeTerminalWorkspaceRepository(TerminalWorkspace.initial()))

        awaitTag(TerminalSideBarNoFolderTestTag)

        onNodeWithTag(TerminalSideBarGitTestTag).performClick()
        awaitTag(TerminalSideBarNoFolderTestTag)
    }

    @Test
    fun foldersExpandAndCollapseInPlace() = runComposeUiTest {
        openTerminal()
        awaitTag(terminalFileEntryTestTag(readme.path))
        onNodeWithTag(TerminalFilesRootTestTag).assertTextEquals(Root)
        val srcTop = onNodeWithTag(terminalFileEntryTestTag(src.path)).fetchSemanticsNode().boundsInRoot.top
        val readmeTop = onNodeWithTag(terminalFileEntryTestTag(readme.path)).fetchSemanticsNode().boundsInRoot.top
        assertTrue(srcTop < readmeTop)
        assertEquals(0, count(terminalFileEntryTestTag(main.path)))

        onNodeWithTag(terminalFileEntryTestTag(src.path)).performClick()
        awaitTag(terminalFileEntryTestTag(main.path))
        val mainNode = onNodeWithTag(terminalFileEntryTestTag(main.path)).fetchSemanticsNode().boundsInRoot
        assertTrue(mainNode.top < onNodeWithTag(terminalFileEntryTestTag(readme.path)).fetchSemanticsNode().boundsInRoot.top)

        onNodeWithTag(terminalFileEntryTestTag(src.path)).performClick()
        awaitTag(terminalFileEntryTestTag(main.path), count = 0)
    }

    @Test
    fun openingAFolderWithOnlyOneFolderOpensTheWholeChain() = runComposeUiTest {
        val a = FileEntry("a", "$Root/a", isDirectory = true)
        val b = FileEntry("b", "${a.path}/b", isDirectory = true)
        val c = FileEntry("c", "${b.path}/c", isDirectory = true)
        val d = FileEntry("d", "${c.path}/d", isDirectory = true)
        val e = FileEntry("e", "${d.path}/e", isDirectory = true)
        val f = FileEntry("f", "${e.path}/f", isDirectory = true)
        val notes = FileEntry("NOTES.md", "${d.path}/NOTES.md", isDirectory = false)
        val files = FakeFileRepository(
            directories = mapOf(Root to listOf(a, readme), a.path to listOf(b), b.path to listOf(c), c.path to listOf(d), d.path to listOf(e, notes), e.path to listOf(f)),
            files = emptyMap(),
        )
        openTerminal(files = files)
        awaitTag(terminalFileEntryTestTag(a.path))

        onNodeWithTag(terminalFileEntryTestTag(a.path)).performClick()

        awaitTag(terminalFileEntryTestTag(notes.path))
        awaitTag(terminalFileEntryTestTag(e.path))
        // d 는 폴더와 파일을 함께 가지므로 거기서 멈춘다.
        assertEquals(0, count(terminalFileEntryTestTag(f.path)))
        onNodeWithTag(terminalFileEntryTestTag(a.path)).assertTextEquals("a/b/c/d")
        listOf(b, c, d).forEach { assertEquals(0, count(terminalFileEntryTestTag(it.path))) }
        val chainLeft = onNodeWithTag(terminalFileEntryTestTag(a.path)).fetchSemanticsNode().boundsInRoot.left
        val readmeLeft = onNodeWithTag(terminalFileEntryTestTag(readme.path)).fetchSemanticsNode().boundsInRoot.left
        assertEquals(readmeLeft, chainLeft)

        onNodeWithTag(terminalFileEntryTestTag(a.path)).performClick()
        awaitTag(terminalFileEntryTestTag(notes.path), count = 0)
        onNodeWithTag(terminalFileEntryTestTag(a.path)).assertTextEquals("a")
        onNodeWithTag(terminalFileEntryTestTag(a.path)).performClick()
        awaitTag(terminalFileEntryTestTag(notes.path))
        onNodeWithTag(terminalFileEntryTestTag(a.path)).assertTextEquals("a/b/c/d")
    }

    @Test
    fun aChainEndingInACollapsedFolderOpensThatFolder() = runComposeUiTest {
        val a = FileEntry("a", "$Root/a", isDirectory = true)
        val b = FileEntry("b", "${a.path}/b", isDirectory = true)
        val note = FileEntry("note.txt", "${a.path}/note.txt", isDirectory = false)
        val inner = FileEntry("Inner.kt", "${b.path}/Inner.kt", isDirectory = false)
        val files = FakeFileRepository(
            directories = mapOf(Root to listOf(a, readme), a.path to listOf(b, note), b.path to listOf(inner)),
            files = emptyMap(),
        )
        openTerminal(files = files)
        awaitTag(terminalFileEntryTestTag(a.path))

        onNodeWithTag(terminalFileEntryTestTag(a.path)).performClick()
        awaitTag(terminalFileEntryTestTag(b.path))

        // 파일이 사라지면 a 는 접힌 b 하나뿐이 되어 한 줄로 합쳐진다.
        files.directories.value = files.directories.value + (a.path to listOf(b))
        awaitTag(terminalFileEntryTestTag(b.path), count = 0)
        onNodeWithTag(terminalFileEntryTestTag(a.path)).assertTextEquals("a/b")
        assertEquals(0, count(terminalFileEntryTestTag(inner.path)))

        onNodeWithTag(terminalFileEntryTestTag(a.path)).performClick()
        awaitTag(terminalFileEntryTestTag(inner.path))

        onNodeWithTag(terminalFileEntryTestTag(a.path)).performClick()
        awaitTag(terminalFileEntryTestTag(inner.path), count = 0)
        onNodeWithTag(terminalFileEntryTestTag(a.path)).assertTextEquals("a")
    }

    @Test
    fun theTreeFollowsTheDisk() = runComposeUiTest {
        val files = files()
        openTerminal(files = files)
        awaitTag(terminalFileEntryTestTag(readme.path))

        val added = FileEntry("NOTES.md", "$Root/NOTES.md", isDirectory = false)
        files.directories.value = files.directories.value + (Root to listOf(src, added, readme))

        awaitTag(terminalFileEntryTestTag(added.path))
    }

    @Test
    fun clickingAFileOpensItInANewTabAndClickingAgainSelectsThatTab() = runComposeUiTest {
        val workspace = FakeTerminalWorkspaceRepository(initial)
        openTerminal(workspace = workspace, files = files(readme.path to FileContent.Text("# Jarvis\n\n끝", truncated = false)))
        awaitTag(terminalFileEntryTestTag(readme.path))

        onNodeWithTag(terminalFileEntryTestTag(readme.path)).performClick()

        waitUntil(timeoutMillis = FrameTimeoutMillis) { workspace.workspace.value.focusedTab?.program == TerminalProgram.File }
        val fileTab = workspace.workspace.value.focusedTab!!
        assertEquals(readme.path, fileTab.filePath)
        assertEquals(2, workspace.workspace.value.selectedPanel!!.tabs.size)
        awaitTag(terminalFileViewerTestTag(fileTab.id))
        onNodeWithText("# Jarvis").assertIsDisplayed()
        onNodeWithText("끝").assertIsDisplayed()
        onNodeWithText("3").assertIsDisplayed()
        onNodeWithTag(terminalTabTitleTestTag(fileTab.id)).assertTextEquals("README.md")

        workspace.workspace.value = workspace.workspace.value.selectTab(workspace.workspace.value.selectedPanel!!.tabs.first().id)
        onNodeWithTag(terminalFileEntryTestTag(readme.path)).performClick()

        waitUntil(timeoutMillis = FrameTimeoutMillis) { workspace.workspace.value.focusedTab?.id == fileTab.id }
        assertEquals(2, workspace.workspace.value.selectedPanel!!.tabs.size)
    }

    @Test
    fun binaryBigAndMissingFilesSaySo() = runComposeUiTest {
        val workspace = FakeTerminalWorkspaceRepository(initial)
        val big = FileEntry("big.log", "$Root/big.log", isDirectory = false)
        val files = FakeFileRepository(
            directories = mapOf(Root to listOf(readme, big, main)),
            files = mapOf(readme.path to FileContent.Binary, big.path to FileContent.Text("앞부분", truncated = true)),
        )
        openTerminal(workspace = workspace, files = files)
        awaitTag(terminalFileEntryTestTag(readme.path))

        onNodeWithTag(terminalFileEntryTestTag(readme.path)).performClick()
        awaitTag(TerminalFileViewerNoticeTestTag)
        onNodeWithTag(TerminalFileViewerNoticeTestTag).assertTextEquals("텍스트가 아닌 파일이라 보일 수 없습니다")

        onNodeWithTag(terminalFileEntryTestTag(big.path)).performClick()
        waitUntil(timeoutMillis = FrameTimeoutMillis) { onAllNodesWithText("앞 512 KiB 만 보입니다").fetchSemanticsNodes().size == 1 }
        onNodeWithText("앞부분").assertIsDisplayed()

        onNodeWithTag(terminalFileEntryTestTag(main.path)).performClick()
        waitUntil(timeoutMillis = FrameTimeoutMillis) { onAllNodesWithText("파일을 읽을 수 없습니다").fetchSemanticsNodes().size == 1 }
    }

    // docs/common/terminal-file-diff.html D1, D2, D4, D5, D6
    @Test
    fun fileTabsOverlayTheDiffAgainstHead() = runComposeUiTest {
        val git = git()
        git.diffs.value = mapOf(
            // "a b c d" → "a B c d e"
            readme.path to GitFileDiff(
                listOf(
                    GitDiffHunk(oldStart = 2, oldCount = 1, newStart = 2, newCount = 1, removed = listOf("b")),
                    GitDiffHunk(oldStart = 4, oldCount = 0, newStart = 5, newCount = 1, removed = emptyList()),
                ),
            ),
            main.path to GitFileDiff(listOf(GitDiffHunk(oldStart = 1, oldCount = 2, newStart = 0, newCount = 0, removed = listOf("fun main()", "}")))),
        )
        openTerminal(files = files(readme.path to FileContent.Text("a\nB\nc\nd\ne", truncated = false)), git = git)
        awaitTag(terminalFileEntryTestTag(readme.path))

        onNodeWithTag(terminalFileEntryTestTag(readme.path)).performClick()
        awaitTag(TerminalFileViewerDiffSummaryTestTag)
        onNodeWithTag(TerminalFileViewerDiffSummaryTestTag).assertTextEquals("HEAD 대비 +2 −1")
        onNodeWithTag(terminalFileViewerAddedTestTag(2)).assertIsDisplayed()
        onNodeWithTag(terminalFileViewerAddedTestTag(5)).assertIsDisplayed()
        onNodeWithTag(terminalFileViewerRemovedTestTag(2)).assertIsDisplayed()
        onNodeWithText("b").assertIsDisplayed()
        assertEquals(0, count(terminalFileViewerAddedTestTag(1)))
        assertTrue(
            onNodeWithTag(terminalFileViewerRemovedTestTag(2)).fetchSemanticsNode().boundsInRoot.top <
                onNodeWithTag(terminalFileViewerAddedTestTag(2)).fetchSemanticsNode().boundsInRoot.top,
        )

        // 커밋해 HEAD 와 같아지면 표시가 사라진다.
        git.diffs.value = git.diffs.value + (readme.path to GitFileDiff(emptyList()))
        awaitTag(TerminalFileViewerDiffSummaryTestTag, count = 0)
        assertEquals(0, count(terminalFileViewerAddedTestTag(2)))
        assertEquals(0, onAllNodesWithText("b").fetchSemanticsNodes().size)

        // 디스크에서 지운 파일은 HEAD 의 줄이 모두 지운 줄이다.
        onNodeWithTag(terminalFileEntryTestTag(src.path)).performClick()
        awaitTag(terminalFileEntryTestTag(main.path))
        onNodeWithTag(terminalFileEntryTestTag(main.path)).performClick()
        awaitTag(terminalFileViewerRemovedTestTag(1))
        onNodeWithTag(TerminalFileViewerDiffSummaryTestTag).assertTextEquals("HEAD 대비 +0 −2")
        onNodeWithText("fun main()").assertIsDisplayed()
        assertEquals(0, onAllNodesWithText("파일을 읽을 수 없습니다").fetchSemanticsNodes().size)
    }

    @Test
    fun theGitSectionListsStagedAndUnstagedChangesAndTheGraph() = runComposeUiTest {
        openTerminal()
        onNodeWithTag(TerminalSideBarGitTestTag).performClick()
        awaitTag(TerminalGitBranchTestTag)

        onNodeWithTag(TerminalGitBranchTestTag).assertTextEquals("main")
        onNodeWithText("스테이지된 변경 (1)").assertIsDisplayed()
        onNodeWithText("변경 (2)").assertIsDisplayed()
        onNodeWithTag(terminalGitStagedTestTag(added.path)).assertIsDisplayed()
        onNodeWithTag(terminalGitUnstagedTestTag(foo.path)).assertIsDisplayed()
        onNodeWithTag(terminalGitUnstagedTestTag(tmp.path)).assertIsDisplayed()
        onNodeWithText("Foo.kt").assertIsDisplayed()
        onNodeWithText("app").assertIsDisplayed()

        awaitTag(terminalGitCommitTestTag(head.hash))
        onNodeWithText("사이드 바 추가").assertIsDisplayed()
        onNodeWithText("HEAD -> main").assertIsDisplayed()
        onNodeWithText("a1b2c3d · dev · 2026-09-25 10:00").assertIsDisplayed()
        onNodeWithTag(terminalGitCommitTestTag(first.hash)).assertIsDisplayed()
    }

    @Test
    fun clickingACommitExpandsItsFilesAndOnlyOneCommitIsOpenAtATime() = runComposeUiTest {
        openTerminal()
        onNodeWithTag(TerminalSideBarGitTestTag).performClick()
        awaitTag(terminalGitCommitTestTag(head.hash))
        assertEquals(0, count(TerminalGitCommitFilesTestTag))

        onNodeWithTag(terminalGitCommitTestTag(head.hash)).performClick()

        awaitTag(terminalGitCommitFileTestTag(foo.path))
        onNodeWithText("파일 2개").assertIsDisplayed()
        onNodeWithTag(terminalGitCommitFileTestTag(renamed.path)).assertIsDisplayed()
        onNodeWithText("New.kt").assertIsDisplayed()
        assertEquals(1, count(TerminalGitCommitFilesTestTag))

        // 둘째 줄을 눌러도 같은 커밋이라 접힌다.
        onNodeWithText("a1b2c3d · dev · 2026-09-25 10:00").performClick()
        awaitTag(TerminalGitCommitFilesTestTag, count = 0)

        onNodeWithTag(terminalGitCommitTestTag(head.hash)).performClick()
        awaitTag(terminalGitCommitFileTestTag(foo.path))
        onNodeWithTag(terminalGitCommitTestTag(first.hash)).performClick()

        awaitTag(TerminalGitCommitFilesEmptyTestTag)
        assertEquals(0, count(terminalGitCommitFileTestTag(foo.path)))
        assertEquals(1, count(TerminalGitCommitFilesTestTag))
        onNodeWithText("바뀐 파일이 없습니다").assertIsDisplayed()
    }

    @Test
    fun anUnreadableCommitSaysSoAndSwitchingThePanelCollapsesIt() = runComposeUiTest {
        val workspace = FakeTerminalWorkspaceRepository(initial)
        val git = git().apply { commitFiles.value = emptyMap() }
        openTerminal(workspace = workspace, git = git)
        onNodeWithTag(TerminalSideBarGitTestTag).performClick()
        awaitTag(terminalGitCommitTestTag(head.hash))

        onNodeWithTag(terminalGitCommitTestTag(head.hash)).performClick()
        awaitTag(TerminalGitCommitFilesUnreadableTestTag)

        val other = "/work/other"
        git.statuses.value = git.statuses.value + (other to git.statuses.value.getValue(Root).copy(root = other))
        git.graphs.value = git.graphs.value + (other to git.graphs.value.getValue(Root))
        workspace.workspace.value = workspace.workspace.value.addPanel(name = "Other", directory = other)

        awaitTag(TerminalGitCommitFilesTestTag, count = 0)
        onNodeWithTag(terminalGitCommitTestTag(head.hash)).assertIsDisplayed()
    }

    @Test
    fun stageAndUnstageButtonsCallGitWithTheRepositoryRoot() = runComposeUiTest {
        val git = git()
        openTerminal(git = git)
        onNodeWithTag(TerminalSideBarGitTestTag).performClick()
        awaitTag(terminalGitStageTestTag(foo.path))

        onNodeWithTag(terminalGitStageTestTag(foo.path)).performClick()
        waitUntil(timeoutMillis = FrameTimeoutMillis) { git.staged.size == 1 }
        assertEquals(Root to listOf(foo), git.staged.single())

        onNodeWithTag(terminalGitUnstageTestTag(added.path)).performClick()
        waitUntil(timeoutMillis = FrameTimeoutMillis) { git.unstaged.size == 1 }
        assertEquals(Root to listOf(added), git.unstaged.single())

        onNodeWithTag(TerminalGitStageAllTestTag).performClick()
        waitUntil(timeoutMillis = FrameTimeoutMillis) { git.staged.size == 2 }
        assertEquals(Root to listOf(foo, tmp), git.staged.last())

        onNodeWithTag(TerminalGitUnstageAllTestTag).performClick()
        waitUntil(timeoutMillis = FrameTimeoutMillis) { git.unstaged.size == 2 }
        assertEquals(Root to listOf(added), git.unstaged.last())
        assertEquals(0, count(TerminalGitErrorTestTag))
    }

    @Test
    fun aGitFailureStaysUntilTheNextCommandSucceeds() = runComposeUiTest {
        val git = git().apply { failure = "fatal: pathspec 'x' did not match any files" }
        openTerminal(git = git)
        onNodeWithTag(TerminalSideBarGitTestTag).performClick()
        awaitTag(terminalGitStageTestTag(foo.path))

        onNodeWithTag(terminalGitStageTestTag(foo.path)).performClick()
        awaitTag(TerminalGitErrorTestTag)
        onNodeWithTag(TerminalGitErrorTestTag).assertTextEquals("fatal: pathspec 'x' did not match any files")

        git.failure = null
        onNodeWithTag(terminalGitStageTestTag(foo.path)).performClick()
        awaitTag(TerminalGitErrorTestTag, count = 0)
    }

    @Test
    fun clickingAChangeOpensTheFileFromTheRepositoryRoot() = runComposeUiTest {
        val workspace = FakeTerminalWorkspaceRepository(initial)
        openTerminal(workspace = workspace)
        onNodeWithTag(TerminalSideBarGitTestTag).performClick()
        awaitTag(terminalGitUnstagedTestTag(foo.path))

        onNodeWithText("Foo.kt").performClick()

        waitUntil(timeoutMillis = FrameTimeoutMillis) { workspace.workspace.value.focusedTab?.program == TerminalProgram.File }
        assertEquals("$Root/app/Foo.kt", workspace.workspace.value.focusedTab!!.filePath)
    }

    @Test
    fun foldersOutsideARepositoryAndReposWithoutCommitsSaySo() = runComposeUiTest {
        val git = FakeGitChangesRepository()
        openTerminal(git = git)
        onNodeWithTag(TerminalSideBarGitTestTag).performClick()
        awaitTag(TerminalGitNotRepositoryTestTag)

        git.statuses.value = mapOf(Root to GitStatus(root = Root, branch = null, staged = emptyList(), unstaged = emptyList()))

        awaitTag(TerminalGitNoCommitsTestTag)
        onNodeWithTag(TerminalGitBranchTestTag).assertTextEquals("HEAD (detached)")
        assertEquals(0, count(TerminalGitStageAllTestTag))
    }

    private fun ComposeUiTest.openGitWithPushTarget(target: GitPushTarget?, git: FakeGitChangesRepository = git()): FakeGitChangesRepository {
        git.statuses.value = mapOf(Root to git.statuses.value.getValue(Root).copy(pushTarget = target))
        openTerminal(git = git)
        onNodeWithTag(TerminalSideBarGitTestTag).performClick()
        awaitTag(TerminalGitBranchTestTag)
        return git
    }

    @Test
    fun aBranchAheadOfItsRemoteBranchCanBePushed() = runComposeUiTest {
        val target = GitPushTarget("origin", "main", exists = true, ahead = 2)
        val git = openGitWithPushTarget(target)

        onNodeWithTag(TerminalGitPushTargetTestTag).assertTextEquals("origin/main")
        onNodeWithTag(TerminalGitPushTestTag).assertTextEquals("push ↑2").assertIsEnabled()

        onNodeWithTag(TerminalGitPushTestTag).performClick()
        waitUntil(timeoutMillis = FrameTimeoutMillis) { git.pushed.size == 1 }
        assertEquals(Root to target, git.pushed.single())
        assertEquals(0, count(TerminalGitErrorTestTag))
    }

    @Test
    fun aBranchWithNothingToPushShowsADisabledButtonAndBehindCommits() = runComposeUiTest {
        val git = openGitWithPushTarget(GitPushTarget("origin", "main", exists = true, ahead = 0, behind = 3))

        onNodeWithTag(TerminalGitPushTestTag).assertTextEquals("push").assertIsNotEnabled()

        git.statuses.value = mapOf(Root to git.statuses.value.getValue(Root).copy(pushTarget = GitPushTarget("origin", "main", exists = true, ahead = 1, behind = 3)))
        waitUntil(timeoutMillis = FrameTimeoutMillis) { onAllNodesWithText("push ↑1 ↓3").fetchSemanticsNodes().size == 1 }
        onNodeWithTag(TerminalGitPushTestTag).assertIsEnabled()
    }

    // 워크트리로 새로 만든 브랜치는 원격에 아직 없다.
    @Test
    fun aBranchMissingOnTheRemoteIsPublished() = runComposeUiTest {
        val target = GitPushTarget("origin", "feature/login", exists = false)
        val git = openGitWithPushTarget(target)

        onNodeWithTag(TerminalGitPushTargetTestTag).assertTextEquals("origin 에 없음")
        onNodeWithTag(TerminalGitPushTestTag).assertTextEquals("새 브랜치 push").assertIsEnabled()

        onNodeWithTag(TerminalGitPushTestTag).performClick()
        waitUntil(timeoutMillis = FrameTimeoutMillis) { git.pushed.size == 1 }
        assertEquals(Root to target, git.pushed.single())
    }

    @Test
    fun thereIsNoPushButtonWithoutAPushTarget() = runComposeUiTest {
        openGitWithPushTarget(null)

        assertEquals(0, count(TerminalGitPushTestTag))
        assertEquals(0, count(TerminalGitPushTargetTestTag))
    }

    @Test
    fun theButtonIsDisabledWhilePushingAndAFailureStays() = runComposeUiTest {
        val gate = CompletableDeferred<Unit>()
        val git = git().apply {
            this.gate = gate
            failure = "! [rejected] main -> main (fetch first)"
        }
        openGitWithPushTarget(GitPushTarget("origin", "main", exists = true, ahead = 1), git)

        onNodeWithTag(TerminalGitPushTestTag).performClick()
        waitUntil(timeoutMillis = FrameTimeoutMillis) { onAllNodesWithText("push 중…").fetchSemanticsNodes().size == 1 }
        onNodeWithTag(TerminalGitPushTestTag).assertIsNotEnabled()

        gate.complete(Unit)
        awaitTag(TerminalGitErrorTestTag)
        onNodeWithTag(TerminalGitErrorTestTag).assertTextEquals("! [rejected] main -> main (fetch first)")
        onNodeWithTag(TerminalGitPushTestTag).assertTextEquals("push ↑1").assertIsEnabled()
        assertEquals(1, git.pushed.size)
    }

    private companion object {
        const val FrameTimeoutMillis = 10_000L
        const val Root = "/work/jarvis"
    }
}
