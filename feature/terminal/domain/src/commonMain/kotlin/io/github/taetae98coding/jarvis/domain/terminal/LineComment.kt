package io.github.taetae98coding.jarvis.domain.terminal

/**
 * 파일 탭에서 고른 줄에 남긴 코멘트. [lines] 는 남길 때의 줄이고, 보내는 글의 코드 블록이 된다
 * (docs/common/terminal-line-comment.html C8·C11).
 */
data class LineComment(
    val id: Long,
    val path: String,
    val lines: List<DiffedLine>,
    val body: String,
) {
    /** 카드 제목. "12행", "12–14행", 지운 줄만이면 "HEAD 5행 (지운 줄)". */
    val rangeLabel: String
        get() {
            val current = currentRange()
            if (current != null) return "${current.label("–")}행"

            return "HEAD ${removedRange().label("–")}행 (지운 줄)"
        }

    internal fun currentRange(): IntRange? {
        val numbers = lines.filterIsInstance<DiffedLine.Current>().map { it.number }
        return if (numbers.isEmpty()) null else numbers.min()..numbers.max()
    }

    internal fun removedRange(): IntRange {
        val numbers = lines.filterIsInstance<DiffedLine.Removed>().map { it.oldNumber }
        return numbers.min()..numbers.max()
    }
}

/** 같은 줄을 가리키는지. 지금 줄끼리는 지금 번호, 지운 줄끼리는 HEAD 번호로 본다. 글자는 보지 않는다. */
fun DiffedLine.sameLine(other: DiffedLine): Boolean =
    when (this) {
        is DiffedLine.Current -> other is DiffedLine.Current && other.number == number
        is DiffedLine.Removed -> other is DiffedLine.Removed && other.oldNumber == oldNumber
    }

/**
 * Claude 에 보내는 한 메시지. 코멘트는 파일끼리(처음 남긴 순서) 모으고 파일 안에서는 남긴 순서다. 경로는
 * [directory] 아래면 상대 경로다.
 */
fun lineCommentsPrompt(comments: List<LineComment>, directory: String?): String {
    val ordered = comments.groupBy { it.path }.values.flatten()
    val base = directory?.trimEnd('/')?.takeIf { it.isNotEmpty() }?.let { "$it/" }

    return buildString {
        append("아래 코드 줄에 남긴 코멘트 ${comments.size}개를 반영해 주세요.\n")
        ordered.forEachIndexed { index, comment ->
            val path = if (base != null && comment.path.startsWith(base)) comment.path.removePrefix(base) else comment.path
            val current = comment.currentRange()
            val location = if (current != null) "$path:${current.label("-")}" else "$path (HEAD ${comment.removedRange().label("-")}, 지운 줄)"
            val code = codeLines(comment.lines)
            val fence = "`".repeat(maxOf(3, (longestBacktickRun(code) ?: 0) + 1))

            append('\n')
            append("${index + 1}. $location\n")
            append("$fence\n")
            code.forEach { append(it).append('\n') }
            append("$fence\n")
            append(comment.body.trim()).append('\n')
        }
    }.trimEnd('\n')
}

private fun IntRange.label(separator: String): String = if (first == last) "$first" else "$first$separator$last"

private fun codeLines(lines: List<DiffedLine>): List<String> {
    val numberWidth = lines.maxOf { if (it is DiffedLine.Current) it.number.toString().length else 0 }
    val showMarkers = lines.any { it is DiffedLine.Removed || (it is DiffedLine.Current && it.added) }

    return lines.map { line ->
        val (number, marker, text) = when (line) {
            is DiffedLine.Current -> Triple(line.number.toString(), if (line.added) "+" else " ", line.text)
            is DiffedLine.Removed -> Triple("", "−", line.text)
        }
        val prefix = if (showMarkers) "${number.padStart(numberWidth)} $marker" else number.padStart(numberWidth)
        "$prefix $text".trimEnd()
    }
}

private fun longestBacktickRun(lines: List<String>): Int? =
    lines.flatMap { line -> Regex("`+").findAll(line).map { it.value.length }.toList() }.maxOrNull()
