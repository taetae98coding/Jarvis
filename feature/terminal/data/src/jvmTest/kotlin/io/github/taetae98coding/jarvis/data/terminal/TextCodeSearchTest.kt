package io.github.taetae98coding.jarvis.data.terminal

import io.github.taetae98coding.jarvis.domain.terminal.CodeLanguage
import io.github.taetae98coding.jarvis.domain.terminal.CodeLocation
import io.github.taetae98coding.jarvis.domain.terminal.CodeSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import okio.FileSystem
import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals

class TextCodeSearchTest {
    private val search = TextCodeSearch(FileSystem.SYSTEM, Dispatchers.IO)

    private fun project(): File =
        Files.createTempDirectory("jarvis-code").toFile().canonicalFile.apply {
            File(this, "settings.gradle.kts").writeText("")
            File(this, "app/src").mkdirs()
            File(this, "app/src/Greeter.kt").writeText("class Greeter {\n    fun greet() = \"hi\"\n}\n")
            File(this, "app/src/Main.kt").writeText("fun main() {\n    Greeter().greet()\n}\n")
            File(this, "app/build/generated").mkdirs()
            File(this, "app/build/generated/Copy.kt").writeText("fun greet() = Unit\n")
            File(this, "app/src/Notes.md").writeText("greet\n")
        }

    @Test
    fun rootIsTheSettingsFolder() = runTest {
        val root = project()

        assertEquals(root.path, search.root("${root.path}/app/src/Main.kt", CodeLanguage.Kotlin))
    }

    @Test
    fun declarationsSkipBuildFoldersAndOtherLanguages() = runTest {
        val root = project()
        val main = File(root, "app/src/Main.kt")

        val found = search.declarations(root.path, "greet", CodeLanguage.Kotlin, main.path, main.readText())

        assertEquals(listOf(CodeLocation(File(root, "app/src/Greeter.kt").path, 1, 8, "    fun greet() = \"hi\"")), found)
    }

    @Test
    fun usagesReadTheEditingTextForTheCurrentFile() = runTest {
        val root = project()
        val main = File(root, "app/src/Main.kt")
        val editing = "fun main() {\n    Greeter().greet()\n    Greeter().greet()\n}\n"

        val found = search.usages(root.path, "greet", CodeLanguage.Kotlin, main.path, editing)

        assertEquals(listOf(main.path to 1, main.path to 2), found.map { it.path to it.line })
    }

    @Test
    fun repositoryFallsBackToTextSearchWithoutAnalysis() = runTest {
        val root = project()
        val main = File(root, "app/src/Main.kt")
        val repository = DefaultCodeIntelRepository(UnavailableCodeAnalysisDataSource, search)
        val text = main.readText()

        val declarations = repository.definition(main.path, text, text.indexOf("greet"))
        val completions = repository.complete(main.path, "val greeting = 1\ngre", 20)

        assertEquals(CodeSource.TextSearch, declarations.source)
        assertEquals(listOf(1), declarations.locations.map { it.line })
        assertEquals(listOf("greeting"), completions.items.map { it.label })
    }
}
