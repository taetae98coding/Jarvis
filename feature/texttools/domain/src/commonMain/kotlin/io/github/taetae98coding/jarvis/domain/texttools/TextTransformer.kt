package io.github.taetae98coding.jarvis.domain.texttools

enum class TextTransform {
    UPPERCASE,
    LOWERCASE,
    TITLE_CASE,
    CAMEL_CASE,
    SNAKE_CASE,
    KEBAB_CASE,
    TRIM_WHITESPACE,
    REMOVE_LINE_BREAKS,
}

data class TextTransformOutput(
    val transform: TextTransform,
    val text: String,
)

object TextTransformer {
    /** 빈 입력은 결과 줄이 없다(docs/common/text-tools.html R11). */
    fun transformAll(text: String): List<TextTransformOutput> =
        if (text.isEmpty()) emptyList() else TextTransform.entries.map { TextTransformOutput(it, transform(it, text)) }

    fun transform(transform: TextTransform, text: String): String =
        when (transform) {
            TextTransform.UPPERCASE -> text.uppercase()
            TextTransform.LOWERCASE -> text.lowercase()
            TextTransform.TITLE_CASE -> titleCase(text)
            TextTransform.CAMEL_CASE -> perLine(text) { words ->
                words.mapIndexed { index, word -> if (index == 0) word.lowercase() else word.capitalized() }.joinToString("")
            }
            TextTransform.SNAKE_CASE -> perLine(text) { words -> words.joinToString("_") { it.lowercase() } }
            TextTransform.KEBAB_CASE -> perLine(text) { words -> words.joinToString("-") { it.lowercase() } }
            TextTransform.TRIM_WHITESPACE -> trimWhitespace(text)
            TextTransform.REMOVE_LINE_BREAKS -> text.collapseWhitespace()
        }

    /** 글자·숫자가 아닌 글자와 대소문자 경계에서 나눈다. 숫자는 앞 단어에 붙는다. */
    internal fun words(line: String): List<String> {
        val words = mutableListOf<String>()
        val current = StringBuilder()

        fun flush() {
            if (current.isNotEmpty()) words += current.toString()
            current.clear()
        }

        for ((index, char) in line.withIndex()) {
            if (!char.isLetterOrDigit()) {
                flush()
                continue
            }
            if (current.isNotEmpty() && char.isUpperCase()) {
                val previous = current.last()
                val next = line.getOrNull(index + 1)
                // fooBar → foo|Bar, HTTPServer → HTTP|Server(대문자 연속의 마지막 글자가 다음 단어의 첫 글자다).
                val lowerToUpper = previous.isLowerCase() || previous.isDigit()
                val acronymEnd = previous.isUpperCase() && next != null && next.isLowerCase()
                if (lowerToUpper || acronymEnd) flush()
            }
            current.append(char)
        }
        flush()
        return words
    }

    private fun perLine(text: String, join: (List<String>) -> String): String =
        text.lines().joinToString("\n") { join(words(it)) }

    private fun titleCase(text: String): String {
        val builder = StringBuilder(text.length)
        var atWordStart = true
        for (char in text) {
            builder.append(if (atWordStart) char.uppercase() else char.lowercase())
            atWordStart = char.isWhitespace()
        }
        return builder.toString()
    }

    private fun trimWhitespace(text: String): String {
        val lines = text.lines().map { it.collapseWhitespace() }
        val result = mutableListOf<String>()
        for (line in lines) {
            if (line.isEmpty() && (result.isEmpty() || result.last().isEmpty())) continue
            result += line
        }
        if (result.lastOrNull()?.isEmpty() == true) result.removeAt(result.lastIndex)
        return result.joinToString("\n")
    }

    // Regex("\\s+") 는 JVM 에서 ASCII 공백만, JS·Wasm 에서는 NBSP 같은 유니코드 공백까지 잡아 타깃마다 결과가 다르다.
    // 세기(TextCounter)와 같은 Char.isWhitespace 로 나눈다.
    private fun String.collapseWhitespace(): String {
        val builder = StringBuilder(length)
        var pendingSpace = false
        for (char in this) {
            if (char.isWhitespace()) {
                pendingSpace = builder.isNotEmpty()
            } else {
                if (pendingSpace) builder.append(' ')
                pendingSpace = false
                builder.append(char)
            }
        }
        return builder.toString()
    }

    private fun String.capitalized(): String =
        if (isEmpty()) this else this[0].uppercase() + substring(1).lowercase()
}
