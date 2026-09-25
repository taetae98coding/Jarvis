package io.github.taetae98coding.jarvis.domain.terminal

/** 자동완성·선언·사용처 이동을 하는 언어(docs/common/terminal-code-navigation.html). */
enum class CodeLanguage(val label: String) {
    Kotlin("Kotlin"),
    Swift("Swift"),
}

fun codeLanguageOf(path: String): CodeLanguage? =
    when (path.substringAfterLast('/').substringAfterLast('.', missingDelimiterValue = "").lowercase()) {
        "kt", "kts" -> CodeLanguage.Kotlin
        "swift" -> CodeLanguage.Swift
        else -> null
    }

/** 결과를 코드 분석(언어 서버)이 줬는지, 이름을 글자로 찾아 채웠는지. */
enum class CodeSource {
    Analysis,
    TextSearch,
}

/** 파일 하나의 코드 분석 상태(N4). */
sealed interface CodeAnalysisStatus {
    /** 코드 분석을 쓰지 않는다. [reason] 이 null 이면 플랫폼에 코드 분석이 없어서 알릴 것도 없다. */
    data class Unavailable(val reason: String?) : CodeAnalysisStatus

    /** [percent] 는 서버가 진행률을 주지 않으면 null 이다. */
    data class Starting(val percent: Int?) : CodeAnalysisStatus

    data object Ready : CodeAnalysisStatus

    data class Failed(val reason: String) : CodeAnalysisStatus
}

enum class CodeCompletionKind {
    Function,
    Variable,
    Type,
    Keyword,
    Other,
}

/**
 * 자동완성 항목 하나. [signature] 는 이름 뒤에 붙여 보일 매개변수, [detail] 은 오른쪽의 흐린 설명이다. [data] 는 넣을 때
 * 데이터 소스가 다시 읽는 원래 항목이다(C4).
 */
data class CodeCompletion(
    val label: String,
    val kind: CodeCompletionKind,
    val signature: String? = null,
    val detail: String? = null,
    val sortText: String = label,
    val filterText: String = label,
    val data: String? = null,
)

data class CodeCompletions(val items: List<CodeCompletion>, val source: CodeSource)

/** [start] 부터 [end] 앞까지를 [text] 로 바꾼다. 위치는 글자(UTF-16) 단위다. */
data class CodeTextEdit(val start: Int, val end: Int, val text: String)

/** 자동완성을 넣은 뒤의 글 전체와 커서. */
data class CodeEdit(val text: String, val cursor: Int)

/** [line]·[column] 은 0 부터다. [lineText] 는 그 줄의 글이다. */
data class CodeLocation(val path: String, val line: Int, val column: Int, val lineText: String)

/** [libraryOnly] 는 결과가 모두 파일이 아닌 곳(라이브러리 안)이라 [locations] 가 빈 것이다(G5). */
data class CodeLocations(val locations: List<CodeLocation>, val source: CodeSource, val libraryOnly: Boolean = false)

enum class CodeNavigationKind {
    Declaration,
    Usages,
}

data class CodeNavigation(val kind: CodeNavigationKind, val result: CodeLocations)

data class TextPosition(val line: Int, val column: Int)

fun isIdentifierChar(char: Char, language: CodeLanguage): Boolean =
    char.isLetterOrDigit() || char == '_' || (language == CodeLanguage.Swift && char == '$')

/** [offset] 에 걸친 식별자. 커서가 식별자 끝 바로 뒤에 있어도 그 식별자다. 숫자로 시작하면 식별자가 아니다. */
fun identifierAt(text: String, offset: Int, language: CodeLanguage): IntRange? {
    if (offset !in 0..text.length) return null

    var start = offset
    while (start > 0 && isIdentifierChar(text[start - 1], language)) start--
    var end = offset
    while (end < text.length && isIdentifierChar(text[end], language)) end++
    if (start == end || text[start].isDigit()) return null

    return start until end
}

/** 커서 앞에 이어진 식별자 글자. 자동완성 목록을 거르고(C2) 넣을 때 바꿀 글이다(C4). */
fun completionPrefix(text: String, offset: Int, language: CodeLanguage): String {
    var start = offset.coerceIn(0, text.length)
    while (start > 0 && isIdentifierChar(text[start - 1], language)) start--

    return text.substring(start, offset.coerceIn(0, text.length))
}

/** 줄마다 시작 위치. 줄 나누기는 [String.lines] 처럼 `\r\n`·`\n`·`\r` 이다. */
fun lineStarts(text: String): IntArray {
    val starts = mutableListOf(0)
    var index = 0
    while (index < text.length) {
        when (text[index]) {
            '\r' -> {
                if (index + 1 < text.length && text[index + 1] == '\n') index++
                starts += index + 1
            }
            '\n' -> starts += index + 1
        }
        index++
    }

    return starts.toIntArray()
}

/** 줄·칸을 위치로. 없는 줄은 글 끝, 줄 끝을 넘는 칸은 그 줄 끝이다. */
fun offsetOf(text: String, line: Int, column: Int, starts: IntArray = lineStarts(text)): Int {
    if (line < 0) return 0
    if (line >= starts.size) return text.length

    val start = starts[line]
    val end = if (line + 1 < starts.size) lineContentEnd(text, starts[line + 1]) else text.length

    return (start + column.coerceAtLeast(0)).coerceAtMost(end)
}

fun positionOf(text: String, offset: Int, starts: IntArray = lineStarts(text)): TextPosition {
    val clamped = offset.coerceIn(0, text.length)
    var low = 0
    var high = starts.size - 1
    while (low < high) {
        val mid = (low + high + 1) / 2
        if (starts[mid] <= clamped) low = mid else high = mid - 1
    }

    return TextPosition(line = low, column = clamped - starts[low])
}

/** [line] 번째 줄의 글(줄바꿈 없이). */
fun lineTextOf(text: String, line: Int, starts: IntArray = lineStarts(text)): String {
    if (line !in starts.indices) return ""

    val end = if (line + 1 < starts.size) lineContentEnd(text, starts[line + 1]) else text.length

    return text.substring(starts[line], end)
}

// 다음 줄 시작 앞의 줄바꿈(\n, \r, \r\n)을 뺀 끝.
private fun lineContentEnd(text: String, nextStart: Int): Int {
    var end = nextStart
    if (end > 0 && text[end - 1] == '\n') end--
    if (end > 0 && text[end - 1] == '\r') end--

    return end
}

/** 겹치지 않는 [edits] 를 한 번에 적용한다. 위치는 모두 적용하기 전 글 기준이다. */
fun applyTextEdits(text: String, edits: List<CodeTextEdit>): String {
    val builder = StringBuilder(text)
    edits.sortedWith(compareByDescending<CodeTextEdit> { it.start }.thenByDescending { it.end }).forEach { edit ->
        builder.setRange(edit.start.coerceIn(0, builder.length), edit.end.coerceIn(0, builder.length), edit.text)
    }

    return builder.toString()
}

/** [main] 을 [others] 와 함께 넣은 뒤 [main] 이 넣은 글 끝의 위치. [others] 가운데 [main] 앞에 있는 것만큼 밀린다. */
fun cursorAfterEdits(main: CodeTextEdit, others: List<CodeTextEdit>): Int {
    val shift = others.filter { it.end <= main.start }.sumOf { it.text.length - (it.end - it.start) }

    return main.start + shift + main.text.length
}

/** 자동완성 목록(C2). 접두사로 시작하는 것만(대소문자 무시) [CodeCompletion.sortText] 순으로 [limit] 개. */
fun filterCompletions(items: List<CodeCompletion>, prefix: String, limit: Int = CompletionMaxItems): List<CodeCompletion> =
    items
        .filter { prefix.isEmpty() || it.filterText.startsWith(prefix, ignoreCase = true) || it.label.startsWith(prefix, ignoreCase = true) }
        .sortedWith(compareBy<CodeCompletion> { it.sortText }.thenBy { it.label })
        .take(limit)

const val CompletionMaxItems: Int = 50

const val NavigationMaxResults: Int = 200

/**
 * 텍스트 검색의 자동완성(C5). [text] 에 나온 식별자 가운데 [offset] 앞 접두사로 시작하고 더 긴 것을, 커서에 가까이 나온
 * 순으로. 지금 치고 있는 식별자 자신은 뺀다.
 */
fun wordCompletions(text: String, offset: Int, language: CodeLanguage, limit: Int = CompletionMaxItems): List<CodeCompletion> {
    val prefix = completionPrefix(text, offset, language)
    if (prefix.isEmpty()) return emptyList()

    val typing = identifierAt(text, offset, language)
    val distances = mutableMapOf<String, Int>()
    var index = 0
    while (index < text.length) {
        if (!isIdentifierChar(text[index], language)) {
            index++
            continue
        }
        val start = index
        while (index < text.length && isIdentifierChar(text[index], language)) index++
        if (typing != null && start == typing.first) continue

        val word = text.substring(start, index)
        if (word[0].isDigit() || word.length <= prefix.length || !word.startsWith(prefix, ignoreCase = true)) continue

        val distance = if (start < offset) offset - index else start - offset
        distances[word] = minOf(distances[word] ?: Int.MAX_VALUE, distance)
    }

    return distances.entries
        .sortedWith(compareBy<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
        .take(limit)
        .mapIndexed { order, (word, _) -> CodeCompletion(label = word, kind = CodeCompletionKind.Other, sortText = order.toString().padStart(6, '0')) }
}

/** 텍스트 검색이 보는 파일 하나. */
data class CodeFile(val path: String, val text: String)

/** 텍스트 검색의 선언(G6). 선언 키워드 바로 뒤에 [name] 이 오는 자리. */
fun findDeclarations(files: Sequence<CodeFile>, name: String, language: CodeLanguage, limit: Int = NavigationMaxResults): List<CodeLocation> =
    files.flatMap { file -> declarationOffsets(file.text, name, language).map { locationOf(file, it) } }.take(limit).toList()

/** 텍스트 검색의 사용처(G6). 단어 전체가 [name] 인 자리 가운데 선언이 아닌 것. */
fun findUsages(files: Sequence<CodeFile>, name: String, language: CodeLanguage, limit: Int = NavigationMaxResults): List<CodeLocation> =
    files.flatMap { file ->
        val declarations = declarationOffsets(file.text, name, language).toSet()
        wordOffsets(file.text, name, language).filter { it !in declarations }.map { locationOf(file, it) }
    }.take(limit).toList()

private fun locationOf(file: CodeFile, offset: Int): CodeLocation {
    val starts = lineStarts(file.text)
    val position = positionOf(file.text, offset, starts)

    return CodeLocation(file.path, position.line, position.column, lineTextOf(file.text, position.line, starts))
}

private fun wordOffsets(text: String, name: String, language: CodeLanguage): Sequence<Int> =
    sequence {
        if (name.isEmpty()) return@sequence
        var from = 0
        while (true) {
            val found = text.indexOf(name, from)
            if (found < 0) break
            val before = found == 0 || !isIdentifierChar(text[found - 1], language)
            val afterIndex = found + name.length
            val after = afterIndex >= text.length || !isIdentifierChar(text[afterIndex], language)
            if (before && after) yield(found)
            from = found + name.length
        }
    }

private fun declarationOffsets(text: String, name: String, language: CodeLanguage): List<Int> {
    val keywords = when (language) {
        CodeLanguage.Kotlin -> KotlinDeclarationKeywords
        CodeLanguage.Swift -> SwiftDeclarationKeywords
    }

    return wordOffsets(text, name, language).filter { offset -> keywords.containsMatchIn(declarationHead(text, offset)) }.toList()
}

// 이름 앞의 같은 줄 글. Kotlin 은 `fun <T> Foo.name`, Swift 는 `func name` 처럼 키워드와 이름 사이에 받는 타입·타입 매개변수가 올 수 있다.
private fun declarationHead(text: String, offset: Int): String {
    var start = offset
    while (start > 0 && text[start - 1] != '\n' && text[start - 1] != '\r') start--

    return text.substring(start, offset)
}

private val KotlinDeclarationKeywords =
    Regex("""(?:^|[^\w])(?:fun|val|var|class|interface|object|typealias)\s+(?:<[^>]*>\s*)?(?:[\w.<>?, ]+\.)?$""")

private val SwiftDeclarationKeywords =
    Regex("""(?:^|[^\w$])(?:func|var|let|class|struct|enum|protocol|extension|actor|typealias|case)\s+$""")

/** 프로젝트 루트로 치는 파일 이름(N3). */
private val KotlinSettingsFiles = setOf("settings.gradle.kts", "settings.gradle")

private val KotlinBuildFiles = setOf("build.gradle.kts", "build.gradle")

/**
 * [path] 파일의 프로젝트 루트(N3). [entries] 는 폴더 바로 아래 이름들이고, 읽을 수 없으면 null 이다. 경로는 `/` 로 나눈
 * 절대 경로다.
 */
fun codeProjectRoot(path: String, language: CodeLanguage, entries: (directory: String) -> Set<String>?): String {
    val fileDirectory = path.substringBeforeLast('/', missingDelimiterValue = "").ifEmpty { "/" }
    val ancestors = generateSequence(fileDirectory) { directory ->
        if (directory == "/") null else directory.substringBeforeLast('/', missingDelimiterValue = "").ifEmpty { "/" }
    }.toList()
    val names = ancestors.associateWith { entries(it).orEmpty() }

    fun nearest(predicate: (Set<String>) -> Boolean): String? = ancestors.firstOrNull { predicate(names.getValue(it)) }

    return when (language) {
        CodeLanguage.Kotlin ->
            nearest { it.any(KotlinSettingsFiles::contains) }
                ?: ancestors.lastOrNull { names.getValue(it).any(KotlinBuildFiles::contains) }
        CodeLanguage.Swift ->
            nearest { "Package.swift" in it }
                ?: nearest { names -> names.any { it.endsWith(".xcodeproj") } }
    } ?: nearest { ".git" in it } ?: fileDirectory
}
