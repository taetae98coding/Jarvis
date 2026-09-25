package io.github.taetae98coding.jarvis.domain.terminal

/** 마크다운 미리보기가 그리는 블록(docs/common/terminal-file-editor.html M2). */
sealed interface MarkdownBlock {
    data class Heading(val level: Int, val content: List<MarkdownInline>) : MarkdownBlock

    data class Paragraph(val content: List<MarkdownInline>) : MarkdownBlock

    /** [language] 는 울타리 뒤 첫 낱말이다. 들여쓴 코드와 이름 없는 울타리는 null. */
    data class CodeBlock(val language: String?, val text: String) : MarkdownBlock

    data class Quote(val blocks: List<MarkdownBlock>) : MarkdownBlock

    data class ListBlock(val ordered: Boolean, val start: Int, val items: List<MarkdownListItem>) : MarkdownBlock

    /** 모든 줄은 [header] 와 칸 수가 같다. */
    data class Table(
        val header: List<List<MarkdownInline>>,
        val alignments: List<MarkdownAlignment>,
        val rows: List<List<List<MarkdownInline>>>,
    ) : MarkdownBlock

    data object Rule : MarkdownBlock
}

/** [checked] 는 작업 목록이 아니면 null 이다. */
data class MarkdownListItem(val checked: Boolean?, val blocks: List<MarkdownBlock>)

enum class MarkdownAlignment {
    Start,
    Center,
    End,
}

sealed interface MarkdownInline {
    data class Text(val text: String) : MarkdownInline

    data class Code(val text: String) : MarkdownInline

    data class Emphasis(val children: List<MarkdownInline>) : MarkdownInline

    data class Strong(val children: List<MarkdownInline>) : MarkdownInline

    data class Strike(val children: List<MarkdownInline>) : MarkdownInline

    data class Link(val children: List<MarkdownInline>, val url: String) : MarkdownInline

    data class Image(val alt: String, val url: String) : MarkdownInline

    data object LineBreak : MarkdownInline
}

fun parseMarkdown(text: String): List<MarkdownBlock> = parseBlocks(text.lines().map { it.replace("\t", "    ") })

/**
 * 마크다운 파일 [file] 안의 링크 [link] 를 파일 경로로 푼다. 주소(`https:` 처럼 scheme 이 있는 것)와 `#` 로만 된 문서 안
 * 링크는 null 이다(M3). `/` 로 시작하면 그대로 절대 경로다.
 */
fun resolveRelativePath(file: String, link: String): String? {
    val target = link.substringBefore('#').substringBefore('?')
    if (target.isEmpty() || UrlScheme.containsMatchIn(target)) return null

    val base = if (target.startsWith("/")) emptyList() else file.substringBeforeLast('/', missingDelimiterValue = "").split('/')
    val segments = mutableListOf<String>()
    (base + percentDecode(target).split('/')).forEach { segment ->
        when (segment) {
            "", "." -> Unit
            ".." -> if (segments.isNotEmpty()) segments.removeAt(segments.lastIndex)
            else -> segments += segment
        }
    }

    return segments.joinToString(separator = "/", prefix = "/")
}

private val UrlScheme = Regex("^[a-zA-Z][a-zA-Z0-9+.-]*:")

private fun percentDecode(value: String): String {
    if ('%' !in value) return value

    val bytes = mutableListOf<Byte>()
    var i = 0
    while (i < value.length) {
        val hex = if (value[i] == '%' && i + 2 < value.length) value.substring(i + 1, i + 3).toIntOrNull(16) else null
        if (hex != null) {
            bytes += hex.toByte()
            i += 3
        } else {
            value[i].toString().encodeToByteArray().forEach { bytes += it }
            i++
        }
    }
    return bytes.toByteArray().decodeToString()
}

private val AtxHeading = Regex("""^ {0,3}(#{1,6})(?:[ ]+(.*?))?(?:[ ]+#+)?[ ]*$""")
private val ThematicBreak = Regex("""^ {0,3}([-*_])(?:[ ]*\1){2,}[ ]*$""")
private val FenceOpen = Regex("""^( {0,3})(`{3,}|~{3,})(.*)$""")
private val ListMarker = Regex("""^( {0,3})([-*+]|(\d{1,9})([.)]))( +|$)(.*)$""")
private val TaskMarker = Regex("""^\[([ xX])](?: +(.*)|$)""")
private val SetextUnderline = Regex("""^ {0,3}(=+|-+)[ ]*$""")
private val QuoteMarker = Regex("""^ {0,3}> ?""")
private val DelimiterCell = Regex("""^:?-+:?$""")

private data class ListMarkerMatch(
    val ordered: Boolean,
    val bullet: Char,
    val number: Int,
    val contentIndent: Int,
    val content: String,
)

private fun listMarker(line: String): ListMarkerMatch? {
    if (ThematicBreak.matches(line)) return null
    val match = ListMarker.matchEntire(line) ?: return null

    val indent = match.groupValues[1].length
    val marker = match.groupValues[2]
    val spaces = match.groupValues[5].length
    val content = match.groupValues[6]
    val ordered = match.groupValues[3].isNotEmpty()
    // 표시 뒤 공백이 다섯 칸 이상이면 들여쓴 코드지만, 여기서는 한 칸만 표시에 붙이고 나머지는 내용으로 본다.
    val gap = if (content.isEmpty() || spaces > 4) 1 else spaces

    return ListMarkerMatch(
        ordered = ordered,
        bullet = if (ordered) match.groupValues[4][0] else marker[0],
        number = match.groupValues[3].toIntOrNull() ?: 1,
        contentIndent = indent + marker.length + gap,
        content = if (spaces > 4) " ".repeat(spaces - 1) + content else content,
    )
}

private fun fenceOf(line: String): Triple<Int, String, String>? {
    val match = FenceOpen.matchEntire(line) ?: return null
    val marker = match.groupValues[2]
    val info = match.groupValues[3].trim()
    // 백틱 울타리의 정보 글에는 백틱이 올 수 없다. 그러면 인라인 코드다.
    if (marker[0] == '`' && '`' in info) return null

    return Triple(match.groupValues[1].length, marker, info)
}

private fun isFenceClose(line: String, marker: String): Boolean {
    val trimmed = line.trim()
    return leadingSpaces(line) <= 3 && trimmed.length >= marker.length && trimmed.all { it == marker[0] }
}

private fun leadingSpaces(line: String): Int = line.length - line.trimStart(' ').length

private fun startsBlock(line: String): Boolean {
    if (fenceOf(line) != null || AtxHeading.matches(line) || ThematicBreak.matches(line) || QuoteMarker.containsMatchIn(line)) return true

    // 번호 목록은 1 로 시작할 때만 문단을 끊는다(CommonMark 5.3). 빈 항목도 끊지 않는다.
    val marker = listMarker(line) ?: return false
    return marker.content.isNotBlank() && (!marker.ordered || marker.number == 1)
}

private fun splitTableRow(line: String): List<String> {
    var row = line.trim()
    if (row.startsWith("|")) row = row.drop(1)
    if (row.endsWith("|") && !row.endsWith("\\|")) row = row.dropLast(1)

    val cells = mutableListOf<String>()
    val cell = StringBuilder()
    var i = 0
    var inCode = false
    while (i < row.length) {
        val c = row[i]
        when {
            c == '\\' && i + 1 < row.length && row[i + 1] == '|' -> {
                cell.append('|')
                i += 2
                continue
            }
            c == '`' -> inCode = !inCode
            c == '|' && !inCode -> {
                cells += cell.toString().trim()
                cell.clear()
                i++
                continue
            }
        }
        cell.append(c)
        i++
    }
    cells += cell.toString().trim()

    return cells
}

private fun delimiterRow(line: String): List<MarkdownAlignment>? {
    if ('-' !in line) return null
    val cells = splitTableRow(line)
    if (cells.isEmpty() || cells.any { !DelimiterCell.matches(it) }) return null

    return cells.map { cell ->
        when {
            cell.startsWith(":") && cell.endsWith(":") -> MarkdownAlignment.Center
            cell.endsWith(":") -> MarkdownAlignment.End
            else -> MarkdownAlignment.Start
        }
    }
}

private fun tableStartsAt(lines: List<String>, i: Int): List<MarkdownAlignment>? {
    if ('|' !in lines[i] || i + 1 >= lines.size) return null
    val alignments = delimiterRow(lines[i + 1]) ?: return null

    return alignments.takeIf { it.size == splitTableRow(lines[i]).size }
}

private fun parseBlocks(lines: List<String>): List<MarkdownBlock> {
    val blocks = mutableListOf<MarkdownBlock>()
    var i = 0

    while (i < lines.size) {
        val line = lines[i]
        if (line.isBlank()) {
            i++
            continue
        }

        val fence = fenceOf(line)
        if (fence != null) {
            val (indent, marker, info) = fence
            val body = mutableListOf<String>()
            i++
            while (i < lines.size && !isFenceClose(lines[i], marker)) {
                val current = lines[i]
                body += current.drop(minOf(indent, leadingSpaces(current)))
                i++
            }
            i++
            blocks += MarkdownBlock.CodeBlock(info.substringBefore(' ').takeIf { it.isNotEmpty() }, body.joinToString("\n"))
            continue
        }

        val heading = AtxHeading.matchEntire(line)
        if (heading != null) {
            blocks += MarkdownBlock.Heading(heading.groupValues[1].length, parseInlines(heading.groupValues[2].trim()))
            i++
            continue
        }

        if (ThematicBreak.matches(line)) {
            blocks += MarkdownBlock.Rule
            i++
            continue
        }

        if (QuoteMarker.containsMatchIn(line)) {
            val quoted = mutableListOf<String>()
            while (i < lines.size) {
                val current = lines[i]
                val marker = QuoteMarker.find(current)
                when {
                    marker != null -> quoted += current.substring(marker.range.last + 1)
                    // 느슨한 이어짐: 인용 안 문단 뒤의 표시 없는 줄도 그 문단이다.
                    current.isNotBlank() && !startsBlock(current) && quoted.lastOrNull()?.isNotBlank() == true -> quoted += current
                    else -> break
                }
                i++
            }
            blocks += MarkdownBlock.Quote(parseBlocks(quoted))
            continue
        }

        val marker = listMarker(line)
        if (marker != null) {
            val (block, next) = parseList(lines, i, marker)
            blocks += block
            i = next
            continue
        }

        if (leadingSpaces(line) >= 4) {
            val body = mutableListOf<String>()
            while (i < lines.size && (lines[i].isBlank() || leadingSpaces(lines[i]) >= 4)) {
                body += lines[i].drop(minOf(4, leadingSpaces(lines[i])))
                i++
            }
            while (body.lastOrNull()?.isBlank() == true) body.removeAt(body.lastIndex)
            blocks += MarkdownBlock.CodeBlock(null, body.joinToString("\n"))
            continue
        }

        val alignments = tableStartsAt(lines, i)
        if (alignments != null) {
            val header = splitTableRow(line).map(::parseInlines)
            val rows = mutableListOf<List<List<MarkdownInline>>>()
            i += 2
            while (i < lines.size && lines[i].isNotBlank() && !startsBlock(lines[i])) {
                val cells = splitTableRow(lines[i])
                rows += List(header.size) { index -> parseInlines(cells.getOrElse(index) { "" }) }
                i++
            }
            blocks += MarkdownBlock.Table(header, alignments, rows)
            continue
        }

        val paragraph = mutableListOf(line.trimStart())
        i++
        var setextLevel = 0
        while (i < lines.size) {
            val current = lines[i]
            if (current.isBlank()) break
            val underline = SetextUnderline.matchEntire(current)
            if (underline != null) {
                setextLevel = if (underline.groupValues[1][0] == '=') 1 else 2
                i++
                break
            }
            if (startsBlock(current) || tableStartsAt(lines, i) != null) break
            paragraph += current.trimStart()
            i++
        }
        val content = parseInlines(paragraph.joinToString("\n").trimEnd())
        blocks += if (setextLevel > 0) MarkdownBlock.Heading(setextLevel, content) else MarkdownBlock.Paragraph(content)
    }

    return blocks
}

private fun parseList(lines: List<String>, from: Int, first: ListMarkerMatch): Pair<MarkdownBlock, Int> {
    val items = mutableListOf<MarkdownListItem>()
    var i = from

    while (i < lines.size) {
        val marker = listMarker(lines[i]) ?: break
        if (marker.ordered != first.ordered || marker.bullet != first.bullet) break

        val itemLines = mutableListOf(marker.content)
        i++
        var sawBlank = false
        while (i < lines.size) {
            val current = lines[i]
            when {
                current.isBlank() -> {
                    itemLines += ""
                    sawBlank = true
                }
                leadingSpaces(current) >= marker.contentIndent -> {
                    itemLines += current.substring(marker.contentIndent)
                    sawBlank = false
                }
                sawBlank || listMarker(current) != null || startsBlock(current) -> break
                else -> itemLines += current.trimStart()
            }
            i++
        }
        while (itemLines.lastOrNull()?.isBlank() == true) itemLines.removeAt(itemLines.lastIndex)

        val task = TaskMarker.matchEntire(itemLines.firstOrNull().orEmpty())
        val checked = task?.let { it.groupValues[1] != " " }
        if (task != null) itemLines[0] = task.groupValues[2]
        items += MarkdownListItem(checked, parseBlocks(itemLines))
    }

    return MarkdownBlock.ListBlock(first.ordered, first.number, items) to i
}

private const val AsciiPunctuation = "!\"#$%&'()*+,-./:;<=>?@[\\]^_`{|}~"

private val AutoLinkAngle = Regex("""^<((?:https?|mailto):[^\s<>]+)>""")

private val BareUrl = Regex("""^https?://[^\s<]+""")

// 자동 링크를 찾을 때 문단 끝까지 잘라 보지 않게 한다. 이보다 긴 주소는 잘린다.
private const val MaxUrlLength = 2048

internal fun parseInlines(text: String): List<MarkdownInline> = InlineParser(text).parse()

private class InlineParser(private val s: String) {
    private val out = mutableListOf<MarkdownInline>()
    private val buffer = StringBuilder()

    fun parse(): List<MarkdownInline> {
        var i = 0
        while (i < s.length) {
            i = step(i)
        }
        flush()
        return out
    }

    private fun flush() {
        if (buffer.isNotEmpty()) {
            out += MarkdownInline.Text(buffer.toString())
            buffer.clear()
        }
    }

    private fun add(inline: MarkdownInline) {
        flush()
        out += inline
    }

    private fun step(i: Int): Int {
        val c = s[i]
        return when {
            c == '\\' && i + 1 < s.length && s[i + 1] == '\n' -> {
                add(MarkdownInline.LineBreak)
                i + 2
            }
            c == '\\' && i + 1 < s.length && s[i + 1] in AsciiPunctuation -> {
                buffer.append(s[i + 1])
                i + 2
            }
            c == '\n' -> lineBreak(i)
            c == '`' -> codeSpan(i)
            c == '!' && i + 1 < s.length && s[i + 1] == '[' -> link(i + 1)?.let { (label, url, end) ->
                add(MarkdownInline.Image(plainText(parseInlines(label)), url))
                end
            } ?: literal(i)
            c == '[' -> link(i)?.let { (label, url, end) ->
                add(MarkdownInline.Link(parseInlines(label), url))
                end
            } ?: literal(i)
            c == '<' -> AutoLinkAngle.find(s.substring(i, minOf(s.length, i + MaxUrlLength)))?.let { match ->
                val url = match.groupValues[1]
                add(MarkdownInline.Link(listOf(MarkdownInline.Text(url)), url))
                i + match.value.length
            } ?: literal(i)
            c == 'h' && (s.startsWith("http://", i) || s.startsWith("https://", i)) && (i == 0 || s[i - 1].isWhitespace() || s[i - 1] == '(') ->
                bareUrl(i) ?: literal(i)
            c == '~' && s.startsWith("~~", i) -> delimited(i, "~~") { MarkdownInline.Strike(it) } ?: literal(i, 2)
            (c == '*' || c == '_') && i + 1 < s.length && s[i + 1] == c -> delimited(i, "$c$c") { MarkdownInline.Strong(it) } ?: literal(i, 2)
            c == '*' || c == '_' -> delimited(i, "$c") { MarkdownInline.Emphasis(it) } ?: literal(i)
            else -> literal(i)
        }
    }

    private fun literal(i: Int, count: Int = 1): Int {
        buffer.append(s, i, minOf(i + count, s.length))
        return i + count
    }

    // 줄 끝 공백 둘 이상은 줄바꿈이고, 그 밖의 줄바꿈은 공백 하나다.
    private fun lineBreak(i: Int): Int {
        val hard = buffer.endsWith("  ")
        while (buffer.endsWith(" ")) buffer.setLength(buffer.length - 1)
        if (hard) add(MarkdownInline.LineBreak) else buffer.append(' ')
        var next = i + 1
        while (next < s.length && s[next] == ' ') next++
        return next
    }

    private fun codeSpan(i: Int): Int {
        var run = 0
        while (i + run < s.length && s[i + run] == '`') run++
        val close = closingBackticks(i + run, run) ?: return literal(i, run)

        var code = s.substring(i + run, close).replace('\n', ' ')
        if (code.length >= 2 && code.startsWith(' ') && code.endsWith(' ') && code.isNotBlank()) code = code.substring(1, code.length - 1)
        add(MarkdownInline.Code(code))
        return close + run
    }

    private fun closingBackticks(from: Int, run: Int): Int? {
        var k = from
        while (k < s.length) {
            if (s[k] != '`') {
                k++
                continue
            }
            var length = 0
            while (k + length < s.length && s[k + length] == '`') length++
            if (length == run) return k
            k += length
        }
        return null
    }

    private fun bareUrl(i: Int): Int? {
        val match = BareUrl.find(s.substring(i, minOf(s.length, i + MaxUrlLength))) ?: return null
        val url = match.value.trimEnd('.', ',', ':', ';', '!', '?', '"', '\'', ')')
        if (url.length <= "https://".length) return null
        add(MarkdownInline.Link(listOf(MarkdownInline.Text(url)), url))
        return i + url.length
    }

    /** `[` 에서 시작하는 `[글](주소 "제목")` 의 (글, 주소, 끝 다음 위치). */
    private fun link(open: Int): Triple<String, String, Int>? {
        var depth = 0
        var k = open
        var close = -1
        while (k < s.length) {
            when (s[k]) {
                '\\' -> k++
                '`' -> {
                    var run = 0
                    while (k + run < s.length && s[k + run] == '`') run++
                    val end = closingBackticks(k + run, run)
                    if (end != null) k = end + run - 1 else k += run - 1
                }
                '[' -> depth++
                ']' -> {
                    depth--
                    if (depth == 0) {
                        close = k
                        break
                    }
                }
            }
            k++
        }
        if (close < 0 || close + 1 >= s.length || s[close + 1] != '(') return null

        var parens = 0
        var end = -1
        k = close + 2
        while (k < s.length) {
            when (s[k]) {
                '\\' -> k++
                '(' -> parens++
                ')' -> if (parens == 0) {
                    end = k
                    break
                } else {
                    parens--
                }
            }
            k++
        }
        if (end < 0) return null

        val inside = s.substring(close + 2, end).trim()
        val destination = if (inside.startsWith("<")) inside.substringAfter('<').substringBefore('>') else inside.substringBefore(' ')

        return Triple(s.substring(open + 1, close), destination, end + 1)
    }

    /** [delimiter] 로 열고 닫힌 것. 여는 쪽 바로 뒤와 닫는 쪽 바로 앞은 공백이 아니어야 한다. `_` 는 낱말 안에서 열고 닫지 않는다. */
    private fun delimited(i: Int, delimiter: String, build: (List<MarkdownInline>) -> MarkdownInline): Int? {
        val start = i + delimiter.length
        if (start >= s.length || s[start].isWhitespace()) return null
        val underscore = delimiter[0] == '_'
        if (underscore && i > 0 && s[i - 1].isLetterOrDigit()) return null

        val close = closing(start, delimiter) ?: return null
        if (underscore && close + delimiter.length < s.length && s[close + delimiter.length].isLetterOrDigit()) return null

        add(build(parseInlines(s.substring(start, close))))
        return close + delimiter.length
    }

    private fun closing(from: Int, delimiter: String): Int? {
        val c = delimiter[0]
        var k = from
        while (k < s.length) {
            val ch = s[k]
            when {
                ch == '\\' -> k += 2
                ch == '`' -> {
                    var run = 0
                    while (k + run < s.length && s[k + run] == '`') run++
                    val end = closingBackticks(k + run, run)
                    k = if (end != null) end + run else k + run
                }
                ch == c -> {
                    var run = 0
                    while (k + run < s.length && s[k + run] == c) run++
                    val candidate = when {
                        // `*a **b** c*` 의 안쪽 ** 는 한 글자 구분자의 짝이 아니다.
                        delimiter.length == 1 && run == 2 -> null
                        // 같은 글자가 이어진 줄(`***`)에서는 끝쪽을 닫는 구분자로 쓴다.
                        run >= delimiter.length -> k + run - delimiter.length
                        else -> null
                    }
                    if (candidate != null && candidate > from && !s[candidate - 1].isWhitespace()) return candidate
                    k += run
                }
                else -> k++
            }
        }
        return null
    }
}

private fun plainText(inlines: List<MarkdownInline>): String =
    inlines.joinToString("") { inline ->
        when (inline) {
            is MarkdownInline.Text -> inline.text
            is MarkdownInline.Code -> inline.text
            is MarkdownInline.Emphasis -> plainText(inline.children)
            is MarkdownInline.Strong -> plainText(inline.children)
            is MarkdownInline.Strike -> plainText(inline.children)
            is MarkdownInline.Link -> plainText(inline.children)
            is MarkdownInline.Image -> inline.alt
            MarkdownInline.LineBreak -> " "
        }
    }
