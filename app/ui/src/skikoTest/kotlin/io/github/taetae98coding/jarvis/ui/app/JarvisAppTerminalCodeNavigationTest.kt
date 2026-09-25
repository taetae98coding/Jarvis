package io.github.taetae98coding.jarvis.ui.app

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.test.withKeyDown
import io.github.taetae98coding.jarvis.domain.terminal.CodeAnalysisStatus
import io.github.taetae98coding.jarvis.domain.terminal.CodeCompletion
import io.github.taetae98coding.jarvis.domain.terminal.CodeCompletionKind
import io.github.taetae98coding.jarvis.domain.terminal.CodeLocation
import io.github.taetae98coding.jarvis.domain.terminal.CodeLocations
import io.github.taetae98coding.jarvis.domain.terminal.CodeSource
import io.github.taetae98coding.jarvis.domain.terminal.FileContent
import io.github.taetae98coding.jarvis.domain.terminal.FileEntry
import io.github.taetae98coding.jarvis.domain.terminal.TerminalWorkspace
import io.github.taetae98coding.jarvis.ui.terminal.TerminalFileViewerCodeStatusTestTag
import io.github.taetae98coding.jarvis.ui.terminal.TerminalFileViewerCompletionTestTag
import io.github.taetae98coding.jarvis.ui.terminal.TerminalFileViewerEditTestTag
import io.github.taetae98coding.jarvis.ui.terminal.TerminalFileViewerEditorTestTag
import io.github.taetae98coding.jarvis.ui.terminal.TerminalFileViewerLocationsTestTag
import io.github.taetae98coding.jarvis.ui.terminal.TerminalFileViewerRevealedTestTag
import io.github.taetae98coding.jarvis.ui.terminal.TerminalTestTag
import io.github.taetae98coding.jarvis.ui.terminal.terminalFileViewerCompletionTestTag
import io.github.taetae98coding.jarvis.ui.terminal.terminalFileViewerLocationTestTag
import io.github.taetae98coding.jarvis.ui.terminal.terminalFileViewerTestTag
import kotlin.test.Test
import kotlin.test.assertEquals

/** docs/common/terminal-code-navigation.html */
@OptIn(ExperimentalTestApi::class)
class JarvisAppTerminalCodeNavigationTest {
    private val main = FileEntry("Main.kt", "$Root/Main.kt", isDirectory = false)
    private val other = "$Root/Other.kt"
    private val otherText = "package a\n\nfun greet() = Unit\n"

    private fun ComposeUiTest.count(tag: String) = onAllNodesWithTag(tag).fetchSemanticsNodes().size

    private fun ComposeUiTest.awaitTag(tag: String, count: Int = 1) {
        waitUntil(timeoutMillis = FrameTimeoutMillis) { count(tag) == count }
    }

    private fun ComposeUiTest.editorText(): String =
        onNodeWithTag(TerminalFileViewerEditorTestTag).fetchSemanticsNode().config[SemanticsProperties.EditableText].text

    private fun files(mainText: String = "fun main() = greet()\n") =
        FakeFileRepository(
            directories = mapOf(Root to listOf(main)),
            files = mapOf(main.path to FileContent.Text(mainText, truncated = false), other to FileContent.Text(otherText, truncated = false)),
        )

    private fun ComposeUiTest.openTerminal(code: FakeCodeIntelRepository, files: FakeFileRepository = files()): FakeTerminalWorkspaceRepository {
        val workspace = FakeTerminalWorkspaceRepository(TerminalWorkspace.initial().addPanel(name = "Jarvis", directory = Root).addTab().openFile(main.path))
        setContent { TestJarvisApp(terminalWorkspace = workspace, files = files, codeIntel = code) }
        onNodeWithTag(TerminalTestTag).performClick()
        awaitTag(terminalFileViewerTestTag(workspace.workspace.value.focusedTab!!.id))

        return workspace
    }

    private fun ComposeUiTest.edit(text: String) {
        onNodeWithTag(TerminalFileViewerEditTestTag).performClick()
        awaitTag(TerminalFileViewerEditorTestTag)
        onNodeWithTag(TerminalFileViewerEditorTestTag).performTextReplacement(text)
    }

    private fun location(line: Int, path: String = other) = CodeLocation(path, line, 4, otherText.lines().getOrElse(line) { "" })

    // N4
    @Test
    fun statusShowsWhileAnalysisStartsAndDisappearsWhenReady() = runComposeUiTest {
        val code = FakeCodeIntelRepository(status = CodeAnalysisStatus.Starting(42))
        openTerminal(code)

        awaitTag(TerminalFileViewerCodeStatusTestTag)
        onNodeWithTag(TerminalFileViewerCodeStatusTestTag).assertTextEquals("Kotlin 분석 준비 중… 42%")

        code.status.value = CodeAnalysisStatus.Failed("분석 서버가 멈췄습니다")
        waitUntil(timeoutMillis = FrameTimeoutMillis) { runCatching { onNodeWithText("텍스트 검색만 — 분석 서버가 멈췄습니다").fetchSemanticsNode() }.isSuccess }

        code.status.value = CodeAnalysisStatus.Ready
        awaitTag(TerminalFileViewerCodeStatusTestTag, count = 0)
    }

    // C1, C3, C4
    @Test
    fun typingOpensCompletionsAndEnterInsertsTheChosenOne() = runComposeUiTest {
        val code = FakeCodeIntelRepository(
            completions = listOf(
                CodeCompletion("greet", CodeCompletionKind.Function, signature = "()", detail = "Unit"),
                CodeCompletion("greeting", CodeCompletionKind.Variable, detail = "String"),
            ),
        )
        openTerminal(code)
        edit("val x = gr")

        onNodeWithTag(TerminalFileViewerEditorTestTag).performTextInput("e")
        awaitTag(TerminalFileViewerCompletionTestTag)
        awaitTag(terminalFileViewerCompletionTestTag(1))

        onNodeWithTag(TerminalFileViewerEditorTestTag).performKeyInput {
            pressKey(Key.DirectionDown)
            pressKey(Key.Enter)
        }
        waitUntil(timeoutMillis = FrameTimeoutMillis) { editorText() == "val x = greeting" }
        assertEquals(listOf("greeting"), code.applied)
        awaitTag(TerminalFileViewerCompletionTestTag, count = 0)

        // Esc 는 목록만 닫고 편집 보기는 남는다.
        onNodeWithTag(TerminalFileViewerEditorTestTag).performTextInput(".")
        awaitTag(TerminalFileViewerCompletionTestTag)
        onNodeWithTag(TerminalFileViewerEditorTestTag).performKeyInput { pressKey(Key.Escape) }
        awaitTag(TerminalFileViewerCompletionTestTag, count = 0)
        assertEquals(1, count(TerminalFileViewerEditorTestTag))

        // 식별자가 아닌 글자는 목록을 닫는다.
        onNodeWithTag(TerminalFileViewerEditorTestTag).performTextInput("g")
        awaitTag(TerminalFileViewerCompletionTestTag)
        onNodeWithTag(TerminalFileViewerEditorTestTag).performTextInput(" ")
        awaitTag(TerminalFileViewerCompletionTestTag, count = 0)
    }

    // G1, G4
    @Test
    fun commandBJumpsToTheOnlyDeclarationInAnotherFile() = runComposeUiTest {
        val code = FakeCodeIntelRepository(definitions = CodeLocations(listOf(location(2)), CodeSource.Analysis))
        val workspace = openTerminal(code)
        edit("fun main() = greet")

        onNodeWithTag(TerminalFileViewerEditorTestTag).performKeyInput { withKeyDown(Key.MetaLeft) { pressKey(Key.B) } }

        waitUntil(timeoutMillis = FrameTimeoutMillis) { workspace.workspace.value.focusedTab?.filePath == other }
        awaitTag(TerminalFileViewerRevealedTestTag)
    }

    // G2, G3
    @Test
    fun severalUsagesOpenAListToPickFrom() = runComposeUiTest {
        val code = FakeCodeIntelRepository(
            // 선언이 커서의 식별자 자신이면 사용하는 곳을 찾는다.
            definitions = CodeLocations(listOf(CodeLocation(main.path, 0, 4, "fun greet() = Unit")), CodeSource.Analysis),
            usages = CodeLocations(listOf(location(0), location(2)), CodeSource.TextSearch),
        )
        val workspace = openTerminal(code)
        edit("fun greet() = Unit")

        onNodeWithTag(TerminalFileViewerEditorTestTag).performKeyInput {
            repeat(10) { pressKey(Key.DirectionLeft) }
            withKeyDown(Key.CtrlLeft) { pressKey(Key.B) }
        }

        awaitTag(TerminalFileViewerLocationsTestTag)
        onNodeWithText("사용하는 곳 2곳 · 텍스트 검색").fetchSemanticsNode()
        onNodeWithTag(terminalFileViewerLocationTestTag(1)).performClick()

        waitUntil(timeoutMillis = FrameTimeoutMillis) { workspace.workspace.value.focusedTab?.filePath == other }
        awaitTag(TerminalFileViewerLocationsTestTag, count = 0)
    }

    // G5
    @Test
    fun nothingFoundSaysSo() = runComposeUiTest {
        openTerminal(FakeCodeIntelRepository())
        edit("fun main() = greet")

        onNodeWithTag(TerminalFileViewerEditorTestTag).performKeyInput { withKeyDown(Key.MetaLeft) { pressKey(Key.B) } }

        awaitTag(TerminalFileViewerCodeStatusTestTag)
        onNodeWithTag(TerminalFileViewerCodeStatusTestTag).assertTextEquals("선언을 찾지 못했습니다")
    }

    private companion object {
        const val FrameTimeoutMillis = 10_000L
        const val Root = "/work/jarvis"
    }
}
