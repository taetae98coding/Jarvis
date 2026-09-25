package io.github.taetae98coding.jarvis.domain.terminal

/** 파일 탭이 문법 색을 칠하는 언어(docs/common/terminal-file-editor.html H1). */
enum class SyntaxLanguage {
    Kotlin,
    Swift,
    Java,
    JavaScript,
    C,
    Go,
    Rust,
    Python,
    Shell,
    Json,
    Yaml,
    Toml,
    Xml,
    Markdown,
}

enum class SyntaxKind {
    Keyword,
    Type,
    Function,
    String,
    Number,
    Comment,
    Annotation,
    Constant,
    Property,
    Tag,
    Heading,
    Emphasis,
    Link,
    Code,
}

/** [start] 부터 [end] 앞까지. 위치는 글자(UTF-16) 단위다. */
data class SyntaxToken(val start: Int, val end: Int, val kind: SyntaxKind)

fun syntaxLanguageOf(path: String): SyntaxLanguage? {
    val name = path.substringAfterLast('/')
    FileNameLanguages[name]?.let { return it }

    return ExtensionLanguages[name.substringAfterLast('.', missingDelimiterValue = "").lowercase()]
}

/** 마크다운 울타리 코드 블록의 언어 이름(```kotlin)으로 고른다. */
fun syntaxLanguageOfName(name: String): SyntaxLanguage? {
    val key = name.trim().lowercase()
    if (key.isEmpty()) return null

    return LanguageAliases[key] ?: ExtensionLanguages[key]
}

/** [text] 전체의 토큰. 앞에서부터 겹치지 않게 늘어서 있다. */
fun highlightSyntax(text: String, language: SyntaxLanguage): List<SyntaxToken> {
    val out = mutableListOf<SyntaxToken>()
    when (language) {
        SyntaxLanguage.Kotlin -> CLikeTokenizer(text, KotlinRules, out).run()
        SyntaxLanguage.Swift -> CLikeTokenizer(text, SwiftRules, out).run()
        SyntaxLanguage.Java -> CLikeTokenizer(text, JavaRules, out).run()
        SyntaxLanguage.JavaScript -> CLikeTokenizer(text, JavaScriptRules, out).run()
        SyntaxLanguage.C -> CLikeTokenizer(text, CRules, out).run()
        SyntaxLanguage.Go -> CLikeTokenizer(text, GoRules, out).run()
        SyntaxLanguage.Rust -> CLikeTokenizer(text, RustRules, out).run()
        SyntaxLanguage.Python -> CLikeTokenizer(text, PythonRules, out).run()
        SyntaxLanguage.Shell -> tokenizeShell(text, out)
        SyntaxLanguage.Json -> tokenizeJson(text, out)
        SyntaxLanguage.Yaml -> tokenizeYaml(text, out)
        SyntaxLanguage.Toml -> tokenizeToml(text, out)
        SyntaxLanguage.Xml -> tokenizeXml(text, out)
        SyntaxLanguage.Markdown -> tokenizeMarkdown(text, out)
    }
    out.sortBy { it.start }

    return out
}

/**
 * [text] 를 [String.lines] 와 같은 자리(CRLF·LF·CR)에서 나눈 줄마다, 그 줄 안 위치로 옮긴 토큰. 줄을 넘는 토큰(블록 주석,
 * 여러 줄 문자열)은 줄마다 잘라 넣는다(H2).
 */
fun highlightLines(text: String, language: SyntaxLanguage): List<List<SyntaxToken>> {
    val tokens = highlightSyntax(text, language)
    val result = mutableListOf<List<SyntaxToken>>()
    var index = 0
    var lineStart = 0

    fun addLine(lineEnd: Int) {
        while (index < tokens.size && tokens[index].end <= lineStart) index++
        val line = mutableListOf<SyntaxToken>()
        var k = index
        while (k < tokens.size && tokens[k].start < lineEnd) {
            val token = tokens[k]
            val start = maxOf(token.start, lineStart)
            val end = minOf(token.end, lineEnd)
            if (start < end) line += SyntaxToken(start - lineStart, end - lineStart, token.kind)
            k++
        }
        result += line
    }

    var i = 0
    while (i < text.length) {
        val c = text[i]
        if (c == '\n' || c == '\r') {
            addLine(i)
            i += if (c == '\r' && i + 1 < text.length && text[i + 1] == '\n') 2 else 1
            lineStart = i
        } else {
            i++
        }
    }
    addLine(text.length)

    return result
}

private val ExtensionLanguages: Map<String, SyntaxLanguage> = buildMap {
    listOf("kt", "kts").forEach { put(it, SyntaxLanguage.Kotlin) }
    put("swift", SyntaxLanguage.Swift)
    listOf("java", "gradle", "groovy").forEach { put(it, SyntaxLanguage.Java) }
    listOf("js", "jsx", "mjs", "cjs", "ts", "tsx").forEach { put(it, SyntaxLanguage.JavaScript) }
    listOf("c", "h", "cc", "cpp", "hpp", "m", "mm").forEach { put(it, SyntaxLanguage.C) }
    put("go", SyntaxLanguage.Go)
    put("rs", SyntaxLanguage.Rust)
    put("py", SyntaxLanguage.Python)
    listOf("sh", "bash", "zsh").forEach { put(it, SyntaxLanguage.Shell) }
    put("json", SyntaxLanguage.Json)
    listOf("yml", "yaml").forEach { put(it, SyntaxLanguage.Yaml) }
    listOf("toml", "properties", "ini").forEach { put(it, SyntaxLanguage.Toml) }
    listOf("xml", "html", "htm", "svg", "plist").forEach { put(it, SyntaxLanguage.Xml) }
    listOf("md", "markdown").forEach { put(it, SyntaxLanguage.Markdown) }
}

private val FileNameLanguages: Map<String, SyntaxLanguage> =
    listOf(".zshrc", ".bashrc", ".bash_profile", ".zprofile", ".profile", "gradlew").associateWith { SyntaxLanguage.Shell }

private val LanguageAliases: Map<String, SyntaxLanguage> = mapOf(
    "kotlin" to SyntaxLanguage.Kotlin,
    "java" to SyntaxLanguage.Java,
    "javascript" to SyntaxLanguage.JavaScript,
    "typescript" to SyntaxLanguage.JavaScript,
    "c++" to SyntaxLanguage.C,
    "objc" to SyntaxLanguage.C,
    "objective-c" to SyntaxLanguage.C,
    "golang" to SyntaxLanguage.Go,
    "rust" to SyntaxLanguage.Rust,
    "python" to SyntaxLanguage.Python,
    "shell" to SyntaxLanguage.Shell,
    "console" to SyntaxLanguage.Shell,
    "html" to SyntaxLanguage.Xml,
)

private class CLikeRules(
    val keywords: Set<String>,
    val constants: Set<String>,
    val lineComment: String? = "//",
    val hashLineComment: Boolean = false,
    val blockComment: Boolean = true,
    val tripleQuotes: Boolean = false,
    val tripleSingleQuotes: Boolean = false,
    val singleQuoteStrings: Boolean = true,
    // Rust 는 ' 가 문자 리터럴이기도 하고 수명('a)이기도 하다. 닫는 ' 가 바로 오지 않으면 수명으로 본다.
    val singleQuoteCharsOnly: Boolean = false,
    val backtickStrings: Boolean = false,
    val atAnnotations: Boolean = true,
    val hashDirectives: Boolean = false,
    val hashAttributes: Boolean = false,
    val bangMacros: Boolean = false,
    val dollarInIdentifiers: Boolean = false,
)

private val KotlinRules = CLikeRules(
    keywords = setOf(
        "as", "break", "class", "continue", "do", "else", "for", "fun", "if", "in", "interface", "is", "object", "package", "return",
        "super", "this", "throw", "try", "typealias", "typeof", "val", "var", "when", "while", "by", "catch", "constructor", "finally",
        "import", "init", "where", "abstract", "actual", "annotation", "companion", "const", "crossinline", "data", "enum", "expect",
        "external", "final", "infix", "inline", "inner", "internal", "lateinit", "noinline", "open", "operator", "out", "override",
        "private", "protected", "public", "reified", "sealed", "suspend", "tailrec", "vararg",
    ),
    constants = setOf("true", "false", "null"),
    tripleQuotes = true,
)

private val SwiftRules = CLikeRules(
    keywords = setOf(
        "associatedtype", "class", "deinit", "enum", "extension", "fileprivate", "func", "import", "init", "inout", "internal", "let",
        "open", "operator", "private", "precedencegroup", "protocol", "public", "rethrows", "static", "struct", "subscript", "typealias",
        "var", "break", "case", "catch", "continue", "default", "defer", "do", "else", "fallthrough", "for", "guard", "if", "in",
        "repeat", "return", "throw", "switch", "where", "while", "as", "await", "async", "is", "super", "self", "Self", "throws", "try",
        "some", "any", "convenience", "dynamic", "final", "lazy", "mutating", "nonmutating", "optional", "override", "required",
        "unowned", "weak", "willSet", "didSet", "indirect", "actor", "nonisolated", "isolated", "consuming", "borrowing",
    ),
    constants = setOf("true", "false", "nil"),
    tripleQuotes = true,
    singleQuoteStrings = false,
    hashAttributes = true,
)

private val JavaRules = CLikeRules(
    keywords = setOf(
        "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char", "class", "const", "continue", "default", "do",
        "double", "else", "enum", "extends", "final", "finally", "float", "for", "goto", "if", "implements", "import", "instanceof",
        "int", "interface", "long", "native", "new", "package", "private", "protected", "public", "return", "short", "static",
        "strictfp", "super", "switch", "synchronized", "this", "throw", "throws", "transient", "try", "void", "volatile", "while",
        "var", "record", "sealed", "permits", "yield", "def", "in", "as", "trait",
    ),
    constants = setOf("true", "false", "null"),
    tripleQuotes = true,
    tripleSingleQuotes = true,
)

private val JavaScriptRules = CLikeRules(
    keywords = setOf(
        "break", "case", "catch", "class", "const", "continue", "debugger", "default", "delete", "do", "else", "export", "extends",
        "finally", "for", "function", "if", "import", "in", "instanceof", "let", "new", "return", "super", "switch", "this", "throw",
        "try", "typeof", "var", "void", "while", "with", "yield", "async", "await", "of", "static", "from", "as", "type", "interface",
        "enum", "implements", "private", "protected", "public", "readonly", "declare", "namespace", "abstract", "keyof", "infer",
        "satisfies", "never", "unknown", "any", "number", "string", "boolean", "symbol", "bigint",
    ),
    constants = setOf("true", "false", "null", "undefined", "NaN", "Infinity"),
    backtickStrings = true,
    dollarInIdentifiers = true,
)

private val CRules = CLikeRules(
    keywords = setOf(
        "auto", "break", "case", "char", "const", "continue", "default", "do", "double", "else", "enum", "extern", "float", "for",
        "goto", "if", "inline", "int", "long", "register", "restrict", "return", "short", "signed", "sizeof", "static", "struct",
        "switch", "typedef", "union", "unsigned", "void", "volatile", "while", "bool", "class", "namespace", "template", "typename",
        "public", "private", "protected", "virtual", "override", "final", "new", "delete", "this", "try", "catch", "throw", "using",
        "constexpr", "noexcept", "explicit", "friend", "operator", "mutable", "static_cast", "dynamic_cast", "reinterpret_cast",
        "const_cast", "decltype", "self", "super", "id", "instancetype",
    ),
    constants = setOf("true", "false", "NULL", "nullptr", "nil", "YES", "NO"),
    hashDirectives = true,
)

private val GoRules = CLikeRules(
    keywords = setOf(
        "break", "case", "chan", "const", "continue", "default", "defer", "else", "fallthrough", "for", "func", "go", "goto", "if",
        "import", "interface", "map", "package", "range", "return", "select", "struct", "switch", "type", "var", "bool", "byte",
        "complex64", "complex128", "error", "float32", "float64", "int", "int8", "int16", "int32", "int64", "rune", "string", "uint",
        "uint8", "uint16", "uint32", "uint64", "uintptr", "any",
    ),
    constants = setOf("true", "false", "nil", "iota"),
    backtickStrings = true,
    atAnnotations = false,
)

private val RustRules = CLikeRules(
    keywords = setOf(
        "as", "async", "await", "break", "const", "continue", "crate", "dyn", "else", "enum", "extern", "fn", "for", "if", "impl", "in",
        "let", "loop", "match", "mod", "move", "mut", "pub", "ref", "return", "self", "Self", "static", "struct", "super", "trait",
        "type", "unsafe", "use", "where", "while", "yield", "i8", "i16", "i32", "i64", "i128", "isize", "u8", "u16", "u32", "u64",
        "u128", "usize", "f32", "f64", "bool", "char", "str",
    ),
    constants = setOf("true", "false"),
    singleQuoteCharsOnly = true,
    atAnnotations = false,
    hashAttributes = true,
    bangMacros = true,
)

private val PythonRules = CLikeRules(
    keywords = setOf(
        "and", "as", "assert", "async", "await", "break", "class", "continue", "def", "del", "elif", "else", "except", "finally", "for",
        "from", "global", "if", "import", "in", "is", "lambda", "nonlocal", "not", "or", "pass", "raise", "return", "try", "while",
        "with", "yield", "match", "case", "self",
    ),
    constants = setOf("True", "False", "None"),
    lineComment = null,
    hashLineComment = true,
    blockComment = false,
    tripleQuotes = true,
    tripleSingleQuotes = true,
)

private class CLikeTokenizer(
    private val text: String,
    private val rules: CLikeRules,
    private val out: MutableList<SyntaxToken>,
) {
    private val n = text.length
    private var i = 0

    fun run() {
        var atLineStart = true
        while (i < n) {
            val c = text[i]
            if (c == '\n') {
                atLineStart = true
                i++
                continue
            }
            if (c.isWhitespace()) {
                i++
                continue
            }

            val lineStart = atLineStart
            atLineStart = false
            when {
                rules.hashDirectives && c == '#' && lineStart -> emit(lineEnd(i), SyntaxKind.Annotation)
                rules.lineComment != null && text.startsWith(rules.lineComment, i) -> emit(lineEnd(i), SyntaxKind.Comment)
                rules.hashLineComment && c == '#' -> emit(lineEnd(i), SyntaxKind.Comment)
                rules.blockComment && text.startsWith("/*", i) -> emit(closeAfter("*/", i + 2), SyntaxKind.Comment)
                rules.tripleQuotes && text.startsWith("\"\"\"", i) -> emit(closeAfter("\"\"\"", i + 3), SyntaxKind.String)
                rules.tripleSingleQuotes && text.startsWith("'''", i) -> emit(closeAfter("'''", i + 3), SyntaxKind.String)
                c == '"' -> emit(quoted(i, '"', multiLine = false), SyntaxKind.String)
                c == '\'' && rules.singleQuoteCharsOnly -> charOrLifetime()
                c == '\'' && rules.singleQuoteStrings -> emit(quoted(i, '\'', multiLine = false), SyntaxKind.String)
                c == '`' && rules.backtickStrings -> emit(quoted(i, '`', multiLine = true), SyntaxKind.String)
                c == '@' && rules.atAnnotations && i + 1 < n && isIdentifierStart(text[i + 1]) ->
                    emit(identifierEnd(i + 1, allowDots = true), SyntaxKind.Annotation)
                c == '#' && rules.hashAttributes -> hashAttribute()
                c.isDigit() || (c == '.' && i + 1 < n && text[i + 1].isDigit()) -> emit(numberEnd(i), SyntaxKind.Number)
                isIdentifierStart(c) -> identifier()
                else -> i++
            }
        }
    }

    private fun emit(end: Int, kind: SyntaxKind) {
        out += SyntaxToken(i, end, kind)
        i = end
    }

    private fun lineEnd(from: Int): Int {
        val end = text.indexOf('\n', from)
        return if (end < 0) n else end
    }

    private fun closeAfter(close: String, from: Int): Int {
        val end = text.indexOf(close, from)
        return if (end < 0) n else end + close.length
    }

    // 닫히지 않은 한 줄 문자열은 줄 끝에서 끊는다. 따옴표 하나 빠진 것이 파일 끝까지 번지지 않게.
    private fun quoted(start: Int, quote: Char, multiLine: Boolean): Int {
        var k = start + 1
        while (k < n) {
            val ch = text[k]
            when {
                ch == '\\' -> k += 2
                ch == quote -> return k + 1
                ch == '\n' && !multiLine -> return k
                else -> k++
            }
        }
        return n
    }

    private fun charOrLifetime() {
        val close = when {
            i + 2 < n && text[i + 1] == '\\' -> text.indexOf('\'', i + 2).takeIf { it in 0..(i + 10) }
            i + 2 < n && text[i + 2] == '\'' -> i + 2
            else -> null
        }
        if (close != null) {
            emit(close + 1, SyntaxKind.String)
        } else if (i + 1 < n && isIdentifierStart(text[i + 1])) {
            emit(identifierEnd(i + 1, allowDots = false), SyntaxKind.Annotation)
        } else {
            i++
        }
    }

    private fun hashAttribute() {
        when {
            i + 1 < n && text[i + 1] == '[' || text.startsWith("#![", i) -> {
                val close = text.indexOf(']', i)
                val end = lineEnd(i)
                emit(if (close in 0 until end) close + 1 else end, SyntaxKind.Annotation)
            }
            i + 1 < n && isIdentifierStart(text[i + 1]) -> emit(identifierEnd(i + 1, allowDots = false), SyntaxKind.Annotation)
            else -> i++
        }
    }

    private fun numberEnd(start: Int): Int {
        var k = start
        if (text.startsWith("0x", k) || text.startsWith("0X", k) || text.startsWith("0b", k) || text.startsWith("0B", k)) {
            k += 2
            while (k < n && (text[k].isLetterOrDigit() || text[k] == '_')) k++
            return k
        }
        while (k < n && (text[k].isDigit() || text[k] == '_')) k++
        if (k + 1 < n && text[k] == '.' && text[k + 1].isDigit()) {
            k++
            while (k < n && (text[k].isDigit() || text[k] == '_')) k++
        }
        if (k < n && (text[k] == 'e' || text[k] == 'E')) {
            var e = k + 1
            if (e < n && (text[e] == '+' || text[e] == '-')) e++
            if (e < n && text[e].isDigit()) {
                k = e
                while (k < n && text[k].isDigit()) k++
            }
        }
        while (k < n && text[k].isLetter()) k++
        return k
    }

    private fun identifier() {
        val end = identifierEnd(i, allowDots = false)
        val word = text.substring(i, end)
        val kind = when {
            word in rules.keywords -> SyntaxKind.Keyword
            word in rules.constants -> SyntaxKind.Constant
            rules.bangMacros && end < n && text[end] == '!' -> {
                out += SyntaxToken(i, end + 1, SyntaxKind.Function)
                i = end + 1
                return
            }
            nextNonSpace(end) == '(' -> SyntaxKind.Function
            word[0].isUpperCase() -> SyntaxKind.Type
            else -> null
        }
        if (kind != null) out += SyntaxToken(i, end, kind)
        i = end
    }

    private fun nextNonSpace(from: Int): Char? {
        var k = from
        while (k < n && (text[k] == ' ' || text[k] == '\t')) k++
        return text.getOrNull(k)
    }

    private fun isIdentifierStart(c: Char): Boolean = c.isLetter() || c == '_' || (rules.dollarInIdentifiers && c == '$')

    private fun identifierEnd(start: Int, allowDots: Boolean): Int {
        var k = start
        while (k < n) {
            val ch = text[k]
            val part = ch.isLetterOrDigit() || ch == '_' || (rules.dollarInIdentifiers && ch == '$')
            if (part || (allowDots && ch == '.' && k + 1 < n && isIdentifierStart(text[k + 1]))) k++ else break
        }
        return k
    }
}

private val ShellKeywords = setOf(
    "if", "then", "else", "elif", "fi", "for", "while", "until", "do", "done", "case", "esac", "function", "in", "return", "local",
    "export", "readonly", "select", "time", "source", "alias", "unset", "set", "exit", "shift", "declare", "eval", "exec",
)

private fun tokenizeShell(text: String, out: MutableList<SyntaxToken>) {
    val n = text.length
    var i = 0
    while (i < n) {
        val c = text[i]
        val wordStart = i == 0 || text[i - 1].isWhitespace() || text[i - 1] == ';' || text[i - 1] == '(' || text[i - 1] == '|'
        when {
            c == '#' && (i == 0 || text[i - 1].isWhitespace()) -> {
                val end = text.indexOf('\n', i).let { if (it < 0) n else it }
                out += SyntaxToken(i, end, SyntaxKind.Comment)
                i = end
            }
            c == '\'' -> {
                val end = text.indexOf('\'', i + 1).let { if (it < 0) n else it + 1 }
                out += SyntaxToken(i, end, SyntaxKind.String)
                i = end
            }
            c == '"' -> {
                var k = i + 1
                while (k < n && text[k] != '"') k += if (text[k] == '\\') 2 else 1
                val end = minOf(k + 1, n)
                out += SyntaxToken(i, end, SyntaxKind.String)
                i = end
            }
            c == '$' && i + 1 < n && text[i + 1] == '{' -> {
                val end = text.indexOf('}', i).let { if (it < 0) n else it + 1 }
                out += SyntaxToken(i, end, SyntaxKind.Annotation)
                i = end
            }
            c == '$' && i + 1 < n && (text[i + 1].isLetterOrDigit() || text[i + 1] == '_' || text[i + 1] in "@#?*!$-") -> {
                var k = i + 1
                if (text[k].isLetter() || text[k] == '_') {
                    while (k < n && (text[k].isLetterOrDigit() || text[k] == '_')) k++
                } else {
                    k++
                }
                out += SyntaxToken(i, k, SyntaxKind.Annotation)
                i = k
            }
            wordStart && (c.isLetter() || c == '_') -> {
                var k = i
                while (k < n && (text[k].isLetterOrDigit() || text[k] == '_' || text[k] == '-')) k++
                if (text.substring(i, k) in ShellKeywords && (k == n || !text[k].isLetterOrDigit())) out += SyntaxToken(i, k, SyntaxKind.Keyword)
                i = k
            }
            else -> i++
        }
    }
}

private fun tokenizeJson(text: String, out: MutableList<SyntaxToken>) {
    val n = text.length
    var i = 0
    while (i < n) {
        val c = text[i]
        when {
            c == '"' -> {
                var k = i + 1
                while (k < n && text[k] != '"' && text[k] != '\n') k += if (text[k] == '\\') 2 else 1
                val end = minOf(k + 1, n)
                var after = end
                while (after < n && text[after].isWhitespace()) after++
                out += SyntaxToken(i, end, if (after < n && text[after] == ':') SyntaxKind.Property else SyntaxKind.String)
                i = end
            }
            text.startsWith("//", i) -> {
                val end = text.indexOf('\n', i).let { if (it < 0) n else it }
                out += SyntaxToken(i, end, SyntaxKind.Comment)
                i = end
            }
            c == '-' || c.isDigit() -> {
                var k = i + 1
                while (k < n && (text[k].isDigit() || text[k] in ".eE+-")) k++
                if (k > i + 1 || c.isDigit()) out += SyntaxToken(i, k, SyntaxKind.Number)
                i = k
            }
            c.isLetter() -> {
                var k = i
                while (k < n && text[k].isLetter()) k++
                if (text.substring(i, k) in setOf("true", "false", "null")) out += SyntaxToken(i, k, SyntaxKind.Constant)
                i = k
            }
            else -> i++
        }
    }
}

private val YamlConstants = setOf("true", "false", "null", "yes", "no", "on", "off", "~", "True", "False", "Null", "TRUE", "FALSE")

private val YamlKey = Regex("""^(\s*(?:-\s+)*)("[^"]*"|'[^']*'|[^\s#'"\-][^:#]*?|-[^\s:#][^:#]*?)\s*:(?=\s|$)""")

private fun tokenizeYaml(text: String, out: MutableList<SyntaxToken>) {
    forEachLine(text) { start, line ->
        val trimmed = line.trimStart()
        val indent = line.length - trimmed.length
        when {
            trimmed.startsWith("#") -> out += SyntaxToken(start + indent, start + line.length, SyntaxKind.Comment)
            trimmed == "---" || trimmed == "..." -> out += SyntaxToken(start + indent, start + line.length, SyntaxKind.Keyword)
            else -> {
                var valueStart = 0
                YamlKey.find(line)?.let { match ->
                    val key = match.groups[2]!!
                    out += SyntaxToken(start + key.range.first, start + key.range.last + 1, SyntaxKind.Property)
                    valueStart = match.range.last + 1
                } ?: run {
                    val dash = Regex("""^\s*(?:-\s+)*""").find(line)
                    valueStart = dash?.range?.last?.plus(1) ?: 0
                }
                scalarValue(line, valueStart, start, out)
            }
        }
    }
}

private fun tokenizeToml(text: String, out: MutableList<SyntaxToken>) {
    forEachLine(text) { start, line ->
        val trimmed = line.trimStart()
        val indent = line.length - trimmed.length
        when {
            trimmed.startsWith("#") || trimmed.startsWith(";") || trimmed.startsWith("!") ->
                out += SyntaxToken(start + indent, start + line.length, SyntaxKind.Comment)
            trimmed.startsWith("[") -> {
                val close = line.lastIndexOf(']')
                out += SyntaxToken(start + indent, start + if (close > indent) close + 1 else line.length, SyntaxKind.Type)
            }
            else -> {
                val separator = line.indexOfFirst { it == '=' || it == ':' }
                if (separator > indent) {
                    val keyEnd = line.substring(0, separator).trimEnd().length
                    out += SyntaxToken(start + indent, start + keyEnd, SyntaxKind.Property)
                    scalarValue(line, separator + 1, start, out)
                }
            }
        }
    }
}

// 줄의 [from] 부터 값: 따옴표 문자열, 공백 뒤 # 주석, 값 전체가 숫자·상수면 그 색.
private fun scalarValue(line: String, from: Int, offset: Int, out: MutableList<SyntaxToken>) {
    var i = from
    var plainStart = -1
    var plainEnd = -1
    while (i < line.length) {
        val c = line[i]
        when {
            c == '"' || c == '\'' -> {
                var k = i + 1
                while (k < line.length && line[k] != c) k += if (c == '"' && line[k] == '\\') 2 else 1
                val end = minOf(k + 1, line.length)
                out += SyntaxToken(offset + i, offset + end, SyntaxKind.String)
                i = end
            }
            c == '#' && (i == 0 || line[i - 1].isWhitespace()) -> {
                out += SyntaxToken(offset + i, offset + line.length, SyntaxKind.Comment)
                break
            }
            (c == '&' || c == '*' || c == '!') && (i == from || line[i - 1].isWhitespace()) && i + 1 < line.length && !line[i + 1].isWhitespace() -> {
                var k = i + 1
                while (k < line.length && !line[k].isWhitespace()) k++
                out += SyntaxToken(offset + i, offset + k, SyntaxKind.Annotation)
                i = k
            }
            c.isWhitespace() -> i++
            else -> {
                if (plainStart < 0) plainStart = i
                var k = i
                while (k < line.length && !line[k].isWhitespace() && line[k] != '#') k++
                plainEnd = k
                i = k
            }
        }
    }
    if (plainStart < 0) return

    val value = line.substring(plainStart, plainEnd)
    val kind = when {
        value in YamlConstants -> SyntaxKind.Constant
        value.toDoubleOrNull() != null || value.startsWith("0x") && value.drop(2).toLongOrNull(16) != null -> SyntaxKind.Number
        else -> null
    }
    // 값 가운데 낱말 하나만 숫자인 것(`v1 2`)은 칠하지 않는다.
    if (kind != null && line.substring(plainStart).substringBefore(" #").trimEnd() == value) {
        out += SyntaxToken(offset + plainStart, offset + plainEnd, kind)
    }
}

private fun tokenizeXml(text: String, out: MutableList<SyntaxToken>) {
    val n = text.length
    var i = 0

    fun closeAfter(close: String, from: Int): Int = text.indexOf(close, from).let { if (it < 0) n else it + close.length }

    while (i < n) {
        val c = text[i]
        when {
            text.startsWith("<!--", i) -> closeAfter("-->", i + 4).also { out += SyntaxToken(i, it, SyntaxKind.Comment); i = it }
            text.startsWith("<![CDATA[", i) -> closeAfter("]]>", i + 9).also { out += SyntaxToken(i, it, SyntaxKind.String); i = it }
            text.startsWith("<?", i) -> closeAfter("?>", i + 2).also { out += SyntaxToken(i, it, SyntaxKind.Annotation); i = it }
            text.startsWith("<!", i) -> closeAfter(">", i + 2).also { out += SyntaxToken(i, it, SyntaxKind.Annotation); i = it }
            c == '<' && i + 1 < n && (text[i + 1].isLetter() || text[i + 1] == '/') -> {
                var k = if (text[i + 1] == '/') i + 2 else i + 1
                val nameStart = k
                while (k < n && (text[k].isLetterOrDigit() || text[k] in ":-_.")) k++
                out += SyntaxToken(nameStart, k, SyntaxKind.Tag)
                while (k < n && text[k] != '>') {
                    val ch = text[k]
                    when {
                        ch == '"' || ch == '\'' -> {
                            val end = text.indexOf(ch, k + 1).let { if (it < 0) n else it + 1 }
                            out += SyntaxToken(k, end, SyntaxKind.String)
                            k = end
                        }
                        ch.isLetter() || ch == '_' || ch == ':' || ch == '@' -> {
                            val attributeStart = k
                            while (k < n && (text[k].isLetterOrDigit() || text[k] in ":-_.@")) k++
                            out += SyntaxToken(attributeStart, k, SyntaxKind.Property)
                        }
                        else -> k++
                    }
                }
                i = minOf(k + 1, n)
            }
            c == '&' -> {
                val end = text.indexOf(';', i)
                if (end in (i + 2)..(i + 10) && text.substring(i + 1, end).all { it.isLetterOrDigit() || it == '#' }) {
                    out += SyntaxToken(i, end + 1, SyntaxKind.Constant)
                    i = end + 1
                } else {
                    i++
                }
            }
            else -> i++
        }
    }
}

private val MarkdownHeading = Regex("""^ {0,3}#{1,6}(\s|$)""")
private val MarkdownListMarker = Regex("""^\s*([-*+]|\d{1,9}[.)])\s""")
private val MarkdownRule = Regex("""^ {0,3}([-*_])(\s*\1){2,}\s*$""")
private val MarkdownFence = Regex("""^ {0,3}(`{3,}|~{3,})""")
private val MarkdownInlineCode = Regex("""`+[^`\n]*?`+""")
private val MarkdownLink = Regex("""!?\[[^\]\n]*]\([^)\n]*\)|<https?://[^>\s]+>""")
private val MarkdownStrong = Regex("""(\*\*|__)(?=\S)[^\n]*?\S\1""")
private val MarkdownEmphasis = Regex("""(?<![*\w])([*_])(?=\S)[^*_\n]*?\S\1(?![*\w])""")

private fun tokenizeMarkdown(text: String, out: MutableList<SyntaxToken>) {
    var fence: String? = null
    forEachLine(text) { start, line ->
        val lineEnd = start + line.length
        val fenceMatch = MarkdownFence.find(line)
        when {
            fence != null -> {
                out += SyntaxToken(start, lineEnd, SyntaxKind.Code)
                if (fenceMatch != null && fenceMatch.groupValues[1].startsWith(fence!!) && line.trim().all { it == fence!![0] }) fence = null
            }
            fenceMatch != null -> {
                fence = fenceMatch.groupValues[1]
                out += SyntaxToken(start, lineEnd, SyntaxKind.Code)
            }
            MarkdownHeading.containsMatchIn(line) -> out += SyntaxToken(start, lineEnd, SyntaxKind.Heading)
            line.trimStart().startsWith(">") -> out += SyntaxToken(start, lineEnd, SyntaxKind.Comment)
            MarkdownRule.matches(line) -> out += SyntaxToken(start, lineEnd, SyntaxKind.Keyword)
            else -> {
                val taken = mutableListOf<IntRange>()
                MarkdownListMarker.find(line)?.let { match ->
                    val marker = match.groups[1]!!.range
                    out += SyntaxToken(start + marker.first, start + marker.last + 1, SyntaxKind.Keyword)
                }
                fun mark(regex: Regex, kind: SyntaxKind) {
                    regex.findAll(line).forEach { match ->
                        if (taken.none { it.first <= match.range.last && match.range.first <= it.last }) {
                            taken += match.range
                            out += SyntaxToken(start + match.range.first, start + match.range.last + 1, kind)
                        }
                    }
                }
                mark(MarkdownInlineCode, SyntaxKind.Code)
                mark(MarkdownLink, SyntaxKind.Link)
                mark(MarkdownStrong, SyntaxKind.Emphasis)
                mark(MarkdownEmphasis, SyntaxKind.Emphasis)
            }
        }
    }
}

/** [String.lines] 와 같은 자리에서 나눈 줄마다 (줄 첫 글자의 위치, 줄). */
private inline fun forEachLine(text: String, action: (start: Int, line: String) -> Unit) {
    var lineStart = 0
    var i = 0
    while (i < text.length) {
        val c = text[i]
        if (c == '\n' || c == '\r') {
            action(lineStart, text.substring(lineStart, i))
            i += if (c == '\r' && i + 1 < text.length && text[i + 1] == '\n') 2 else 1
            lineStart = i
        } else {
            i++
        }
    }
    action(lineStart, text.substring(lineStart))
}
