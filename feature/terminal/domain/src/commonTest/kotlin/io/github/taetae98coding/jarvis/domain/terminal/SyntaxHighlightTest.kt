package io.github.taetae98coding.jarvis.domain.terminal

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SyntaxHighlightTest {
    private fun kinds(text: String, language: SyntaxLanguage): List<Pair<String, SyntaxKind>> =
        highlightSyntax(text, language).map { text.substring(it.start, it.end) to it.kind }

    @Test
    fun languageComesFromTheFileName() {
        assertEquals(SyntaxLanguage.Kotlin, syntaxLanguageOf("/a/Main.kt"))
        assertEquals(SyntaxLanguage.Kotlin, syntaxLanguageOf("/a/build.gradle.kts"))
        assertEquals(SyntaxLanguage.Swift, syntaxLanguageOf("/a/App.swift"))
        assertEquals(SyntaxLanguage.Markdown, syntaxLanguageOf("/a/README.MD"))
        assertEquals(SyntaxLanguage.Shell, syntaxLanguageOf("/home/.zshrc"))
        assertEquals(SyntaxLanguage.Shell, syntaxLanguageOf("/repo/gradlew"))
        assertNull(syntaxLanguageOf("/a/notes.txt"))
        assertNull(syntaxLanguageOf("/a/Makefile"))
        assertEquals(SyntaxLanguage.Kotlin, syntaxLanguageOfName("kotlin"))
        assertEquals(SyntaxLanguage.JavaScript, syntaxLanguageOfName("ts"))
        assertNull(syntaxLanguageOfName(""))
    }

    @Test
    fun kotlinTokens() {
        val text = """
            @Composable
            fun Greeting(name: String = "hi", count: Int = 0x1F) {
                // 인사
                val list = listOf(1.5e3, null) /* 끝 */
            }
        """.trimIndent()

        assertEquals(
            listOf(
                "@Composable" to SyntaxKind.Annotation,
                "fun" to SyntaxKind.Keyword,
                "Greeting" to SyntaxKind.Function,
                "String" to SyntaxKind.Type,
                "\"hi\"" to SyntaxKind.String,
                "Int" to SyntaxKind.Type,
                "0x1F" to SyntaxKind.Number,
                "// 인사" to SyntaxKind.Comment,
                "val" to SyntaxKind.Keyword,
                "listOf" to SyntaxKind.Function,
                "1.5e3" to SyntaxKind.Number,
                "null" to SyntaxKind.Constant,
                "/* 끝 */" to SyntaxKind.Comment,
            ),
            kinds(text, SyntaxLanguage.Kotlin),
        )
    }

    @Test
    fun multiLineCommentsAndStringsCarryAcrossLines() {
        val text = "val a = \"\"\"\nx \"q\" y\n\"\"\"\n/* a\nb */ val b"

        assertEquals(
            listOf(
                "val" to SyntaxKind.Keyword,
                "\"\"\"\nx \"q\" y\n\"\"\"" to SyntaxKind.String,
                "/* a\nb */" to SyntaxKind.Comment,
                "val" to SyntaxKind.Keyword,
            ),
            kinds(text, SyntaxLanguage.Kotlin),
        )
    }

    @Test
    fun anUnclosedStringStopsAtTheLineEnd() {
        assertEquals(
            listOf("\"abc" to SyntaxKind.String, "val" to SyntaxKind.Keyword),
            kinds("\"abc\nval x", SyntaxLanguage.Kotlin),
        )
    }

    @Test
    fun swiftAttributesAndDirectives() {
        assertEquals(
            listOf(
                "@State" to SyntaxKind.Annotation,
                "var" to SyntaxKind.Keyword,
                "Int" to SyntaxKind.Type,
                "nil" to SyntaxKind.Constant,
                "#if" to SyntaxKind.Annotation,
            ),
            kinds("@State var x: Int? = nil\n#if DEBUG", SyntaxLanguage.Swift).filterNot { it.first == "DEBUG" },
        )
    }

    @Test
    fun rustLifetimesAreNotStrings() {
        assertEquals(
            listOf(
                "fn" to SyntaxKind.Keyword,
                "'a" to SyntaxKind.Annotation,
                "'a" to SyntaxKind.Annotation,
                "str" to SyntaxKind.Keyword,
                "'x'" to SyntaxKind.String,
                "println!" to SyntaxKind.Function,
            ),
            kinds("fn f<'a>(s: &'a str) { 'x'; println!() }", SyntaxLanguage.Rust).filterNot { it.first == "f" },
        )
    }

    @Test
    fun jsonKeysAreProperties() {
        assertEquals(
            listOf(
                "\"name\"" to SyntaxKind.Property,
                "\"jarvis\"" to SyntaxKind.String,
                "\"n\"" to SyntaxKind.Property,
                "-1.5" to SyntaxKind.Number,
                "\"ok\"" to SyntaxKind.Property,
                "true" to SyntaxKind.Constant,
            ),
            kinds("""{"name": "jarvis", "n": -1.5, "ok": true}""", SyntaxLanguage.Json),
        )
    }

    @Test
    fun yamlKeysValuesAndComments() {
        val text = "# 설정\nname: jarvis # 이름\nlist:\n  - key: 'v'\n    port: 8080\n    on: true"

        assertEquals(
            listOf(
                "# 설정" to SyntaxKind.Comment,
                "name" to SyntaxKind.Property,
                "# 이름" to SyntaxKind.Comment,
                "list" to SyntaxKind.Property,
                "key" to SyntaxKind.Property,
                "'v'" to SyntaxKind.String,
                "port" to SyntaxKind.Property,
                "8080" to SyntaxKind.Number,
                "on" to SyntaxKind.Property,
                "true" to SyntaxKind.Constant,
            ),
            kinds(text, SyntaxLanguage.Yaml),
        )
    }

    @Test
    fun xmlTagsAttributesAndComments() {
        assertEquals(
            listOf(
                "<!-- c -->" to SyntaxKind.Comment,
                "View" to SyntaxKind.Tag,
                "android:id" to SyntaxKind.Property,
                "\"@+id/a\"" to SyntaxKind.String,
                "&amp;" to SyntaxKind.Constant,
                "View" to SyntaxKind.Tag,
            ),
            kinds("<!-- c --><View android:id=\"@+id/a\">&amp;</View>", SyntaxLanguage.Xml),
        )
    }

    @Test
    fun shellCommentsKeywordsAndVariables() {
        assertEquals(
            listOf(
                "# run" to SyntaxKind.Comment,
                "if" to SyntaxKind.Keyword,
                "\"\$HOME\"" to SyntaxKind.String,
                "then" to SyntaxKind.Keyword,
                "\$PATH" to SyntaxKind.Annotation,
                "fi" to SyntaxKind.Keyword,
            ),
            kinds("# run\nif [ -d \"\$HOME\" ]; then echo \$PATH#x; fi", SyntaxLanguage.Shell),
        )
    }

    @Test
    fun markdownSource() {
        val text = "# 제목\n- **굵게** 와 `code`\n```kotlin\nval a\n```\n> 인용"

        assertEquals(
            listOf(
                "# 제목" to SyntaxKind.Heading,
                "-" to SyntaxKind.Keyword,
                "**굵게**" to SyntaxKind.Emphasis,
                "`code`" to SyntaxKind.Code,
                "```kotlin" to SyntaxKind.Code,
                "val a" to SyntaxKind.Code,
                "```" to SyntaxKind.Code,
                "> 인용" to SyntaxKind.Comment,
            ),
            kinds(text, SyntaxLanguage.Markdown),
        )
    }

    @Test
    fun linesMatchStringLinesAndSplitTokensThatCrossThem() {
        val text = "a /* x\r\ny */ b\rval\n"
        val lines = highlightLines(text, SyntaxLanguage.Kotlin)

        assertEquals(text.lines().size, lines.size)
        assertEquals(
            listOf(
                listOf(SyntaxToken(2, 6, SyntaxKind.Comment)),
                listOf(SyntaxToken(0, 4, SyntaxKind.Comment)),
                listOf(SyntaxToken(0, 3, SyntaxKind.Keyword)),
                emptyList(),
            ),
            lines,
        )
    }
}
