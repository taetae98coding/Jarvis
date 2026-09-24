package io.github.taetae98coding.jarvis.domain.terminal

data class TerminalSize(
    val columns: Int,
    val rows: Int,
) {
    companion object {
        // 창이 아직 배치되지 않아 격자를 계산할 수 없을 때 셸을 띄우는 크기. 배치되면 곧바로 바뀐다.
        val Default = TerminalSize(columns = 80, rows = 24)
    }
}
