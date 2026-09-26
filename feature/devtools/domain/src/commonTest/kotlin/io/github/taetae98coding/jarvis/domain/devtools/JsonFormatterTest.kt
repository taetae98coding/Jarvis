package io.github.taetae98coding.jarvis.domain.devtools

import kotlin.test.Test
import kotlin.test.assertEquals

class JsonFormatterTest {
    @Test
    fun prettyAndMinified() {
        val result = JsonFormatter.format(""" { "a" : 1, "b":[true ,false, null], "c": {"d": "e"} } """)

        assertEquals(
            JsonFormatResult.Success(
                pretty = """
                    {
                      "a": 1,
                      "b": [
                        true,
                        false,
                        null
                      ],
                      "c": {
                        "d": "e"
                      }
                    }
                """.trimIndent(),
                minified = """{"a":1,"b":[true,false,null],"c":{"d":"e"}}""",
            ),
            result,
        )
    }

    @Test
    fun emptyContainersStayCompact() {
        assertEquals(
            JsonFormatResult.Success(pretty = "{\n  \"a\": {},\n  \"b\": []\n}", minified = """{"a":{},"b":[]}"""),
            JsonFormatter.format("""{"a": { }, "b": [ ]}"""),
        )
    }

    @Test
    fun numbersAndStringsAreCopiedVerbatim() {
        val source = """[1.0, -0.5e+10, 12345678901234567890, "탭\t\"따옴표\" é \/"]"""
        val result = JsonFormatter.format(source) as JsonFormatResult.Success

        assertEquals("""[1.0,-0.5e+10,12345678901234567890,"탭\t\"따옴표\" é \/"]""", result.minified)
    }

    @Test
    fun scalarDocument() {
        assertEquals(JsonFormatResult.Success(pretty = "\"x\"", minified = "\"x\""), JsonFormatter.format("  \"x\"  "))
    }

    @Test
    fun errorsReportLineAndColumn() {
        assertError(line = 2, column = 7,JsonErrorReason.UNEXPECTED_CHARACTER, "{\n  \"a\" 1\n}")
        assertError(line = 1, column = 10, JsonErrorReason.UNEXPECTED_END, """{"a": [1,""")
        assertError(line = 1, column = 9, JsonErrorReason.UNEXPECTED_CHARACTER, """{"a": 1,}""")
        assertError(line = 1, column = 4, JsonErrorReason.TRAILING_CONTENT, "{} {}")
        assertError(line = 1, column = 4, JsonErrorReason.INVALID_ESCAPE, """"a\x"""")
        assertError(line = 1, column = 3, JsonErrorReason.CONTROL_CHARACTER_IN_STRING, "\"a\nb\"")
        // 0 뒤의 숫자는 수의 일부가 아니라 문서 뒤에 남은 글자다.
        assertError(line = 1, column = 2, JsonErrorReason.TRAILING_CONTENT, "01")
        assertError(line = 1, column = 3, JsonErrorReason.INVALID_NUMBER, "1.")
        assertError(line = 1, column = 2, JsonErrorReason.INVALID_NUMBER, "-x")
        assertError(line = 1, column = 1, JsonErrorReason.UNEXPECTED_CHARACTER, "tru")
        assertError(line = 1, column = 1, JsonErrorReason.UNEXPECTED_CHARACTER, "'a'")
    }

    @Test
    fun deepNestingStopsBeforeStackOverflow() {
        val depth = JsonFormatter.MaxDepth + 1
        val deep = "[".repeat(depth + 1) + "]".repeat(depth + 1)

        val error = (JsonFormatter.format(deep) as JsonFormatResult.Failure).error
        assertEquals(JsonErrorReason.TOO_DEEP, error.reason)

        val allowed = "[".repeat(JsonFormatter.MaxDepth) + "]".repeat(JsonFormatter.MaxDepth)
        assertEquals("[".repeat(JsonFormatter.MaxDepth) + "]".repeat(JsonFormatter.MaxDepth), (JsonFormatter.format(allowed) as JsonFormatResult.Success).minified)
    }

    @Test
    fun convertMarksBothLinesOnError() {
        val error = DevToolValue.Error(DevToolError.InvalidJson(1, 2, JsonErrorReason.UNEXPECTED_END))

        assertEquals(
            listOf(DevToolOutput(DevToolOutputKind.JSON_PRETTY, error), DevToolOutput(DevToolOutputKind.JSON_MINIFIED, error)),
            JsonFormatter.convert("{"),
        )
        assertEquals(emptyList(), JsonFormatter.convert(" \n"))
    }

    private fun assertError(line: Int, column: Int, reason: JsonErrorReason, input: String) {
        assertEquals(
            JsonFormatResult.Failure(DevToolError.InvalidJson(line, column, reason)),
            JsonFormatter.format(input),
            input,
        )
    }
}
