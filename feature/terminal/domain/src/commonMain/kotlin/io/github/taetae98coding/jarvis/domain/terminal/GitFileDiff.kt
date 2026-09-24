package io.github.taetae98coding.jarvis.domain.terminal

/**
 * `git diff -U0` 의 덩어리 하나. `@@ -[oldStart],[oldCount] +[newStart],[newCount] @@` 이고 줄 번호는 1 부터다.
 * 수가 0 이면 start 는 그 줄 "뒤" 를 가리킨다. [removed] 는 HEAD 에서 지운 줄의 글자다.
 */
data class GitDiffHunk(
    val oldStart: Int,
    val oldCount: Int,
    val newStart: Int,
    val newCount: Int,
    val removed: List<String>,
)

/** HEAD 와 작업 트리의 파일 하나 차이. [hunks] 가 비면 같다. */
data class GitFileDiff(val hunks: List<GitDiffHunk>) {
    val added: Int
        get() = hunks.sumOf { it.newCount }

    val removed: Int
        get() = hunks.sumOf { it.oldCount }
}

/** 파일 탭의 줄 하나. */
sealed interface DiffedLine {
    /** 지금 파일의 [number] 번째 줄. */
    data class Current(val number: Int, val text: String, val added: Boolean = false) : DiffedLine

    /** HEAD 의 [oldNumber] 번째 줄로, 지금 파일에는 없다. */
    data class Removed(val oldNumber: Int, val text: String) : DiffedLine
}

/**
 * 지금 파일의 [lines] 에 [diff] 를 겹친다. 지운 줄은 원래 자리에 끼우고, [lines] 밖을 가리키는 표시는 버린다 — 파일이
 * 잘려 앞부분만 있거나, 내용과 diff 를 읽은 시점이 어긋난 때다(docs/common/terminal-file-diff.html D6·D7).
 */
fun diffedLines(lines: List<String>, diff: GitFileDiff?): List<DiffedLine> {
    if (diff == null || diff.hunks.isEmpty()) return lines.mapIndexed { index, text -> DiffedLine.Current(index + 1, text) }

    val added = mutableSetOf<Int>()
    // 지금 파일의 몇 번째 줄 앞(0 부터, lines.size 는 맨 끝)에 끼울지.
    val removedBefore = mutableMapOf<Int, MutableList<DiffedLine.Removed>>()
    diff.hunks.forEach { hunk ->
        (hunk.newStart until hunk.newStart + hunk.newCount).forEach { added += it }
        if (hunk.removed.isEmpty()) return@forEach

        val position = if (hunk.newCount == 0) hunk.newStart else hunk.newStart - 1
        if (position !in 0..lines.size) return@forEach
        removedBefore.getOrPut(position) { mutableListOf() } += hunk.removed.mapIndexed { index, text ->
            DiffedLine.Removed(hunk.oldStart + index, text)
        }
    }

    return buildList {
        lines.forEachIndexed { index, text ->
            removedBefore[index]?.let { addAll(it) }
            add(DiffedLine.Current(index + 1, text, added = index + 1 in added))
        }
        removedBefore[lines.size]?.let { addAll(it) }
    }
}
