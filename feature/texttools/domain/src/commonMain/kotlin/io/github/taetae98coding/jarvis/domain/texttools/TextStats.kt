package io.github.taetae98coding.jarvis.domain.texttools

data class TextStats(
    val charactersWithSpaces: Int,
    val charactersWithoutSpaces: Int,
    val words: Int,
    val lines: Int,
    val paragraphs: Int,
    val utf8Bytes: Int,
    /** 한글 등 ASCII 밖의 코드 포인트는 2, ASCII 는 1. 채용 사이트의 자기소개서 바이트 방식이다. */
    val koreanBytes: Int,
    val readingSeconds: Int,
) {
    companion object {
        val Empty = TextStats(0, 0, 0, 0, 0, 0, 0, 0)
    }
}
