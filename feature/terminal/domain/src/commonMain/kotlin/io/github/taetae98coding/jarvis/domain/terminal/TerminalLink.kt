package io.github.taetae98coding.jarvis.domain.terminal

/** 격자의 칸 하나. [row] 는 [TerminalEmulator.line] 의 번호다(음수는 스크롤백). */
data class TerminalCell(val row: Int, val column: Int)

/** 격자 위의 링크 하나. 여러 줄에 걸치면 [cells] 도 여러 줄이다(docs/common/terminal-link.html). */
data class TerminalLink(val url: String, val cells: List<TerminalCell>)

/**
 * 글자열에서 `http://`·`https://` 로 시작하는 주소 구간(R1b). 끝의 구두점과 여는 짝이 없는 닫는 괄호는 뺀다.
 */
fun findUrls(text: String): List<IntRange> {
    val ranges = ArrayList<IntRange>()
    var index = 0

    while (index < text.length) {
        val start = indexOfScheme(text, index)
        if (start < 0) break

        var end = start
        while (end < text.length && isUrlChar(text[end])) end++
        end = trimTrailing(text, start, end)

        val schemeEnd = text.indexOf("://", start) + 3
        if (end > schemeEnd) ranges += start until end
        index = maxOf(end, schemeEnd)
    }

    return ranges
}

/**
 * [row]·[column] 칸의 링크. OSC 8 주소가 붙어 있으면 그 주소가 이어진 칸 묶음, 아니면 그 줄을 앞뒤로 이은 글자열에서
 * 찾은 본문 주소. 링크가 아니면 null.
 */
fun TerminalEmulator.linkAt(row: Int, column: Int): TerminalLink? {
    if (row < -scrollbackSize || row >= rows) return null
    val line = line(row)
    if (column !in 0 until line.columns) return null

    line.linkAt(column)?.let { url -> return TerminalLink(url, hyperlinkCells(row, column, url)) }

    val joined = JoinedRows(this, row)
    val index = joined.indexOf(row, column) ?: return null
    val range = findUrls(joined.text).firstOrNull { index in it } ?: return null

    return TerminalLink(joined.text.substring(range), range.map(joined.cells::get))
}

private fun TerminalEmulator.hyperlinkCells(row: Int, column: Int, url: String): List<TerminalCell> {
    fun runStart(r: Int, c: Int): Int {
        var start = c
        while (start > 0 && line(r).linkAt(start - 1) == url) start--
        return start
    }

    fun runEnd(r: Int, c: Int): Int {
        var end = c
        while (end + 1 < line(r).columns && line(r).linkAt(end + 1) == url) end++
        return end
    }

    val first = runStart(row, column)
    val last = runEnd(row, column)

    // 앞줄 끝·뒷줄 처음에 같은 주소가 이어지면 한 링크다.
    val above = ArrayList<TerminalCell>()
    var r = row - 1
    var reachesStart = first == 0
    while (reachesStart && r >= -scrollbackSize && line(r).columns > 0 && line(r).linkAt(line(r).columns - 1) == url) {
        val end = line(r).columns - 1
        val start = runStart(r, end)
        for (c in end downTo start) above += TerminalCell(r, c)
        reachesStart = start == 0
        r--
    }

    val cells = ArrayList<TerminalCell>(above.asReversed())
    for (c in first..last) cells += TerminalCell(row, c)

    r = row + 1
    var reachesEnd = last == line(row).columns - 1
    while (reachesEnd && r < rows && line(r).columns > 0 && line(r).linkAt(0) == url) {
        val end = runEnd(r, 0)
        for (c in 0..end) cells += TerminalCell(r, c)
        reachesEnd = end == line(r).columns - 1
        r++
    }

    return cells
}

/**
 * [row] 를 앞뒤로 이은 글자열과 글자마다의 칸. 자동 줄바꿈(`wrapped`)으로 이어진 줄은 그대로 잇고, 프로그램이 스스로
 * 끊은 줄은 앞줄이 마지막 칸까지 주소 글자로 차 있고 뒷줄이 (들여쓰기 뒤) 주소 글자로 시작할 때만 잇는다(R2).
 * 빈칸과 전각의 둘째 칸은 공백 하나로 세어 글자와 칸이 1:1 이다.
 */
private class JoinedRows(private val emulator: TerminalEmulator, row: Int) {
    val text: String
    val cells: List<TerminalCell>
    private val starts = HashMap<Int, Int>()

    init {
        var first = row
        while (first - 1 >= -emulator.scrollbackSize && joins(first - 1, first)) first--
        var last = row
        while (last + 1 < emulator.rows && joins(last, last + 1)) last++

        val builder = StringBuilder()
        val cellList = ArrayList<TerminalCell>()
        for (r in first..last) {
            val line = emulator.line(r)
            // 이어진 뒷줄의 들여쓰기는 주소의 일부가 아니다. 자동 줄바꿈된 줄은 꽉 차 있어 뗄 것이 없다.
            val from = if (r > first && !emulator.line(r - 1).wrapped) leadingBlanks(line) else 0
            starts[r] = builder.length - from
            for (c in from until line.columns) {
                builder.append(charAt(line, c))
                cellList += TerminalCell(r, c)
            }
        }
        text = builder.toString()
        cells = cellList
    }

    fun indexOf(row: Int, column: Int): Int? {
        val start = starts[row] ?: return null
        val index = start + column

        return index.takeIf { it >= 0 && it < cells.size && cells[it] == TerminalCell(row, column) }
    }

    private fun joins(above: Int, below: Int): Boolean {
        val upper = emulator.line(above)
        if (upper.wrapped) return true

        val lower = emulator.line(below)
        val lastColumn = upper.columns - 1

        return lastColumn >= 0 && isUrlChar(charAt(upper, lastColumn)) &&
            leadingBlanks(lower).let { it < lower.columns && isUrlChar(charAt(lower, it)) }
    }

    // 프로그램이 찍은 공백(32)과 한 번도 쓰이지 않은 칸(0) 모두 빈칸이다.
    private fun leadingBlanks(line: TerminalLine): Int {
        var column = 0
        while (column < line.columns && line.codePointAt(column).let { it == 0 || it == ' '.code }) column++

        return column
    }

    private fun charAt(line: TerminalLine, column: Int): Char {
        val codePoint = line.codePointAt(column)

        return when {
            codePoint <= 0 -> ' '
            codePoint < 0x10000 -> codePoint.toChar()
            else -> '\uFFFD'
        }
    }
}

private fun indexOfScheme(text: String, from: Int): Int {
    var index = from
    while (index < text.length) {
        val h = text.indexOf("http", index, ignoreCase = true)
        if (h < 0) return -1
        if (text.startsWith("://", h + 4, ignoreCase = true) || text.startsWith("s://", h + 4, ignoreCase = true)) return h
        index = h + 1
    }

    return -1
}

private fun trimTrailing(text: String, start: Int, end: Int): Int {
    var last = end
    while (last > start) {
        val char = text[last - 1]
        val drop = when (char) {
            '.', ',', ';', ':', '!', '?' -> true
            ')' -> !balanced(text, start, last, '(', ')')
            ']' -> !balanced(text, start, last, '[', ']')
            '}' -> !balanced(text, start, last, '{', '}')
            else -> false
        }
        if (!drop) break
        last--
    }

    return last
}

private fun balanced(text: String, start: Int, end: Int, open: Char, close: Char): Boolean {
    var depth = 0
    for (i in start until end) {
        when (text[i]) {
            open -> depth++
            close -> depth--
        }
    }

    return depth >= 0
}

// 공백·제어 문자·꺾쇠·따옴표·전각 글자에서 주소가 끝난다(R1b).
private fun isUrlChar(char: Char): Boolean =
    char > ' ' && char < '\u007F' && char !in "<>\"'`"
