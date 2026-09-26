package io.github.taetae98coding.jarvis.domain.texttools

/**
 * 글자는 UAX #29 확장 자소 클러스터를 흔한 경우만 근사해 센다. 어떤 코드 포인트를 앞 글자에 붙이는지와
 * 버린 규칙은 docs/common/text-tools.html#decision-grapheme 에 있다.
 */
object TextCounter {
    const val HangulCharactersPerMinute = 500
    const val WordsPerMinute = 200

    fun count(text: String): TextStats {
        if (text.isEmpty()) return TextStats.Empty

        val codePoints = text.toCodePoints()
        val clusters = clusters(codePoints)

        var withoutSpaces = 0
        var words = 0
        var latinWords = 0
        var cjkCharacters = 0
        var lineBreaks = 0
        var inWord = false
        var wordHasCjk = false

        fun endWord() {
            if (inWord && !wordHasCjk) latinWords++
            inWord = false
            wordHasCjk = false
        }

        for (cluster in clusters) {
            if (cluster.isLineBreak) lineBreaks++

            if (cluster.isWhitespace) {
                endWord()
                continue
            }

            withoutSpaces++
            if (!inWord) {
                words++
                inWord = true
            }
            if (cluster.isCjk) {
                cjkCharacters++
                wordHasCjk = true
            }
        }
        endWord()

        return TextStats(
            charactersWithSpaces = clusters.size,
            charactersWithoutSpaces = withoutSpaces,
            words = words,
            lines = lineBreaks + 1,
            paragraphs = paragraphs(text),
            utf8Bytes = codePoints.sumOf(::utf8Length),
            koreanBytes = codePoints.sumOf { if (it < 0x80) 1 else 2 },
            readingSeconds = readingSeconds(cjkCharacters, latinWords),
        )
    }

    private fun readingSeconds(cjkCharacters: Int, latinWords: Int): Int {
        // 두 속도를 부동소수점으로 더하면 0.12 × 25 가 3.0000000004 가 돼 올림이 1초 늘어난다. 정수 분수로 계산한다.
        val numerator = cjkCharacters.toLong() * 60 * WordsPerMinute + latinWords.toLong() * 60 * HangulCharactersPerMinute
        val denominator = HangulCharactersPerMinute.toLong() * WordsPerMinute
        return ((numerator + denominator - 1) / denominator).toInt()
    }

    private fun paragraphs(text: String): Int {
        var count = 0
        var inParagraph = false
        for (line in text.lines()) {
            if (line.isBlank()) {
                inParagraph = false
            } else if (!inParagraph) {
                count++
                inParagraph = true
            }
        }
        return count
    }

    private class Cluster(
        val isWhitespace: Boolean,
        val isLineBreak: Boolean,
        val isCjk: Boolean,
    )

    private fun clusters(codePoints: IntArray): List<Cluster> {
        val clusters = ArrayList<Cluster>(codePoints.size)
        var index = 0
        while (index < codePoints.size) {
            val first = codePoints[index]
            var previous = first
            var regionalIndicators = if (isRegionalIndicator(first)) 1 else 0
            index++

            while (index < codePoints.size && extends(previous, codePoints[index], regionalIndicators)) {
                previous = codePoints[index]
                regionalIndicators = if (isRegionalIndicator(previous)) regionalIndicators + 1 else 0
                index++
            }

            clusters += Cluster(
                isWhitespace = isWhitespace(first),
                isLineBreak = first == LF || first == CR,
                isCjk = isCjk(first),
            )
        }
        return clusters
    }

    private fun extends(previous: Int, next: Int, regionalIndicators: Int): Boolean {
        // GB3·GB4·GB5: CR LF 만 붙고, 그 밖의 줄바꿈은 앞뒤와 떨어진다.
        if (previous == CR) return next == LF
        if (previous == LF || next == CR || next == LF) return false
        // GB9: 결합 문자·변형 선택자·피부색·태그·ZWJ 는 앞 글자에 붙는다.
        if (isExtend(next) || next == ZWJ) return true
        // GB11 의 근사: ZWJ 뒤 글자는 앞이 이모지인지 보지 않고 붙인다.
        if (previous == ZWJ) return true
        // GB12·GB13: 국기 글자는 둘씩 짝을 짓는다.
        if (isRegionalIndicator(previous) && isRegionalIndicator(next)) return regionalIndicators % 2 == 1
        return hangulExtends(hangulType(previous), hangulType(next))
    }

    // GB6·GB7·GB8: 첫가끝 자모와 완성형 음절이 한 음절로 붙는 규칙.
    private fun hangulExtends(previous: HangulType, next: HangulType): Boolean =
        when (previous) {
            HangulType.L -> next != HangulType.NONE && next != HangulType.T
            HangulType.LV, HangulType.V -> next == HangulType.V || next == HangulType.T
            HangulType.LVT, HangulType.T -> next == HangulType.T
            HangulType.NONE -> false
        }

    private enum class HangulType { NONE, L, V, T, LV, LVT }

    private fun hangulType(codePoint: Int): HangulType =
        when (codePoint) {
            in 0x1100..0x115F, in 0xA960..0xA97C -> HangulType.L
            in 0x1160..0x11A7, in 0xD7B0..0xD7C6 -> HangulType.V
            in 0x11A8..0x11FF, in 0xD7CB..0xD7FB -> HangulType.T
            in HangulSyllables -> if ((codePoint - HangulSyllables.first) % 28 == 0) HangulType.LV else HangulType.LVT
            else -> HangulType.NONE
        }

    private fun isExtend(codePoint: Int): Boolean =
        codePoint in 0x0300..0x036F ||
            codePoint in 0x1AB0..0x1AFF ||
            codePoint in 0x1DC0..0x1DFF ||
            codePoint in 0x20D0..0x20FF ||
            codePoint in 0xFE20..0xFE2F ||
            codePoint in 0xFE00..0xFE0F ||
            codePoint in 0xE0100..0xE01EF ||
            codePoint in 0x1F3FB..0x1F3FF ||
            codePoint in 0xE0020..0xE007F ||
            // 한글 방점(U+302E·302F)은 앞 음절에 붙는 결합 부호다.
            codePoint == 0x302E || codePoint == 0x302F

    private fun isRegionalIndicator(codePoint: Int): Boolean = codePoint in 0x1F1E6..0x1F1FF

    private fun isWhitespace(codePoint: Int): Boolean =
        codePoint <= 0xFFFF && codePoint.toChar().isWhitespace()

    private fun isCjk(codePoint: Int): Boolean =
        codePoint in HangulSyllables ||
            codePoint in 0x1100..0x11FF ||
            codePoint in 0x3130..0x318F ||
            codePoint in 0xA960..0xA97F ||
            codePoint in 0xD7B0..0xD7FF ||
            codePoint in 0x3040..0x30FF ||
            codePoint in 0x3400..0x4DBF ||
            codePoint in 0x4E00..0x9FFF ||
            codePoint in 0xF900..0xFAFF ||
            codePoint in 0x20000..0x3FFFF

    private fun utf8Length(codePoint: Int): Int =
        when {
            codePoint < 0x80 -> 1
            codePoint < 0x800 -> 2
            // 짝 없는 서로게이트도 여기로 온다. UTF-8 로 적으면 U+FFFD(3바이트)가 된다.
            codePoint < 0x10000 -> 3
            else -> 4
        }

    private val HangulSyllables = 0xAC00..0xD7A3
    private const val CR = '\r'.code
    private const val LF = '\n'.code
    private const val ZWJ = 0x200D
}

/** 서로게이트 쌍은 한 코드 포인트로, 짝 없는 서로게이트는 그 값 그대로 둔다. */
internal fun String.toCodePoints(): IntArray {
    val result = IntArray(length)
    var size = 0
    var index = 0
    while (index < length) {
        val high = this[index]
        if (high.isHighSurrogate() && index + 1 < length && this[index + 1].isLowSurrogate()) {
            result[size++] = 0x10000 + ((high.code - 0xD800) shl 10) + (this[index + 1].code - 0xDC00)
            index += 2
        } else {
            result[size++] = high.code
            index++
        }
    }
    return result.copyOf(size)
}
