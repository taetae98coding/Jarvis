package io.github.taetae98coding.jarvis.domain.terminal

/**
 * 격자에서 차지하는 칸 수. 0 은 앞 글자에 붙는 결합 문자, 2 는 전각이다.
 *
 * Unicode EastAsianWidth 전체 표가 아니라 셸에서 실제로 마주치는 범위만 담았다. 한글 중성·종성
 * 자모(U+1160–U+11FF)가 0 인 것이 중요하다 — macOS 의 파일 이름은 NFD 라 `ls` 가 초성·중성·종성을
 * 따로 보내고, 이것들이 칸을 차지하면 한글 파일 이름 뒤의 열이 전부 밀린다.
 */
fun terminalCharWidth(codePoint: Int): Int =
    when {
        codePoint in 0x0300..0x036F -> 0
        codePoint in 0x0483..0x0489 -> 0
        codePoint in 0x0591..0x05BD -> 0
        codePoint in 0x0610..0x061A -> 0
        codePoint in 0x064B..0x065F -> 0
        codePoint in 0x0E31..0x0E3A && codePoint != 0x0E32 && codePoint != 0x0E33 -> 0
        codePoint in 0x0E47..0x0E4E -> 0
        codePoint in 0x1160..0x11FF -> 0
        codePoint in 0x1AB0..0x1AFF -> 0
        codePoint in 0x1DC0..0x1DFF -> 0
        codePoint in 0x200B..0x200F -> 0
        codePoint in 0x20D0..0x20FF -> 0
        codePoint in 0x302A..0x302F -> 0
        codePoint in 0x3099..0x309A -> 0
        codePoint in 0xD7B0..0xD7FF -> 0
        codePoint in 0xFE00..0xFE0F -> 0
        codePoint in 0xFE20..0xFE2F -> 0
        codePoint in 0xE0100..0xE01EF -> 0

        codePoint in 0x1100..0x115F -> 2
        codePoint in 0x231A..0x231B -> 2
        codePoint in 0x2E80..0x303E -> 2
        codePoint in 0x3041..0x33FF -> 2
        codePoint in 0x3400..0x4DBF -> 2
        codePoint in 0x4E00..0x9FFF -> 2
        codePoint in 0xA000..0xA4CF -> 2
        codePoint in 0xA960..0xA97F -> 2
        codePoint in 0xAC00..0xD7A3 -> 2
        codePoint in 0xF900..0xFAFF -> 2
        codePoint in 0xFE30..0xFE4F -> 2
        codePoint in 0xFF00..0xFF60 -> 2
        codePoint in 0xFFE0..0xFFE6 -> 2
        codePoint in 0x1F300..0x1F64F -> 2
        codePoint in 0x1F680..0x1F6FF -> 2
        codePoint in 0x1F900..0x1F9FF -> 2
        codePoint in 0x20000..0x3FFFD -> 2

        else -> 1
    }
