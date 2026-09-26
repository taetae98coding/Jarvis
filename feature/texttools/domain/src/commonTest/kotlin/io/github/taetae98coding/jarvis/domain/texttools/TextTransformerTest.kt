package io.github.taetae98coding.jarvis.domain.texttools

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** docs/common/text-tools.html R11 */
class TextTransformerTest {
    private fun transform(transform: TextTransform, text: String) = TextTransformer.transform(transform, text)

    @Test
    fun emptyInputHasNoOutputs() {
        assertTrue(TextTransformer.transformAll("").isEmpty())
    }

    @Test
    fun everyTransformHasOneOutputInOrder() {
        assertEquals(TextTransform.entries, TextTransformer.transformAll("a").map { it.transform })
    }

    @Test
    fun upperAndLower() {
        assertEquals("HELLO 세계", transform(TextTransform.UPPERCASE, "Hello 세계"))
        assertEquals("hello 세계", transform(TextTransform.LOWERCASE, "HeLLo 세계"))
    }

    @Test
    fun titleCaseKeepsWhitespace() {
        assertEquals("Hello  World\nFoo-bar 한국어", transform(TextTransform.TITLE_CASE, "hELLO  world\nfoo-BAR 한국어"))
    }

    @Test
    fun wordSplitting() {
        assertEquals(listOf("foo", "Bar", "Baz"), TextTransformer.words("fooBarBaz"))
        assertEquals(listOf("HTTP", "Server"), TextTransformer.words("HTTPServer"))
        assertEquals(listOf("version2", "Final"), TextTransformer.words("version2Final"))
        assertEquals(listOf("hello", "world", "again"), TextTransformer.words("  hello_world--again!! "))
        assertEquals(listOf("사용자", "이름"), TextTransformer.words("사용자 이름"))
        assertEquals(emptyList(), TextTransformer.words(" _-"))
    }

    @Test
    fun camelSnakeKebab() {
        val input = "Hello world-from_Kotlin"

        assertEquals("helloWorldFromKotlin", transform(TextTransform.CAMEL_CASE, input))
        assertEquals("hello_world_from_kotlin", transform(TextTransform.SNAKE_CASE, input))
        assertEquals("hello-world-from-kotlin", transform(TextTransform.KEBAB_CASE, input))
    }

    @Test
    fun caseConversionIsPerLine() {
        val input = "user id\nHTTPServer"

        assertEquals("userId\nhttpServer", transform(TextTransform.CAMEL_CASE, input))
        assertEquals("user_id\nhttp_server", transform(TextTransform.SNAKE_CASE, input))
        assertEquals("user-id\nhttp-server", transform(TextTransform.KEBAB_CASE, input))
    }

    @Test
    fun trimWhitespaceCollapsesRunsAndBlankLines() {
        val input = "\n\n  hello   \t world  \n\n\n  second  line\n  \n"

        assertEquals("hello world\n\nsecond line", transform(TextTransform.TRIM_WHITESPACE, input))
    }

    @Test
    fun removeLineBreaksJoinsWithSingleSpace() {
        assertEquals("one two three", transform(TextTransform.REMOVE_LINE_BREAKS, "one\ntwo\r\n\n  three  "))
    }
}
