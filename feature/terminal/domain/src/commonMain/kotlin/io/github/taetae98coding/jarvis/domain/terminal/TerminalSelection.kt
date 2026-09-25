package io.github.taetae98coding.jarvis.domain.terminal

/**
 * 끌어서 고른 칸 범위. [anchor] 는 누른 칸, [focus] 는 마우스가 있는 칸이고 둘 다 [scrolledLines] 시점의
 * [TerminalEmulator.line] 번호다(docs/common/terminal-selection.html).
 */
data class TerminalSelection(val anchor: TerminalCell, val focus: TerminalCell, val scrolledLines: Long)

/** 지금 화면 기준으로 읽는 순서(윗줄, 같은 줄이면 왼쪽)로 정렬한 범위. [start] 가 앞이다. */
data class TerminalSelectionBounds(val start: TerminalCell, val end: TerminalCell) {
    /** [row] 줄에서 선택된 칸 구간(R2). 첫 줄은 앞 칸부터 끝까지, 가운데 줄은 전부, 마지막 줄은 처음부터 뒤 칸까지다. */
    fun columnsAt(row: Int, columns: Int): IntRange? {
        if (row < start.row || row > end.row || columns <= 0) return null

        val from = if (row == start.row) start.column.coerceIn(0, columns - 1) else 0
        val to = if (row == end.row) end.column.coerceIn(0, columns - 1) else columns - 1

        return (from..to).takeUnless { it.isEmpty() }
    }
}

/**
 * [selection] 을 지금 화면의 줄 번호로 옮겨 정렬한 범위. 뒤 칸까지 스크롤백 밖으로 나갔으면 null(R4). 전각 글자는
 * 한쪽 칸만 닿아도 두 칸이 든다(R2).
 */
fun TerminalEmulator.selectionBounds(selection: TerminalSelection): TerminalSelectionBounds? {
    val shift = (scrolledLines - selection.scrolledLines).toInt()
    val anchor = selection.anchor.copy(row = selection.anchor.row - shift)
    val focus = selection.focus.copy(row = selection.focus.row - shift)
    var (start, end) = if (compareCells(anchor, focus) <= 0) anchor to focus else focus to anchor

    if (end.row < -scrollbackSize || start.row >= rows) return null
    if (start.row < -scrollbackSize) start = TerminalCell(-scrollbackSize, 0)
    if (end.row >= rows) end = TerminalCell(rows - 1, columns - 1)

    val first = line(start.row)
    val startColumn = start.column.coerceIn(0, first.columns - 1)
    if (first.isWideTail(startColumn)) start = start.copy(column = startColumn - 1)

    val last = line(end.row)
    val endColumn = end.column.coerceIn(0, last.columns - 1)
    if (last.isWide(endColumn)) end = end.copy(column = endColumn + 1)

    return TerminalSelectionBounds(start, end)
}

/**
 * 선택된 글자(R5). 줄의 오른쪽 끝 공백은 떼되 자동 줄바꿈된 줄은 다음 줄과 줄바꿈 없이 잇고, 나머지 줄 사이는 LF 다.
 * 끝에 이어진 빈 줄은 뺀다. 범위가 없으면 빈 문자열이다.
 */
fun TerminalEmulator.selectedText(selection: TerminalSelection): String {
    val bounds = selectionBounds(selection) ?: return ""
    val lines = ArrayList<String>()
    val current = StringBuilder()

    for (row in bounds.start.row..bounds.end.row) {
        val line = line(row)
        val range = bounds.columnsAt(row, line.columns) ?: continue
        val text = buildString {
            for (column in range) {
                if (line.isWideTail(column)) continue
                append(line.textAt(column).ifEmpty { " " })
            }
        }

        // 자동 줄바꿈된 줄은 마지막 칸까지 글자라 뗄 공백이 없고, 원래 한 줄이었으므로 잇는다.
        val joinsNext = line.wrapped && row < bounds.end.row
        current.append(if (joinsNext) text else text.trimEnd())
        if (!joinsNext) {
            lines += current.toString()
            current.clear()
        }
    }

    while (lines.isNotEmpty() && lines.last().isEmpty()) lines.removeAt(lines.lastIndex)

    return lines.joinToString("\n")
}

private fun compareCells(a: TerminalCell, b: TerminalCell): Int =
    compareValuesBy(a, b, { it.row }, { it.column })
