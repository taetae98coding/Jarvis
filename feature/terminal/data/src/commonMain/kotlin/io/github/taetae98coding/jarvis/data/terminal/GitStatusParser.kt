package io.github.taetae98coding.jarvis.data.terminal

import io.github.taetae98coding.jarvis.domain.terminal.GitChange
import io.github.taetae98coding.jarvis.domain.terminal.GitChangeKind
import io.github.taetae98coding.jarvis.domain.terminal.GitCommit
import io.github.taetae98coding.jarvis.domain.terminal.GitGraphLine
import io.github.taetae98coding.jarvis.domain.terminal.GitStatus

/**
 * `git status --porcelain=v1 -z -b` 의 출력. 항목은 NUL 로 끝나고 `XY <경로>` 이며, X 나 Y 가 R·C 면 다음 필드가 옛 경로다.
 * 첫 필드 `## …` 는 브랜치다. 충돌(양쪽이 U 이거나 AA·DD)은 작업 트리 쪽에만 둔다 — 해결해 stage 하는 것이 할 일이다.
 */
internal fun parseGitStatus(root: String, output: String): GitStatus {
    val fields = output.split('\u0000')
    val staged = mutableListOf<GitChange>()
    val unstaged = mutableListOf<GitChange>()
    var branch: String? = null

    var index = 0
    while (index < fields.size) {
        val field = fields[index++]
        if (field.startsWith(BranchHeader)) {
            branch = parseBranch(field.removePrefix(BranchHeader))
            continue
        }
        if (field.length < 4) continue

        val x = field[0]
        val y = field[1]
        val path = field.substring(3)
        val original = if (x in RenameCodes || y in RenameCodes) fields.getOrNull(index++) else null

        when {
            x == '!' -> Unit
            x == '?' -> unstaged += GitChange(path, GitChangeKind.Untracked)
            "$x$y" in ConflictCodes -> unstaged += GitChange(path, GitChangeKind.Conflicted)
            else -> {
                changeKind(x)?.let { staged += GitChange(path, it, original.takeIf { x in RenameCodes }) }
                changeKind(y)?.let { unstaged += GitChange(path, it, original.takeIf { y in RenameCodes }) }
            }
        }
    }

    return GitStatus(root = root, branch = branch, staged = staged.sortedBy { it.path }, unstaged = unstaged.sortedBy { it.path })
}

// `main...origin/main [ahead 1]`, `No commits yet on main`(옛 git 은 `Initial commit on main`), `HEAD (no branch)`.
private fun parseBranch(header: String): String? {
    val name = UnbornPrefixes.firstOrNull { header.startsWith(it) }?.let { header.removePrefix(it) }
        ?: header.substringBefore("...").substringBefore(' ')

    return name.takeIf { header != DetachedHeader && it.isNotEmpty() }
}

private fun changeKind(code: Char): GitChangeKind? = GitChangeKind.entries.firstOrNull { it.symbol == code && code != '?' }

private const val BranchHeader = "## "
private const val DetachedHeader = "HEAD (no branch)"
private val UnbornPrefixes = listOf("No commits yet on ", "Initial commit on ")
private val RenameCodes = setOf('R', 'C')
private val ConflictCodes = setOf("DD", "AU", "UD", "UA", "DU", "AA", "UU")

/**
 * [parseGitGraph] 가 읽는 `git log --format`. 필드를 %x1f 로 가르므로 선 그림과 제목의 어떤 글자와도 섞이지 않는다.
 * 끝의 `%n%x1e` 는 커밋마다 둘째 줄을 만든다. 그 줄의 이음선은 git 이 그리므로 그래프가 끊기지 않는다.
 */
internal const val GitGraphFormat = "%x1f%H%x1f%h%x1f%D%x1f%an%x1f%ad%x1f%s%n%x1e"

/** `git log --graph --format=`[GitGraphFormat] 의 출력. %x1f 도 %x1e 도 없는 줄은 선만 있는 줄이다. */
internal fun parseGitGraph(output: String): List<GitGraphLine> {
    val lines = mutableListOf<GitGraphLine>()
    var last: GitCommit? = null

    // isNotBlank 로 거르면 안 된다. %x1e 는 Char.isWhitespace 가 공백으로 보는 제어 문자라 둘째 줄이 사라진다.
    output.lineSequence().filter { it.isNotEmpty() }.forEach { line ->
        val detail = line.indexOf(DetailMarker)
        if (detail >= 0) {
            lines += GitGraphLine(graph = line.substring(0, detail).trimEnd(), commit = last, isDetail = last != null)
            return@forEach
        }

        val start = line.indexOf(FieldSeparator)
        val fields = if (start < 0) emptyList() else line.substring(start + 1).split(FieldSeparator, limit = 6)
        if (fields.size < 6) {
            lines += GitGraphLine(graph = (if (start < 0) line else line.substring(0, start)).trimEnd())
            return@forEach
        }

        val commit = GitCommit(
            hash = fields[0],
            shortHash = fields[1],
            refs = fields[2].split(", ").filter { it.isNotBlank() },
            author = fields[3],
            date = fields[4],
            subject = fields[5],
        )
        last = commit
        lines += GitGraphLine(graph = line.substring(0, start).trimEnd(), commit = commit)
    }

    return lines
}

private const val FieldSeparator = '\u001f'
private const val DetailMarker = '\u001e'
