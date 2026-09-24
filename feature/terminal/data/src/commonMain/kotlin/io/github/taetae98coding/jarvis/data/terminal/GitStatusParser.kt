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
            branch = parseBranchHeader(field.removePrefix(BranchHeader)).branch
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

/**
 * `git status -b` 의 머리. [upstream] 은 `origin/main` 이고, upstream 이 없거나 원격에서 지워졌으면(`[gone]`) null 이다.
 * [ahead]·[behind] 는 upstream 과 비교한 커밋 수다. [unborn] 은 커밋이 아직 없는 브랜치다.
 */
internal data class GitBranchHeader(
    val branch: String?,
    val upstream: String? = null,
    val ahead: Int = 0,
    val behind: Int = 0,
    val unborn: Boolean = false,
)

/** [parseGitStatus] 와 같은 출력에서 머리만 읽는다. `-b` 없이 받은 출력이면 null 이다. */
internal fun parseGitBranchHeader(output: String): GitBranchHeader? =
    output.split('\u0000').firstOrNull { it.startsWith(BranchHeader) }?.let { parseBranchHeader(it.removePrefix(BranchHeader)) }

// `main...origin/main [ahead 1, behind 2]`, `main...origin/main [gone]`, `No commits yet on main`(옛 git 은 `Initial commit on main`),
// `HEAD (no branch)`. 브랜치 이름에는 공백도 `..` 도 들어갈 수 없어서 첫 공백과 `...` 로 가를 수 있다.
private fun parseBranchHeader(header: String): GitBranchHeader {
    if (header == DetachedHeader) return GitBranchHeader(branch = null)

    val unbornPrefix = UnbornPrefixes.firstOrNull { header.startsWith(it) }
    val names = (unbornPrefix?.let { header.removePrefix(it) } ?: header).substringBefore(' ')
    val tracking = header.substringAfter(' ', missingDelimiterValue = "").takeIf { unbornPrefix == null }.orEmpty()

    return GitBranchHeader(
        branch = names.substringBefore("...").ifEmpty { null },
        upstream = names.substringAfter("...", missingDelimiterValue = "").ifEmpty { null }?.takeIf { tracking != GoneTracking },
        ahead = AheadPattern.find(tracking)?.groupValues?.get(1)?.toInt() ?: 0,
        behind = BehindPattern.find(tracking)?.groupValues?.get(1)?.toInt() ?: 0,
        unborn = unbornPrefix != null,
    )
}

private fun changeKind(code: Char): GitChangeKind? = GitChangeKind.entries.firstOrNull { it.symbol == code && code != '?' }

private const val BranchHeader = "## "
private const val DetachedHeader = "HEAD (no branch)"
private val UnbornPrefixes = listOf("No commits yet on ", "Initial commit on ")
private const val GoneTracking = "[gone]"
private val AheadPattern = Regex("""ahead (\d+)""")
private val BehindPattern = Regex("""behind (\d+)""")
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
