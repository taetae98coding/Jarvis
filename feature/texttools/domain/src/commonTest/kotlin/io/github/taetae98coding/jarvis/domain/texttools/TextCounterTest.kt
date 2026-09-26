package io.github.taetae98coding.jarvis.domain.texttools

import kotlin.test.Test
import kotlin.test.assertEquals

/** docs/common/text-tools.html R3·R4 */
class TextCounterTest {
    @Test
    fun emptyTextIsAllZero() {
        assertEquals(TextStats.Empty, TextCounter.count(""))
    }

    @Test
    fun countsWithAndWithoutSpaces() {
        val stats = TextCounter.count("안녕 하세요 Hi")

        assertEquals(9, stats.charactersWithSpaces)
        assertEquals(7, stats.charactersWithoutSpaces)
        assertEquals(3, stats.words)
        assertEquals(1, stats.lines)
        assertEquals(1, stats.paragraphs)
    }

    @Test
    fun lineBreaksAreCharactersButNotNonSpaceCharacters() {
        val stats = TextCounter.count("a\nb\r\nc\rd")

        // \r\n 은 한 글자다.
        assertEquals(7, stats.charactersWithSpaces)
        assertEquals(4, stats.charactersWithoutSpaces)
        assertEquals(4, stats.lines)
        assertEquals(4, stats.words)
    }

    @Test
    fun tabsAndOnlyWhitespace() {
        val stats = TextCounter.count(" \t \n ")

        assertEquals(5, stats.charactersWithSpaces)
        assertEquals(0, stats.charactersWithoutSpaces)
        assertEquals(0, stats.words)
        assertEquals(2, stats.lines)
        assertEquals(0, stats.paragraphs)
    }

    @Test
    fun paragraphsAreSeparatedByBlankLines() {
        val text = "첫 문단 첫 줄\n첫 문단 둘째 줄\n\n  \n둘째 문단\n\n\n셋째 문단\n"

        val stats = TextCounter.count(text)

        assertEquals(3, stats.paragraphs)
        assertEquals(9, stats.lines)
    }

    @Test
    fun utf8BytesPerCodePoint() {
        assertEquals(1, TextCounter.count("a").utf8Bytes)
        assertEquals(2, TextCounter.count("é").utf8Bytes)
        assertEquals(3, TextCounter.count("가").utf8Bytes)
        assertEquals(4, TextCounter.count("😀").utf8Bytes)
        assertEquals(1 + 3 + 4, TextCounter.count("a가😀").utf8Bytes)
    }

    @Test
    fun koreanJobSiteBytesCountNonAsciiAsTwo() {
        val stats = TextCounter.count("가나 ab1!")

        assertEquals(2 + 2 + 1 + 1 + 1 + 1 + 1, stats.koreanBytes)
        assertEquals(3 + 3 + 1 + 1 + 1 + 1 + 1, stats.utf8Bytes)
        // 이모지도 코드 포인트 하나라 2다.
        assertEquals(2, TextCounter.count("😀").koreanBytes)
    }

    @Test
    fun surrogatePairIsOneCharacter() {
        val stats = TextCounter.count("😀😀")

        assertEquals(4, "😀😀".length)
        assertEquals(2, stats.charactersWithSpaces)
    }

    @Test
    fun combiningMarkJoinsBase() {
        // e + U+0301 COMBINING ACUTE ACCENT
        assertEquals(1, TextCounter.count("é").charactersWithSpaces)
        assertEquals(3, TextCounter.count("é").utf8Bytes)
    }

    @Test
    fun emojiModifiersAndVariationSelectorsJoinBase() {
        // 👍🏽 = U+1F44D U+1F3FD, ❤️ = U+2764 U+FE0F
        assertEquals(1, TextCounter.count("👍🏽").charactersWithSpaces)
        assertEquals(1, TextCounter.count("❤️").charactersWithSpaces)
    }

    @Test
    fun zwjSequenceIsOneCharacter() {
        // 👨‍👩‍👧 = 👨 ZWJ 👩 ZWJ 👧
        val family = "👨‍👩‍👧"

        assertEquals(8, family.length)
        assertEquals(1, TextCounter.count(family).charactersWithSpaces)
        assertEquals(3, TextCounter.count("a${family}b").charactersWithSpaces)
    }

    @Test
    fun regionalIndicatorsPairIntoFlags() {
        // 🇰🇷🇯🇵 는 국기 둘, 국기 글자 셋은 국기 하나와 남은 글자 하나다.
        val korea = "🇰🇷"
        val japan = "🇯🇵"

        assertEquals(2, TextCounter.count(korea + japan).charactersWithSpaces)
        assertEquals(2, TextCounter.count(korea + "🇯").charactersWithSpaces)
    }

    @Test
    fun conjoiningJamoFormOneSyllable() {
        // 옛한글 ᄒᆞᆫ = U+1112 U+119E U+11AB, 한 = U+1112 U+1161 U+11AB
        assertEquals(1, TextCounter.count("ᄒᆞᆫ").charactersWithSpaces)
        assertEquals(1, TextCounter.count("한").charactersWithSpaces)
        // 완성형 음절 뒤 종성: 가 + ᆨ → 각
        assertEquals(1, TextCounter.count("각").charactersWithSpaces)
        // 종성이 있는 음절(각) 뒤 중성은 붙지 않는다.
        assertEquals(2, TextCounter.count("각ᅡ").charactersWithSpaces)
    }

    @Test
    fun loneSurrogateCountsAsReplacementCharacter() {
        val stats = TextCounter.count("a\uD800b")

        assertEquals(3, stats.charactersWithSpaces)
        assertEquals(1 + 3 + 1, stats.utf8Bytes)
        assertEquals(1 + 2 + 1, stats.koreanBytes)
    }

    @Test
    fun readingTimeUsesHangulCharactersAndLatinWords() {
        // 한글 500자 = 60초
        assertEquals(60, TextCounter.count("가".repeat(500)).readingSeconds)
        // 영어 200단어 = 60초
        assertEquals(60, TextCounter.count(List(200) { "word" }.joinToString(" ")).readingSeconds)
        // 한글 25자 = 3초 정확히. 부동소수점이면 4초로 올림된다.
        assertEquals(3, TextCounter.count("가".repeat(25)).readingSeconds)
        // 한 단어라도 올림해 1초다.
        assertEquals(1, TextCounter.count("hi").readingSeconds)
    }

    @Test
    fun mixedWordWithHangulCountsByCharacters() {
        // "Kotlin은" 은 한글이 있는 단어라 한글 글자(1)로만 센다. "is" 는 단어 하나.
        val stats = TextCounter.count("Kotlin은 is")

        // 1자 × 60/500 + 1단어 × 60/200 = 0.12 + 0.3 → 1초
        assertEquals(1, stats.readingSeconds)
        assertEquals(2, stats.words)
    }
}
