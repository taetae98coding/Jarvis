package io.github.taetae98coding.jarvis.domain.terminal

/**
 * 셸 출력 바이트를 글자 격자로 해석한다. 해석 범위는 docs/common/terminal.html#emulation 에 있다.
 *
 * 스레드 안전하지 않다. 한 스레드(화면은 메인)에서만 [feed] 하고 읽는다.
 */
class TerminalEmulator(
    columns: Int,
    rows: Int,
    private val scrollbackLimit: Int = DefaultScrollbackLimit,
) {
    var columns: Int = columns.coerceAtLeast(1)
        private set

    var rows: Int = rows.coerceAtLeast(1)
        private set

    /** OSC 0/2 로 셸이 정한 제목. */
    var title: String? = null
        private set

    var cursorRow: Int = 0
        private set

    var cursorColumn: Int = 0
        private set

    var cursorVisible: Boolean = true
        private set

    /** DECCKM. 켜져 있으면 방향키가 `ESC O A` 형태로 나가야 한다. */
    var applicationCursorKeys: Boolean = false
        private set

    var bracketedPaste: Boolean = false
        private set

    /** DECSET 1004. 셸의 줄 편집기는 켜지 않고 Claude Code 는 켠다(docs/common/terminal-line-comment.html#claude-ready). */
    var focusReporting: Boolean = false
        private set

    val isAlternateScreen: Boolean get() = screen === alternate

    /** 대체 화면에서는 스크롤백을 보여주지 않는다. `vim` 을 나가기 전의 화면이 섞이면 안 된다. */
    val scrollbackSize: Int get() = if (isAlternateScreen) 0 else scrollback.size

    private var main = MutableList(this.rows) { TerminalLine(this.columns, TerminalStyle.Default) }
    private var alternate = MutableList(this.rows) { TerminalLine(this.columns, TerminalStyle.Default) }
    private var screen = main
    private val scrollback = ArrayDeque<TerminalLine>()

    private var style = TerminalStyle.Default
    private var pendingWrap = false
    private var autoWrap = true
    private var insertMode = false
    private var lineDrawing = false
    private var scrollTop = 0
    private var scrollBottom = this.rows - 1
    private var saved = SavedCursor()
    private var lastPrinted = 0

    private var state = State.Ground
    private val params = StringBuilder()
    private var privateMarker: Char? = null
    private var intermediate: Char? = null
    private val osc = StringBuilder()

    private var utf8CodePoint = 0
    private var utf8Remaining = 0

    private val responses = ArrayList<Byte>()

    /**
     * 화면 위에서부터 센 줄. 음수는 스크롤백이다 — -1 이 화면 바로 위로 밀려난 줄이고
     * -[scrollbackSize] 가 가장 오래된 줄이다.
     */
    fun line(index: Int): TerminalLine =
        if (index >= 0) screen[index] else scrollback[scrollback.size + index]

    fun feed(bytes: ByteArray, offset: Int = 0, length: Int = bytes.size - offset) {
        for (i in offset until offset + length) {
            decode(bytes[i].toInt() and 0xFF)
        }
    }

    fun feed(text: String) = feed(text.encodeToByteArray())

    /** 셸에 되돌려 줄 바이트(DSR·DA 응답). 한 번 꺼내면 비워진다. */
    fun drainResponses(): ByteArray {
        val bytes = responses.toByteArray()
        responses.clear()

        return bytes
    }

    /**
     * 줄을 다시 흘리지 않고 자른다. 행이 줄면 커서 아래의 줄을 먼저 버리고, 그래도 모자라면 위쪽 줄을
     * 스크롤백으로 올려 커서가 화면 안에 남게 한다.
     */
    fun resize(columns: Int, rows: Int) {
        val newColumns = columns.coerceAtLeast(1)
        val newRows = rows.coerceAtLeast(1)
        if (newColumns == this.columns && newRows == this.rows) return

        val cursorShift = resizeScreen(main, newColumns, newRows, keepHistory = true, cursorOnThis = screen === main)
        resizeScreen(alternate, newColumns, newRows, keepHistory = false, cursorOnThis = screen === alternate)

        this.columns = newColumns
        this.rows = newRows
        cursorRow = (cursorRow - cursorShift).coerceIn(0, newRows - 1)
        cursorColumn = cursorColumn.coerceIn(0, newColumns - 1)
        saved = saved.copy(row = saved.row.coerceIn(0, newRows - 1), column = saved.column.coerceIn(0, newColumns - 1))
        scrollTop = 0
        scrollBottom = newRows - 1
        pendingWrap = false
    }

    private fun resizeScreen(
        lines: MutableList<TerminalLine>,
        newColumns: Int,
        newRows: Int,
        keepHistory: Boolean,
        cursorOnThis: Boolean,
    ): Int {
        lines.forEach { it.resize(newColumns, TerminalStyle.Default) }

        var removedFromTop = 0
        if (newRows < lines.size) {
            var excess = lines.size - newRows
            val below = if (cursorOnThis) lines.lastIndex - cursorRow else excess

            repeat(minOf(excess, below)) { lines.removeAt(lines.lastIndex) }
            excess -= minOf(excess, below)

            repeat(excess) {
                val line = lines.removeAt(0)
                if (keepHistory) pushScrollback(line)
            }
            removedFromTop = excess
        }
        while (lines.size < newRows) lines += TerminalLine(newColumns, TerminalStyle.Default)

        return if (cursorOnThis) removedFromTop else 0
    }

    private fun decode(byte: Int) {
        when {
            utf8Remaining > 0 && byte and 0xC0 == 0x80 -> {
                utf8CodePoint = (utf8CodePoint shl 6) or (byte and 0x3F)
                if (--utf8Remaining == 0) process(utf8CodePoint)
            }

            else -> {
                // 이어지는 바이트가 모자란 채 새 글자가 시작됐다. 깨진 글자를 하나 찍고 새로 읽는다.
                if (utf8Remaining > 0) {
                    utf8Remaining = 0
                    process(ReplacementCharacter)
                }

                when {
                    byte < 0x80 -> process(byte)
                    byte and 0xE0 == 0xC0 -> start(byte and 0x1F, 1)
                    byte and 0xF0 == 0xE0 -> start(byte and 0x0F, 2)
                    byte and 0xF8 == 0xF0 -> start(byte and 0x07, 3)
                    else -> process(ReplacementCharacter)
                }
            }
        }
    }

    private fun start(bits: Int, remaining: Int) {
        utf8CodePoint = bits
        utf8Remaining = remaining
    }

    private fun process(codePoint: Int) {
        when (state) {
            State.Ground -> if (isControl(codePoint)) control(codePoint) else print(codePoint)
            State.Escape -> escape(codePoint)
            State.EscapeCharset -> designate(codePoint)
            State.EscapeSkipOne -> state = State.Ground
            State.Csi -> csi(codePoint)
            State.Osc -> oscChar(codePoint)
            State.OscEscape -> if (codePoint == '\\'.code) finishOsc() else {
                osc.clear()
                state = State.Escape
                escape(codePoint)
            }
            State.IgnoreString -> if (codePoint == Esc) state = State.IgnoreStringEscape
            State.IgnoreStringEscape -> state = if (codePoint == '\\'.code) State.Ground else State.IgnoreString
        }
    }

    private fun isControl(codePoint: Int): Boolean = codePoint < 0x20 || codePoint == 0x7F

    private fun control(codePoint: Int) {
        when (codePoint) {
            Esc -> {
                state = State.Escape
                intermediate = null
            }

            0x08 -> {
                pendingWrap = false
                if (cursorColumn > 0) cursorColumn--
            }

            0x09 -> tab(1)
            0x0A, 0x0B, 0x0C -> lineFeed()
            0x0D -> {
                pendingWrap = false
                cursorColumn = 0
            }

            else -> Unit
        }
    }

    private fun print(input: Int) {
        val codePoint = if (lineDrawing && input in 0x5F..0x7E) LineDrawing[input - 0x5F] else input
        val width = terminalCharWidth(codePoint)

        if (width == 0) {
            attachCombining(codePoint)
            return
        }

        if (pendingWrap || (width == 2 && cursorColumn == columns - 1 && columns > 1)) {
            if (autoWrap) {
                screen[cursorRow].wrapped = true
                cursorColumn = 0
                lineFeed()
            } else if (width == 2) {
                cursorColumn = (columns - 2).coerceAtLeast(0)
            }
            pendingWrap = false
        }

        val line = screen[cursorRow]
        if (insertMode) {
            line.breakWideAround(cursorColumn, blank())
            line.insertBlanks(cursorColumn, width, blank())
        }

        line.breakWideAround(cursorColumn, blank())
        if (width == 2) line.breakWideAround(cursorColumn + 1, blank())

        line.set(cursorColumn, codePoint, style)
        if (width == 2 && cursorColumn + 1 < columns) line.set(cursorColumn + 1, TerminalLine.WideTail, style)
        lastPrinted = codePoint

        val next = cursorColumn + width
        if (next >= columns) {
            cursorColumn = columns - 1
            pendingWrap = autoWrap
        } else {
            cursorColumn = next
        }
    }

    private fun attachCombining(codePoint: Int) {
        var column = if (pendingWrap) cursorColumn else cursorColumn - 1
        val line = screen[cursorRow]
        if (column in 0 until columns && line.isWideTail(column)) column--
        if (column < 0) return

        line.appendCombining(column, codePoint)
    }

    private fun lineFeed() {
        pendingWrap = false
        when {
            cursorRow == scrollBottom -> scrollUp(1)
            cursorRow < rows - 1 -> cursorRow++
        }
    }

    private fun reverseIndex() {
        pendingWrap = false
        when {
            cursorRow == scrollTop -> scrollDown(1)
            cursorRow > 0 -> cursorRow--
        }
    }

    private fun scrollUp(count: Int) {
        repeat(count.coerceAtMost(scrollBottom - scrollTop + 1)) {
            val line = screen.removeAt(scrollTop)
            // xterm 과 같이 위쪽 여백이 화면 맨 위일 때만 스크롤백에 남긴다. 상태줄을 고정한 프로그램이
            // 중간 영역만 굴릴 때 그 줄들이 기록에 쌓이면 안 된다.
            if (scrollTop == 0 && screen === main) pushScrollback(line)
            screen.add(scrollBottom, TerminalLine(columns, blank()))
        }
    }

    private fun scrollDown(count: Int) {
        repeat(count.coerceAtMost(scrollBottom - scrollTop + 1)) {
            screen.removeAt(scrollBottom)
            screen.add(scrollTop, TerminalLine(columns, blank()))
        }
    }

    private fun pushScrollback(line: TerminalLine) {
        scrollback.addLast(line)
        while (scrollback.size > scrollbackLimit) scrollback.removeFirst()
    }

    private fun tab(count: Int) {
        pendingWrap = false
        repeat(count) {
            cursorColumn = ((cursorColumn / TabWidth) + 1) * TabWidth
        }
        cursorColumn = cursorColumn.coerceAtMost(columns - 1)
    }

    private fun backTab(count: Int) {
        pendingWrap = false
        repeat(count) {
            cursorColumn = if (cursorColumn == 0) 0 else ((cursorColumn - 1) / TabWidth) * TabWidth
        }
    }

    // 지울 때는 지금 배경색으로 채운다(xterm 의 BCE). 글자색과 속성은 남기지 않는다.
    private fun blank(): TerminalStyle = TerminalStyle.Default.withBackground(style.background)

    private fun escape(codePoint: Int) {
        state = State.Ground
        when (codePoint.toChar()) {
            '[' -> {
                params.clear()
                privateMarker = null
                intermediate = null
                state = State.Csi
            }

            ']' -> {
                osc.clear()
                state = State.Osc
            }

            'P', 'X', '^', '_' -> state = State.IgnoreString
            '(' -> state = State.EscapeCharset
            ')', '*', '+', '#', '%' -> state = State.EscapeSkipOne
            '7' -> saveCursor()
            '8' -> restoreCursor()
            'D' -> lineFeed()
            'E' -> {
                cursorColumn = 0
                lineFeed()
            }

            'M' -> reverseIndex()
            'c' -> reset()
            else -> if (codePoint == Esc) state = State.Escape
        }
    }

    private fun designate(codePoint: Int) {
        lineDrawing = codePoint == '0'.code
        state = State.Ground
    }

    private fun csi(codePoint: Int) {
        val char = codePoint.toChar()
        when {
            codePoint == Esc -> state = State.Escape
            codePoint == 0x18 || codePoint == 0x1A -> state = State.Ground
            isControl(codePoint) -> control(codePoint)
            char in '0'..'9' || char == ';' || char == ':' -> params.append(char)
            char in "?>=<" && params.isEmpty() && privateMarker == null -> privateMarker = char
            codePoint in 0x20..0x2F -> intermediate = char
            codePoint in 0x40..0x7E -> {
                state = State.Ground
                dispatchCsi(char)
            }

            else -> state = State.Ground
        }
    }

    private fun dispatchCsi(final: Char) {
        val groups = parseParams()
        fun arg(index: Int, default: Int): Int =
            groups.getOrNull(index)?.firstOrNull()?.takeIf { it >= 0 }?.let { if (it == 0 && default > 0) default else it } ?: default
        fun n(index: Int = 0): Int = arg(index, 1)

        if (intermediate != null) return

        when (privateMarker) {
            '?' -> {
                when (final) {
                    'h' -> groups.forEach { setPrivateMode(it.firstOrNull() ?: -1, true) }
                    'l' -> groups.forEach { setPrivateMode(it.firstOrNull() ?: -1, false) }
                }
                return
            }

            '>' -> {
                if (final == 'c') respond("\u001b[>0;10;1c")
                return
            }

            null -> Unit
            else -> return
        }

        when (final) {
            'A' -> moveCursor(cursorRow - n(), cursorColumn)
            'B', 'e' -> moveCursor(cursorRow + n(), cursorColumn)
            'C', 'a' -> moveCursor(cursorRow, cursorColumn + n())
            'D' -> moveCursor(cursorRow, cursorColumn - n())
            'E' -> moveCursor(cursorRow + n(), 0)
            'F' -> moveCursor(cursorRow - n(), 0)
            'G', '`' -> moveCursor(cursorRow, n() - 1)
            'H', 'f' -> moveCursor(n(0) - 1, n(1) - 1)
            'd' -> moveCursor(n() - 1, cursorColumn)
            'I' -> tab(n())
            'Z' -> backTab(n())
            'J' -> eraseDisplay(arg(0, 0))
            'K' -> eraseLine(arg(0, 0))
            '@' -> editLine { it.insertBlanks(cursorColumn, n(), blank()) }
            'P' -> editLine { it.deleteChars(cursorColumn, n(), blank()) }
            'X' -> editLine { it.clear(cursorColumn, cursorColumn + n(), blank()) }
            'L' -> insertLines(n())
            'M' -> deleteLines(n())
            'S' -> scrollUp(n())
            'T' -> scrollDown(n())
            'b' -> if (lastPrinted != 0) repeat(n().coerceAtMost(columns * rows)) { print(lastPrinted) }
            'm' -> selectGraphicRendition(groups)
            'r' -> setScrollRegion(arg(0, 1) - 1, arg(1, rows) - 1)
            's' -> saveCursor()
            'u' -> restoreCursor()
            'h' -> groups.forEach { if (it.firstOrNull() == 4) insertMode = true }
            'l' -> groups.forEach { if (it.firstOrNull() == 4) insertMode = false }
            'n' -> when (arg(0, 0)) {
                5 -> respond("\u001b[0n")
                6 -> respond("\u001b[${cursorRow + 1};${cursorColumn + 1}R")
            }

            'c' -> if (arg(0, 0) == 0) respond("\u001b[?1;2c")
            else -> Unit
        }
    }

    // `;` 가 인자를, `:` 가 한 인자 안의 하위 인자를 가른다. 비어 있는 자리는 -1 이다.
    private fun parseParams(): List<IntArray> {
        if (params.isEmpty()) return emptyList()

        return params.split(';').map { group ->
            group.split(':').map { it.toIntOrNull()?.coerceAtMost(MaxParam) ?: -1 }.toIntArray()
        }
    }

    private fun moveCursor(row: Int, column: Int) {
        pendingWrap = false
        cursorRow = row.coerceIn(0, rows - 1)
        cursorColumn = column.coerceIn(0, columns - 1)
    }

    private inline fun editLine(block: (TerminalLine) -> Unit) {
        pendingWrap = false
        val line = screen[cursorRow]
        line.breakWideAround(cursorColumn, blank())
        block(line)
    }

    private fun eraseDisplay(mode: Int) {
        pendingWrap = false
        when (mode) {
            0 -> {
                eraseLine(0)
                for (row in cursorRow + 1 until rows) screen[row].clear(0, columns, blank())
            }

            1 -> {
                eraseLine(1)
                for (row in 0 until cursorRow) screen[row].clear(0, columns, blank())
            }

            2 -> screen.forEach { it.clear(0, columns, blank()) }
            3 -> scrollback.clear()
        }
    }

    private fun eraseLine(mode: Int) {
        pendingWrap = false
        val line = screen[cursorRow]
        line.breakWideAround(cursorColumn, blank())
        when (mode) {
            0 -> {
                line.clear(cursorColumn, columns, blank())
                line.wrapped = false
            }

            1 -> line.clear(0, cursorColumn + 1, blank())
            2 -> {
                line.clear(0, columns, blank())
                line.wrapped = false
            }
        }
    }

    private fun insertLines(count: Int) {
        if (cursorRow !in scrollTop..scrollBottom) return
        pendingWrap = false

        repeat(count.coerceAtMost(scrollBottom - cursorRow + 1)) {
            screen.removeAt(scrollBottom)
            screen.add(cursorRow, TerminalLine(columns, blank()))
        }
        cursorColumn = 0
    }

    private fun deleteLines(count: Int) {
        if (cursorRow !in scrollTop..scrollBottom) return
        pendingWrap = false

        repeat(count.coerceAtMost(scrollBottom - cursorRow + 1)) {
            screen.removeAt(cursorRow)
            screen.add(scrollBottom, TerminalLine(columns, blank()))
        }
        cursorColumn = 0
    }

    private fun setScrollRegion(top: Int, bottom: Int) {
        val t = top.coerceIn(0, rows - 1)
        val b = bottom.coerceIn(0, rows - 1)
        if (t >= b) return

        scrollTop = t
        scrollBottom = b
        moveCursor(0, 0)
    }

    private fun setPrivateMode(mode: Int, enabled: Boolean) {
        when (mode) {
            1 -> applicationCursorKeys = enabled
            7 -> autoWrap = enabled
            25 -> cursorVisible = enabled
            47, 1047 -> switchScreen(alternateScreen = enabled, clear = enabled && mode == 1047)
            1048 -> if (enabled) saveCursor() else restoreCursor()
            1049 -> if (enabled) {
                saveCursor()
                switchScreen(alternateScreen = true, clear = true)
            } else {
                switchScreen(alternateScreen = false, clear = false)
                restoreCursor()
            }

            1004 -> focusReporting = enabled
            2004 -> bracketedPaste = enabled
        }
    }

    private fun switchScreen(alternateScreen: Boolean, clear: Boolean) {
        val target = if (alternateScreen) alternate else main
        if (clear) target.forEach { it.clear(0, columns, TerminalStyle.Default) }
        screen = target
        scrollTop = 0
        scrollBottom = rows - 1
        pendingWrap = false
    }

    private fun selectGraphicRendition(groups: List<IntArray>) {
        if (groups.isEmpty()) {
            style = TerminalStyle.Default
            return
        }

        var i = 0
        while (i < groups.size) {
            val group = groups[i]
            when (val code = group.firstOrNull()?.takeIf { it >= 0 } ?: 0) {
                0 -> style = TerminalStyle.Default
                1 -> style = style.with(TerminalStyle.Bold, true)
                2 -> style = style.with(TerminalStyle.Dim, true)
                3 -> style = style.with(TerminalStyle.Italic, true)
                4 -> style = style.with(TerminalStyle.Underline, group.getOrNull(1) != 0)
                7 -> style = style.with(TerminalStyle.Inverse, true)
                8 -> style = style.with(TerminalStyle.Hidden, true)
                9 -> style = style.with(TerminalStyle.Strikethrough, true)
                21, 24 -> style = style.with(TerminalStyle.Underline, false)
                22 -> style = style.with(TerminalStyle.Bold, false).with(TerminalStyle.Dim, false)
                23 -> style = style.with(TerminalStyle.Italic, false)
                27 -> style = style.with(TerminalStyle.Inverse, false)
                28 -> style = style.with(TerminalStyle.Hidden, false)
                29 -> style = style.with(TerminalStyle.Strikethrough, false)
                in 30..37 -> style = style.withForeground(TerminalColor.palette(code - 30))
                39 -> style = style.withForeground(TerminalColor.Default)
                in 40..47 -> style = style.withBackground(TerminalColor.palette(code - 40))
                49 -> style = style.withBackground(TerminalColor.Default)
                in 90..97 -> style = style.withForeground(TerminalColor.palette(code - 90 + 8))
                in 100..107 -> style = style.withBackground(TerminalColor.palette(code - 100 + 8))
                38, 48 -> {
                    val (color, consumed) = extendedColor(groups, i)
                    if (color != null) {
                        style = if (code == 38) style.withForeground(color) else style.withBackground(color)
                    }
                    i += consumed
                }
            }
            i++
        }
    }

    /**
     * `38;5;n`·`38;2;r;g;b` 와 콜론 표기 `38:5:n`·`38:2::r:g:b`(색 공간 자리가 비어 있다)를 모두 받는다.
     * 두 번째 값은 세미콜론 표기에서 뒤따라 소비한 인자 수다.
     */
    private fun extendedColor(groups: List<IntArray>, index: Int): Pair<TerminalColor?, Int> {
        val group = groups[index]
        if (group.size > 1) {
            return when (group[1]) {
                5 -> group.getOrNull(2)?.takeIf { it >= 0 }?.let(TerminalColor::palette) to 0
                2 -> {
                    val rgb = if (group.size >= 6) group.copyOfRange(3, 6) else group.copyOfRange(2, group.size)
                    (if (rgb.size == 3) TerminalColor.rgb(rgb[0], rgb[1], rgb[2]) else null) to 0
                }

                else -> null to 0
            }
        }

        fun at(offset: Int): Int = groups.getOrNull(index + offset)?.firstOrNull() ?: -1

        return when (at(1)) {
            5 -> at(2).takeIf { it >= 0 }?.let(TerminalColor::palette) to 2
            2 -> TerminalColor.rgb(at(2), at(3), at(4)) to 4
            else -> null to 0
        }
    }

    private fun oscChar(codePoint: Int) {
        when {
            codePoint == Bel -> finishOsc()
            codePoint == Esc -> state = State.OscEscape
            osc.length < MaxOscLength -> osc.append(codePointToString(codePoint))
        }
    }

    private fun finishOsc() {
        state = State.Ground
        val text = osc.toString()
        osc.clear()

        val separator = text.indexOf(';')
        if (separator < 0) return

        when (text.substring(0, separator)) {
            "0", "2" -> title = text.substring(separator + 1)
        }
    }

    private fun saveCursor() {
        saved = SavedCursor(cursorRow, cursorColumn, style, lineDrawing, pendingWrap)
    }

    private fun restoreCursor() {
        cursorRow = saved.row.coerceIn(0, rows - 1)
        cursorColumn = saved.column.coerceIn(0, columns - 1)
        style = saved.style
        lineDrawing = saved.lineDrawing
        pendingWrap = saved.pendingWrap
    }

    private fun reset() {
        main.forEach { it.clear(0, columns, TerminalStyle.Default) }
        alternate.forEach { it.clear(0, columns, TerminalStyle.Default) }
        screen = main
        style = TerminalStyle.Default
        cursorRow = 0
        cursorColumn = 0
        cursorVisible = true
        applicationCursorKeys = false
        bracketedPaste = false
        focusReporting = false
        autoWrap = true
        insertMode = false
        lineDrawing = false
        pendingWrap = false
        scrollTop = 0
        scrollBottom = rows - 1
        saved = SavedCursor()
    }

    private fun respond(text: String) {
        text.encodeToByteArray().forEach { responses += it }
    }

    private data class SavedCursor(
        val row: Int = 0,
        val column: Int = 0,
        val style: TerminalStyle = TerminalStyle.Default,
        val lineDrawing: Boolean = false,
        val pendingWrap: Boolean = false,
    )

    private enum class State {
        Ground,
        Escape,
        EscapeCharset,
        EscapeSkipOne,
        Csi,
        Osc,
        OscEscape,
        IgnoreString,
        IgnoreStringEscape,
    }

    companion object {
        const val DefaultScrollbackLimit = 2000

        private const val Esc = 0x1B
        private const val Bel = 0x07
        private const val TabWidth = 8
        private const val ReplacementCharacter = 0xFFFD
        private const val MaxParam = 65535
        private const val MaxOscLength = 4096

        // DEC Special Graphics. `ESC ( 0` 뒤의 0x5F–0x7E 가 선그리기 문자가 된다. tmux·htop 의 테두리가 쓴다.
        private val LineDrawing = intArrayOf(
            0x00A0, 0x25C6, 0x2592, 0x2409, 0x240C, 0x240D, 0x240A, 0x00B0,
            0x00B1, 0x2424, 0x240B, 0x2518, 0x2510, 0x250C, 0x2514, 0x253C,
            0x23BA, 0x23BB, 0x2500, 0x23BC, 0x23BD, 0x251C, 0x2524, 0x2534,
            0x252C, 0x2502, 0x2264, 0x2265, 0x03C0, 0x2260, 0x00A3, 0x00B7,
        )
    }
}
