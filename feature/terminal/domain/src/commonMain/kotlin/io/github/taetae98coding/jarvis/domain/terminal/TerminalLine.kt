package io.github.taetae98coding.jarvis.domain.terminal

/**
 * 격자의 한 줄. 칸마다 코드 포인트 하나와 스타일 하나를 배열로 들고 있다.
 *
 * 코드 포인트 0 은 빈칸이다. 전각 문자는 두 칸을 차지하고 둘째 칸에 [WideTail] 을 둔다. 결합 문자는
 * 칸을 차지하지 않아서 앞 칸에 붙여 드문드문 보관한다.
 */
class TerminalLine internal constructor(columns: Int, fill: TerminalStyle) {
    private var codePoints = IntArray(columns)
    private var styles = LongArray(columns) { fill.bits }
    private var combining: MutableMap<Int, String>? = null

    /** 자동 줄바꿈으로 다음 줄에 이어진다. */
    var wrapped: Boolean = false
        internal set

    val columns: Int get() = codePoints.size

    fun codePointAt(column: Int): Int = codePoints[column]

    fun styleAt(column: Int): TerminalStyle = TerminalStyle(styles[column])

    fun isWideTail(column: Int): Boolean = codePoints[column] == WideTail

    fun isWide(column: Int): Boolean = column + 1 < columns && codePoints[column + 1] == WideTail

    /** 그릴 글자. 빈칸과 전각의 둘째 칸은 빈 문자열이다. */
    fun textAt(column: Int): String {
        val codePoint = codePoints[column]
        if (codePoint <= 0) return ""

        val base = codePointToString(codePoint)

        return combining?.get(column)?.let { base + it } ?: base
    }

    /** 오른쪽 빈칸을 뺀 줄 글자. 테스트와 디버깅용이다. */
    fun text(): String = buildString {
        for (column in 0 until columns) {
            when (val codePoint = codePoints[column]) {
                WideTail -> Unit
                0 -> append(' ')
                else -> append(textAt(column))
            }
        }
    }.trimEnd()

    internal fun set(column: Int, codePoint: Int, style: TerminalStyle) {
        codePoints[column] = codePoint
        styles[column] = style.bits
        combining?.remove(column)
    }

    internal fun appendCombining(column: Int, codePoint: Int) {
        if (codePoints[column] <= 0) return

        val map = combining ?: mutableMapOf<Int, String>().also { combining = it }
        map[column] = (map[column] ?: "") + codePointToString(codePoint)
    }

    internal fun clear(from: Int, to: Int, fill: TerminalStyle) {
        for (column in from.coerceAtLeast(0) until to.coerceAtMost(columns)) {
            set(column, 0, fill)
        }
    }

    /** 전각 문자의 한쪽만 덮이면 남은 반쪽을 지운다. 반쪽짜리 글자는 그릴 수 없다. */
    internal fun breakWideAround(column: Int, fill: TerminalStyle) {
        if (column !in 0 until columns) return

        if (codePoints[column] == WideTail && column > 0) set(column - 1, 0, fill)
        if (isWide(column)) set(column + 1, 0, fill)
    }

    internal fun insertBlanks(column: Int, count: Int, fill: TerminalStyle) {
        if (column !in 0 until columns) return
        val n = count.coerceAtMost(columns - column)

        codePoints.copyInto(codePoints, column + n, column, columns - n)
        styles.copyInto(styles, column + n, column, columns - n)
        shiftCombining(from = column, by = n)
        clear(column, column + n, fill)
        repairEdges(fill)
    }

    internal fun deleteChars(column: Int, count: Int, fill: TerminalStyle) {
        if (column !in 0 until columns) return
        val n = count.coerceAtMost(columns - column)

        codePoints.copyInto(codePoints, column, column + n, columns)
        styles.copyInto(styles, column, column + n, columns)
        shiftCombining(from = column + n, by = -n, dropFrom = column)
        clear(columns - n, columns, fill)
        repairEdges(fill)
    }

    internal fun resize(newColumns: Int, fill: TerminalStyle) {
        if (newColumns == columns) return

        val old = columns
        codePoints = codePoints.copyOf(newColumns)
        styles = styles.copyOf(newColumns)
        for (column in old until newColumns) styles[column] = fill.bits
        combining?.keys?.removeAll { it >= newColumns }
        repairEdges(fill)
    }

    // 밀거나 자르면 전각의 한쪽이 줄 끝에서 떨어지거나 둘째 칸만 맨 앞에 남는다.
    private fun repairEdges(fill: TerminalStyle) {
        if (columns == 0) return
        if (codePoints[0] == WideTail) set(0, 0, fill)
        if (codePoints[columns - 1] != WideTail && isWideHeadAtEnd()) set(columns - 1, 0, fill)
        for (column in 1 until columns) {
            if (codePoints[column] == WideTail && codePoints[column - 1] <= 0) set(column, 0, fill)
        }
    }

    private fun isWideHeadAtEnd(): Boolean {
        val last = codePoints[columns - 1]

        return last > 0 && terminalCharWidth(last) == 2
    }

    private fun shiftCombining(from: Int, by: Int, dropFrom: Int = from) {
        val map = combining ?: return
        val moved = map.filterKeys { it >= from }.mapKeys { it.key + by }
        map.keys.removeAll { it >= dropFrom }
        moved.filterKeys { it in 0 until columns }.forEach { (k, v) -> map[k] = v }
    }

    companion object {
        const val WideTail = -1
    }
}

internal fun codePointToString(codePoint: Int): String =
    if (codePoint < 0x10000) {
        codePoint.toChar().toString()
    } else {
        val offset = codePoint - 0x10000
        charArrayOf(
            (0xD800 + (offset shr 10)).toChar(),
            (0xDC00 + (offset and 0x3FF)).toChar(),
        ).concatToString()
    }
