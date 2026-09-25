package io.github.taetae98coding.jarvis.domain.terminal

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CodeNavigationTest {
    @Test
    fun languageComesFromExtension() {
        assertEquals(CodeLanguage.Kotlin, codeLanguageOf("/a/Main.kt"))
        assertEquals(CodeLanguage.Kotlin, codeLanguageOf("/a/build.gradle.kts"))
        assertEquals(CodeLanguage.Swift, codeLanguageOf("/a/App.SWIFT"))
        assertNull(codeLanguageOf("/a/README.md"))
        assertNull(codeLanguageOf("/a/kt"))
    }

    @Test
    fun kotlinRootIsNearestSettingsThenTopmostBuildThenGit() {
        val tree = mapOf(
            "/r" to setOf(".git", "settings.gradle.kts", "app"),
            "/r/app" to setOf("build.gradle.kts", "src"),
            "/r/app/src" to setOf("Main.kt"),
        )
        assertEquals("/r", codeProjectRoot("/r/app/src/Main.kt", CodeLanguage.Kotlin, tree::get))

        val noSettings = mapOf("/r" to setOf(".git", "build.gradle"), "/r/app" to setOf("build.gradle.kts"))
        assertEquals("/r", codeProjectRoot("/r/app/Main.kt", CodeLanguage.Kotlin, noSettings::get))

        assertEquals("/g", codeProjectRoot("/g/x/Main.kt", CodeLanguage.Kotlin, mapOf("/g" to setOf(".git"))::get))
        assertEquals("/loose", codeProjectRoot("/loose/Main.kt", CodeLanguage.Kotlin) { null })
    }

    @Test
    fun swiftRootIsPackageThenXcodeProject() {
        val tree = mapOf(
            "/r" to setOf(".git", "iosApp"),
            "/r/iosApp" to setOf("iosApp.xcodeproj", "iosApp"),
            "/r/iosApp/iosApp" to setOf("App.swift"),
        )
        assertEquals("/r/iosApp", codeProjectRoot("/r/iosApp/iosApp/App.swift", CodeLanguage.Swift, tree::get))

        val pkg = tree + ("/r" to setOf(".git", "Package.swift", "iosApp"))
        assertEquals("/r", codeProjectRoot("/r/iosApp/iosApp/App.swift", CodeLanguage.Swift, pkg::get))
    }

    @Test
    fun identifierIncludesCursorAtEnd() {
        val text = "val message = greet(\"a\")"
        assertEquals(14..18, identifierAt(text, 14, CodeLanguage.Kotlin))
        assertEquals(14..18, identifierAt(text, 19, CodeLanguage.Kotlin))
        assertNull(identifierAt(text, 13, CodeLanguage.Kotlin))
        assertNull(identifierAt("x = 42", 5, CodeLanguage.Kotlin))
        assertEquals(0..1, identifierAt("\$0 + 1", 1, CodeLanguage.Swift))
        assertEquals("gre", completionPrefix(text, 17, CodeLanguage.Kotlin))
        assertEquals("", completionPrefix("a.", 2, CodeLanguage.Kotlin))
    }

    @Test
    fun positionsRoundTripAcrossLineBreaks() {
        val text = "ab\r\ncd\nef\rgh"
        assertEquals(TextPosition(0, 1), positionOf(text, 1))
        assertEquals(TextPosition(1, 0), positionOf(text, 4))
        assertEquals(TextPosition(3, 2), positionOf(text, text.length))
        assertEquals(4, offsetOf(text, 1, 0))
        assertEquals(10, offsetOf(text, 3, 0))
        assertEquals(6, offsetOf(text, 1, 99))
        assertEquals(text.length, offsetOf(text, 9, 0))
        assertEquals(listOf("ab", "cd", "ef", "gh"), (0..3).map { lineTextOf(text, it) })
        assertEquals(text.lines().size, lineStarts(text).size)
    }

    @Test
    fun editsApplyTogetherAndCursorFollowsEarlierInsertions() {
        val text = "package a\n\nfun f() = len"
        val import = CodeTextEdit(10, 10, "import b.length\n")
        val main = CodeTextEdit(text.length - 3, text.length, "length")

        val result = applyTextEdits(text, listOf(main, import))

        assertEquals("package a\nimport b.length\n\nfun f() = length", result)
        assertEquals(result.length, cursorAfterEdits(main, listOf(import)))
    }

    @Test
    fun completionsArePrefixFilteredAndSortedBySortText() {
        val items = listOf(
            CodeCompletion("length", CodeCompletionKind.Variable, sortText = "2"),
            CodeCompletion("lastIndex", CodeCompletionKind.Variable, sortText = "1"),
            CodeCompletion("Lazy", CodeCompletionKind.Type, sortText = "3"),
            CodeCompletion("map", CodeCompletionKind.Function, sortText = "0"),
            CodeCompletion("ignoresSafeArea()", CodeCompletionKind.Function, filterText = "ignoresSafeArea()", sortText = "4"),
        )

        assertEquals(listOf("lastIndex", "length", "Lazy"), filterCompletions(items, "l").map { it.label })
        assertEquals(listOf("ignoresSafeArea()"), filterCompletions(items, "ignoresSa").map { it.label })
        assertEquals(5, filterCompletions(items, "").size)
        assertEquals(1, filterCompletions(items, "", limit = 1).size)
    }

    @Test
    fun wordCompletionsPreferNearbyWordsAndSkipTheTypedOne() {
        val text = "val greeting = 1\nval great = 2\nfun f() = gre"

        val words = wordCompletions(text, text.length, CodeLanguage.Kotlin).map { it.label }

        assertEquals(listOf("great", "greeting"), words)
        assertEquals(emptyList(), wordCompletions("a.", 2, CodeLanguage.Kotlin))
    }

    @Test
    fun textSearchFindsDeclarationsAfterKeywordsAndWholeWordUsages() {
        val files = sequenceOf(
            CodeFile("/r/A.kt", "fun greet(name: String) = name\nfun <T> List<T>.greet() = Unit\nval greeting = greet(\"a\")"),
            CodeFile("/r/B.kt", "class Greeter {\n    fun hi() = greet(\"b\") // greet\n}"),
        )

        val declarations = findDeclarations(files, "greet", CodeLanguage.Kotlin)
        val usages = findUsages(files, "greet", CodeLanguage.Kotlin)

        assertEquals(listOf(CodeLocation("/r/A.kt", 0, 4, "fun greet(name: String) = name"), CodeLocation("/r/A.kt", 1, 16, "fun <T> List<T>.greet() = Unit")), declarations)
        assertEquals(listOf("/r/A.kt" to 2, "/r/B.kt" to 1, "/r/B.kt" to 1), usages.map { it.path to it.line })
        assertEquals(listOf(15, 29), usages.drop(1).map { it.column })
    }

    @Test
    fun swiftDeclarationKeywords() {
        val files = sequenceOf(CodeFile("/r/A.swift", "struct ContentView: View {}\nlet view = ContentView()\nfunc make() -> ContentView { ContentView() }"))

        assertEquals(listOf(0), findDeclarations(files, "ContentView", CodeLanguage.Swift).map { it.line })
        assertEquals(3, findUsages(files, "ContentView", CodeLanguage.Swift).size)
    }

    @Test
    fun goToDeclarationOnTheDeclarationFindsUsages() = runTest {
        val text = "fun greet() = Unit\nval x = greet()"
        val repository = FakeCodeIntelRepository(
            definition = CodeLocations(listOf(CodeLocation("/r/A.kt", 0, 4, "fun greet() = Unit")), CodeSource.Analysis),
            usages = CodeLocations(listOf(CodeLocation("/r/A.kt", 1, 8, "val x = greet()")), CodeSource.Analysis),
        )
        val goTo = GoToDeclarationUseCase(repository)

        assertEquals(CodeNavigationKind.Usages, goTo("/r/A.kt", text, 6)?.kind)
        assertEquals(CodeNavigationKind.Declaration, goTo("/r/A.kt", text, text.length - 3)?.kind)
        assertNull(goTo("/r/A.kt", text, 12))
        assertNull(goTo("/r/README.md", text, 6))
    }

    private class FakeCodeIntelRepository(
        private val definition: CodeLocations,
        private val usages: CodeLocations,
    ) : CodeIntelRepository {
        override fun observeAnalysis(path: String): Flow<CodeAnalysisStatus> = emptyFlow()

        override suspend fun complete(path: String, text: String, offset: Int) = CodeCompletions(emptyList(), CodeSource.Analysis)

        override suspend fun applyCompletion(path: String, text: String, offset: Int, item: CodeCompletion) = CodeEdit(text, offset)

        override suspend fun definition(path: String, text: String, offset: Int) = definition

        override suspend fun usages(path: String, text: String, offset: Int) = usages
    }
}
