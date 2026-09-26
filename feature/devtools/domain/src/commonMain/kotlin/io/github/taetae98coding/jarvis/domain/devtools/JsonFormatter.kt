package io.github.taetae98coding.jarvis.domain.devtools

/**
 * RFC 8259 JSON 을 읽으며 정렬본과 한 줄본을 함께 쓴다. 값을 트리로 만들지 않고 원문 조각을 옮기므로
 * 숫자는 적힌 그대로 남는다(`1.0` 이 `1` 로, 큰 정수가 Double 로 뭉개지지 않는다).
 */
object JsonFormatter {
    /** 이보다 깊으면 재귀가 스택을 넘기기 전에 멈춘다. Wasm 의 스택이 가장 얕다. */
    const val MaxDepth = 512

    fun convert(input: String): List<DevToolOutput> {
        if (input.isBlank()) return emptyList()

        return when (val result = format(input)) {
            is JsonFormatResult.Success -> listOf(
                DevToolOutput(DevToolOutputKind.JSON_PRETTY, DevToolValue.Text(result.pretty)),
                DevToolOutput(DevToolOutputKind.JSON_MINIFIED, DevToolValue.Text(result.minified)),
            )

            is JsonFormatResult.Failure -> listOf(
                DevToolOutput(DevToolOutputKind.JSON_PRETTY, DevToolValue.Error(result.error)),
                DevToolOutput(DevToolOutputKind.JSON_MINIFIED, DevToolValue.Error(result.error)),
            )
        }
    }

    fun format(input: String): JsonFormatResult =
        try {
            val writer = JsonWriter(input)
            writer.document()
            JsonFormatResult.Success(pretty = writer.pretty.toString(), minified = writer.minified.toString())
        } catch (e: JsonSyntaxException) {
            JsonFormatResult.Failure(e.toError(input))
        }
}

sealed interface JsonFormatResult {
    data class Success(val pretty: String, val minified: String) : JsonFormatResult

    data class Failure(val error: DevToolError.InvalidJson) : JsonFormatResult
}

private class JsonSyntaxException(val index: Int, val reason: JsonErrorReason) : Exception()

private fun JsonSyntaxException.toError(input: String): DevToolError.InvalidJson {
    var line = 1
    var column = 1
    for (i in 0 until index.coerceAtMost(input.length)) {
        if (input[i] == '\n') {
            line++
            column = 1
        } else {
            column++
        }
    }

    return DevToolError.InvalidJson(line = line, column = column, reason = reason)
}

private class JsonWriter(private val input: String) {
    val pretty = StringBuilder()
    val minified = StringBuilder()
    private var index = 0

    fun document() {
        skipWhitespace()
        value(depth = 0)
        skipWhitespace()
        if (index < input.length) fail(JsonErrorReason.TRAILING_CONTENT)
    }

    private fun value(depth: Int) {
        if (depth > JsonFormatter.MaxDepth) fail(JsonErrorReason.TOO_DEEP)

        when (peek()) {
            '{' -> container(depth, open = '{', close = '}') { member(it) }
            '[' -> container(depth, open = '[', close = ']') { value(it) }
            '"' -> string()
            't' -> literal("true")
            'f' -> literal("false")
            'n' -> literal("null")
            else -> number()
        }
    }

    private inline fun container(depth: Int, open: Char, close: Char, element: (Int) -> Unit) {
        index++
        emit(open)
        skipWhitespace()

        if (peek() == close) {
            index++
            emit(close)
            return
        }

        while (true) {
            newline(depth + 1)
            element(depth + 1)
            skipWhitespace()

            when (peek()) {
                ',' -> {
                    index++
                    emit(',')
                    skipWhitespace()
                }

                close -> {
                    index++
                    newline(depth)
                    emit(close)
                    return
                }

                else -> fail(JsonErrorReason.UNEXPECTED_CHARACTER)
            }
        }
    }

    private fun member(depth: Int) {
        if (peek() != '"') fail(JsonErrorReason.UNEXPECTED_CHARACTER)
        string()
        skipWhitespace()
        if (peek() != ':') fail(JsonErrorReason.UNEXPECTED_CHARACTER)
        index++
        minified.append(':')
        pretty.append(": ")
        skipWhitespace()
        value(depth)
    }

    private fun string() {
        val start = index
        index++

        while (true) {
            val char = peek()
            when {
                char == '"' -> break
                char == '\\' -> escape()
                char < ' ' -> fail(JsonErrorReason.CONTROL_CHARACTER_IN_STRING)
                else -> index++
            }
        }

        index++
        emit(input.substring(start, index))
    }

    private fun escape() {
        index++
        when (peek()) {
            '"', '\\', '/', 'b', 'f', 'n', 'r', 't' -> index++
            'u' -> {
                index++
                repeat(4) {
                    if (peek().digitToIntOrNull(16) == null) fail(JsonErrorReason.INVALID_ESCAPE)
                    index++
                }
            }

            else -> fail(JsonErrorReason.INVALID_ESCAPE)
        }
    }

    private fun literal(word: String) {
        if (!input.startsWith(word, index)) fail(JsonErrorReason.UNEXPECTED_CHARACTER)
        index += word.length
        emit(word)
    }

    // number = [ minus ] int [ frac ] [ exp ], int = zero / ( digit1-9 *DIGIT )
    private fun number() {
        val start = index
        if (peekOrNull() == '-') index++

        when (peekOrNull()) {
            null -> fail(JsonErrorReason.UNEXPECTED_END)
            '0' -> index++
            in '1'..'9' -> digits()
            else -> fail(if (index == start) JsonErrorReason.UNEXPECTED_CHARACTER else JsonErrorReason.INVALID_NUMBER)
        }

        if (peekOrNull() == '.') {
            index++
            if (peekOrNull() !in '0'..'9') fail(JsonErrorReason.INVALID_NUMBER)
            digits()
        }

        if (peekOrNull() == 'e' || peekOrNull() == 'E') {
            index++
            if (peekOrNull() == '+' || peekOrNull() == '-') index++
            if (peekOrNull() !in '0'..'9') fail(JsonErrorReason.INVALID_NUMBER)
            digits()
        }

        emit(input.substring(start, index))
    }

    private fun digits() {
        while (peekOrNull() in '0'..'9') index++
    }

    private fun skipWhitespace() {
        while (index < input.length && input[index].let { it == ' ' || it == '\t' || it == '\n' || it == '\r' }) index++
    }

    private fun peekOrNull(): Char? = input.getOrNull(index)

    private fun peek(): Char = peekOrNull() ?: fail(JsonErrorReason.UNEXPECTED_END)

    private fun emit(char: Char) {
        pretty.append(char)
        minified.append(char)
    }

    private fun emit(text: String) {
        pretty.append(text)
        minified.append(text)
    }

    private fun newline(depth: Int) {
        pretty.append('\n')
        repeat(depth) { pretty.append(Indent) }
    }

    private fun fail(reason: JsonErrorReason): Nothing = throw JsonSyntaxException(index, reason)
}

private const val Indent = "  "
