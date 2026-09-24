package io.github.taetae98coding.jarvis.ui.terminal

import androidx.compose.ui.graphics.Color
import io.github.taetae98coding.jarvis.domain.terminal.TerminalColor

internal val TerminalBackground = Color(0xFF1E1E1E)
internal val TerminalForeground = Color(0xFFD4D4D4)

// 0–15 는 VS Code 통합 터미널의 기본 테마. 16–231 은 6×6×6 색 정육면체, 232–255 는 회색 24단계로
// xterm 과 같다. 256색을 쓰는 프로그램은 이 둘이 xterm 과 같다고 가정한다.
private val Ansi = longArrayOf(
    0xFF000000, 0xFFCD3131, 0xFF0DBC79, 0xFFE5E510, 0xFF2472C8, 0xFFBC3FBC, 0xFF11A8CD, 0xFFE5E5E5,
    0xFF666666, 0xFFF14C4C, 0xFF23D18B, 0xFFF5F543, 0xFF3B8EEA, 0xFFD670D6, 0xFF29B8DB, 0xFFFFFFFF,
)

private val CubeLevels = intArrayOf(0, 95, 135, 175, 215, 255)

private val Palette: List<Color> = List(256) { index ->
    when {
        index < 16 -> Color(Ansi[index])
        index < 232 -> {
            val i = index - 16
            Color(CubeLevels[i / 36], CubeLevels[(i / 6) % 6], CubeLevels[i % 6])
        }

        else -> {
            val level = 8 + (index - 232) * 10
            Color(level, level, level)
        }
    }
}

internal fun TerminalColor.resolve(default: Color): Color {
    paletteIndex?.let { return Palette[it] }
    rgb?.let { return Color(0xFF000000 or it.toLong()) }

    return default
}
