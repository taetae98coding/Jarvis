package io.github.taetae98coding.jarvis.data.terminal

import io.github.taetae98coding.jarvis.domain.terminal.CodeAnalysisStatus
import io.github.taetae98coding.jarvis.domain.terminal.CodeCompletion
import io.github.taetae98coding.jarvis.domain.terminal.CodeCompletionKind
import io.github.taetae98coding.jarvis.domain.terminal.CodeEdit
import io.github.taetae98coding.jarvis.domain.terminal.CodeLanguage
import io.github.taetae98coding.jarvis.domain.terminal.CodeLocation
import io.github.taetae98coding.jarvis.domain.terminal.CodeTextEdit
import io.github.taetae98coding.jarvis.domain.terminal.completionPrefix
import io.github.taetae98coding.jarvis.domain.terminal.cursorAfterEdits
import io.github.taetae98coding.jarvis.domain.terminal.applyTextEdits
import io.github.taetae98coding.jarvis.domain.terminal.lineStarts
import io.github.taetae98coding.jarvis.domain.terminal.lineTextOf
import io.github.taetae98coding.jarvis.domain.terminal.offsetOf
import io.github.taetae98coding.jarvis.domain.terminal.positionOf
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.net.URI
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * 언어 서버 하나와의 대화(docs/platform/jvm.html#terminal-code-navigation). 문서는 처음 요청할 때 열고, 요청마다 글이 바뀌었으면
 * 먼저 전체 글로 `didChange` 를 보낸다. [readyOnInitialize] 가 아니면(Kotlin) 서버가 임포트를 마쳤다고 알릴 때 준비된다.
 */
internal class LspSession(
    private val language: CodeLanguage,
    private val root: String,
    input: InputStream,
    output: OutputStream,
    private val readyOnInitialize: Boolean,
    private val requestTimeout: Duration = RequestTimeout,
) {
    private val connection = LspConnection(input, output, ::onServerRequest, ::onServerNotification)
    private val _status = MutableStateFlow<CodeAnalysisStatus>(CodeAnalysisStatus.Starting(null))
    val status: Flow<CodeAnalysisStatus> = _status.asStateFlow()

    val closed get() = connection.closed

    private val documents = mutableMapOf<String, OpenDocument>()
    private val documentLock = Mutex()
    private val progressTitles = mutableMapOf<String, String>()

    // workspace/executeCommand 가 도는 동안 서버가 요청으로 보내는 편집과 커서 자리(Kotlin 자동완성, C4).
    private val captureLock = Any()
    private var capturedEdits: MutableList<Pair<String, CodeTextEdit>>? = null
    private var capturedCursor: Pair<String, Pair<Int, Int>>? = null
    private var capturedTexts: Map<String, String> = emptyMap()

    @Volatile
    private var shuttingDown = false

    val isReady: Boolean get() = _status.value == CodeAnalysisStatus.Ready

    suspend fun start(): Boolean {
        connection.start()
        val initialized = runCatching {
            connection.request("initialize", initializeParams(), InitializeTimeout)
        }.getOrNull()
        if (initialized == null) {
            _status.value = CodeAnalysisStatus.Failed("분석 서버가 초기화에 답하지 않았습니다")
            return false
        }
        connection.notify("initialized", buildJsonObject {})
        if (readyOnInitialize) _status.value = CodeAnalysisStatus.Ready

        return true
    }

    /** 서버 출력이 끝나면 부른다(N5). */
    fun onClosed() {
        if (!shuttingDown && _status.value !is CodeAnalysisStatus.Failed) _status.value = CodeAnalysisStatus.Failed("분석 서버가 멈췄습니다")
    }

    /** 서버가 stderr 에 남긴 이유. [onClosed] 의 문구보다 낫다. */
    fun fail(reason: String) {
        _status.value = CodeAnalysisStatus.Failed(reason)
    }

    suspend fun shutdown() {
        shuttingDown = true
        runCatching { connection.request("shutdown", null, ShutdownTimeout) }
        runCatching { connection.notify("exit", null) }
    }

    fun close(path: String) {
        val removed = synchronized(documents) { documents.remove(path) } ?: return
        runCatching { connection.notify("textDocument/didClose", buildJsonObject { put("textDocument", identifier(removed.uri)) }) }
    }

    suspend fun complete(path: String, text: String, offset: Int): List<CodeCompletion>? {
        val uri = sync(path, text) ?: return null
        val result = request("textDocument/completion", positionParams(uri, text, offset) {
            putJsonObject("context") { put("triggerKind", 1) }
        }) ?: return null
        val items = when (result) {
            is JsonArray -> result
            is JsonObject -> result["items"] as? JsonArray ?: JsonArray(emptyList())
            else -> return emptyList()
        }

        return items.mapNotNull { (it as? JsonObject)?.toCompletion() }
    }

    suspend fun applyCompletion(path: String, text: String, offset: Int, item: CodeCompletion): CodeEdit? {
        val raw = item.data?.let { runCatching { Json.parseToJsonElement(it).jsonObject }.getOrNull() } ?: return null
        val uri = sync(path, text) ?: return null
        val command = raw["command"] as? JsonObject

        if (command != null) return executeCompletionCommand(uri, text, command)

        val edit = raw["textEdit"] as? JsonObject
        val newText = edit?.get("newText")?.jsonPrimitive?.contentOrNull
        val starts = lineStarts(text)
        val main = if (edit != null && !newText.isNullOrEmpty()) {
            val range = (edit["range"] ?: edit["replace"] ?: edit["insert"])?.jsonObject ?: return null
            range.toTextEdit(text, starts, newText) ?: return null
        } else {
            val prefix = completionPrefix(text, offset, language)
            val insert = raw["insertText"]?.jsonPrimitive?.contentOrNull ?: item.label
            CodeTextEdit(offset - prefix.length, offset, insert)
        }
        val others = (raw["additionalTextEdits"] as? JsonArray).orEmpty().mapNotNull { (it as? JsonObject)?.toTextEdit(text, starts) }

        return CodeEdit(applyTextEdits(text, others + main), cursorAfterEdits(main, others))
    }

    suspend fun definition(path: String, text: String, offset: Int): AnalysisLocations? =
        locations("textDocument/definition", path, text, offset) {}

    suspend fun usages(path: String, text: String, offset: Int): AnalysisLocations? =
        locations("textDocument/references", path, text, offset) {
            putJsonObject("context") { put("includeDeclaration", false) }
        }

    private suspend fun locations(
        method: String,
        path: String,
        text: String,
        offset: Int,
        extra: JsonObjectBuilder.() -> Unit,
    ): AnalysisLocations? {
        val uri = sync(path, text) ?: return null
        val result = request(method, positionParams(uri, text, offset, extra)) ?: return null
        val entries = when (result) {
            is JsonArray -> result.mapNotNull { it as? JsonObject }
            is JsonObject -> listOf(result)
            else -> emptyList()
        }
        val texts = mutableMapOf(path to text)
        var skipped = 0
        val found = entries.mapNotNull { entry ->
            // Location 이거나 LocationLink 다.
            val target = entry["uri"] ?: entry["targetUri"]
            val range = (entry["targetSelectionRange"] ?: entry["range"])?.jsonObject
            val filePath = target?.jsonPrimitive?.contentOrNull?.let(::pathOf)
            if (filePath == null || range == null) {
                skipped++
                return@mapNotNull null
            }
            val start = range["start"] as? JsonObject ?: return@mapNotNull null
            val fileText = texts.getOrPut(filePath) { runCatching { File(filePath).readText() }.getOrDefault("") }
            val line = start.int("line")
            CodeLocation(filePath, line, start.int("character"), lineTextOf(fileText, line))
        }

        return AnalysisLocations(found.distinct(), skipped)
    }

    private suspend fun executeCompletionCommand(uri: String, text: String, command: JsonObject): CodeEdit? {
        synchronized(captureLock) {
            capturedEdits = mutableListOf()
            capturedCursor = null
            capturedTexts = mapOf(uri to text)
        }
        // 서버는 답하기 전에 workspace/applyEdit·window/showDocument 를 요청으로 보낸다. 읽기 스레드가 차례로 처리하므로 답이 오면 다 모였다.
        val answered = request("workspace/executeCommand", buildJsonObject {
            put("command", command["command"] ?: JsonPrimitive(""))
            command["arguments"]?.let { put("arguments", it) }
        }) != null
        val (edits, cursor) = synchronized(captureLock) {
            val result = capturedEdits.orEmpty().filter { samePath(it.first, uri) }.map { it.second } to capturedCursor
            capturedEdits = null
            capturedCursor = null
            result
        }
        if (!answered || edits.isEmpty()) return null

        val newText = applyTextEdits(text, edits)
        val cursorOffset = cursor?.takeIf { samePath(it.first, uri) }?.second?.let { (line, column) -> offsetOf(newText, line, column) }
            ?: edits.maxBy { it.start }.let { main -> cursorAfterEdits(main, edits - main) }

        return CodeEdit(newText, cursorOffset)
    }

    // 요청하기 전에 서버 문서를 [text] 로 맞춘다. 준비되지 않았으면 null.
    private suspend fun sync(path: String, text: String): String? {
        if (!isReady) return null

        return documentLock.withLock {
            val current = synchronized(documents) { documents[path] }
            val result = runCatching {
                if (current == null) {
                    val opened = OpenDocument(uriOf(path), version = 1, text = text)
                    connection.notify("textDocument/didOpen", buildJsonObject {
                        putJsonObject("textDocument") {
                            put("uri", opened.uri)
                            put("languageId", language.name.lowercase())
                            put("version", opened.version)
                            put("text", text)
                        }
                    })
                    synchronized(documents) { documents[path] = opened }
                    opened.uri
                } else {
                    if (current.text != text) {
                        val changed = current.copy(version = current.version + 1, text = text)
                        connection.notify("textDocument/didChange", buildJsonObject {
                            putJsonObject("textDocument") {
                                put("uri", changed.uri)
                                put("version", changed.version)
                            }
                            putJsonArray("contentChanges") { add(buildJsonObject { put("text", text) }) }
                        })
                        synchronized(documents) { documents[path] = changed }
                    }
                    current.uri
                }
            }
            result.getOrNull()
        }
    }

    private suspend fun request(method: String, params: JsonElement): JsonElement? =
        runCatching { connection.request(method, params, requestTimeout) }.getOrNull()

    private fun positionParams(uri: String, text: String, offset: Int, extra: JsonObjectBuilder.() -> Unit): JsonObject {
        val position = positionOf(text, offset)

        return buildJsonObject {
            put("textDocument", identifier(uri))
            putJsonObject("position") {
                put("line", position.line)
                put("character", position.column)
            }
            extra()
        }
    }

    private fun identifier(uri: String) = buildJsonObject { put("uri", uri) }

    private fun initializeParams(): JsonObject {
        val rootUri = uriOf(root)

        return buildJsonObject {
            put("processId", ProcessHandle.current().pid())
            put("rootUri", rootUri)
            putJsonArray("workspaceFolders") {
                add(buildJsonObject {
                    put("uri", rootUri)
                    put("name", File(root).name)
                })
            }
            putJsonObject("capabilities") {
                putJsonObject("workspace") {
                    put("applyEdit", true)
                    put("workspaceFolders", true)
                }
                putJsonObject("window") {
                    put("workDoneProgress", true)
                    putJsonObject("showDocument") { put("support", true) }
                }
                putJsonObject("textDocument") {
                    putJsonObject("synchronization") { put("didSave", false) }
                    putJsonObject("completion") {
                        putJsonObject("completionItem") {
                            put("snippetSupport", false)
                            put("labelDetailsSupport", true)
                        }
                    }
                    putJsonObject("definition") { put("linkSupport", true) }
                    putJsonObject("references") {}
                }
            }
        }
    }

    private fun onServerRequest(method: String, params: JsonElement?): JsonElement =
        when (method) {
            "workspace/applyEdit" -> {
                captureEdit(params?.jsonObject?.get("edit") as? JsonObject)
                buildJsonObject { put("applied", true) }
            }
            "window/showDocument" -> {
                val body = params?.jsonObject
                val start = (body?.get("selection") as? JsonObject)?.get("start")?.jsonObject
                val uri = body?.get("uri")?.jsonPrimitive?.contentOrNull
                if (uri != null && start != null) {
                    synchronized(captureLock) {
                        if (capturedEdits != null) {
                            capturedCursor = uri to ((start["line"]?.jsonPrimitive?.intOrNull ?: 0) to (start["character"]?.jsonPrimitive?.intOrNull ?: 0))
                        }
                    }
                }
                buildJsonObject { put("success", true) }
            }
            "workspace/configuration" -> buildJsonArray {
                repeat((params?.jsonObject?.get("items") as? JsonArray)?.size ?: 0) { add(JsonNull) }
            }
            "workspace/workspaceFolders" -> buildJsonArray {
                add(buildJsonObject {
                    put("uri", uriOf(root))
                    put("name", File(root).name)
                })
            }
            else -> JsonNull
        }

    private fun captureEdit(edit: JsonObject?) {
        edit ?: return
        synchronized(captureLock) {
            val target = capturedEdits ?: return
            val perUri = mutableListOf<Pair<String, JsonArray>>()
            (edit["changes"] as? JsonObject)?.forEach { (uri, edits) -> (edits as? JsonArray)?.let { perUri += uri to it } }
            (edit["documentChanges"] as? JsonArray)?.forEach { change ->
                val body = change as? JsonObject ?: return@forEach
                val uri = (body["textDocument"] as? JsonObject)?.get("uri")?.jsonPrimitive?.contentOrNull ?: return@forEach
                (body["edits"] as? JsonArray)?.let { perUri += uri to it }
            }
            perUri.forEach { (uri, edits) ->
                val text = capturedTexts.entries.firstOrNull { samePath(it.key, uri) }?.value ?: return@forEach
                val starts = lineStarts(text)
                edits.forEach { (it as? JsonObject)?.toTextEdit(text, starts)?.let { edit -> target += uri to edit } }
            }
        }
    }

    private fun onServerNotification(method: String, params: JsonElement?) {
        val body = params as? JsonObject
        when (method) {
            "\$/progress" -> {
                val token = body?.get("token")?.jsonPrimitive?.contentOrNull ?: return
                val value = body["value"] as? JsonObject ?: return
                when (value["kind"]?.jsonPrimitive?.contentOrNull) {
                    "begin" -> synchronized(progressTitles) { progressTitles[token] = value["title"]?.jsonPrimitive?.contentOrNull.orEmpty() }
                    "report" -> {
                        val percent = value["percentage"]?.jsonPrimitive?.intOrNull
                        if (_status.value is CodeAnalysisStatus.Starting && percent != null) _status.value = CodeAnalysisStatus.Starting(percent)
                    }
                    "end" -> {
                        val title = synchronized(progressTitles) { progressTitles.remove(token) }
                        if (title == ImportingTitle) markReady()
                    }
                }
            }
            // kotlin-lsp 263.4702 이 임포트를 마치면 보낸다. 실패(Gradle 동기화 실패)여도 파일 안의 심볼은 알므로 준비로 본다.
            "intellij/workspaceImportState" ->
                if (body?.get("phase")?.jsonPrimitive?.contentOrNull == "FINISHED") markReady()
        }
    }

    private fun markReady() {
        if (_status.value is CodeAnalysisStatus.Starting) _status.value = CodeAnalysisStatus.Ready
    }

    private fun JsonObject.toCompletion(): CodeCompletion? {
        val label = this["label"]?.jsonPrimitive?.contentOrNull ?: return null
        val labelDetails = this["labelDetails"] as? JsonObject

        return CodeCompletion(
            label = label,
            kind = completionKind(this["kind"]?.jsonPrimitive?.intOrNull),
            signature = labelDetails?.get("detail")?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() },
            detail = (labelDetails?.get("description") ?: this["detail"])?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() },
            sortText = this["sortText"]?.jsonPrimitive?.contentOrNull ?: label,
            filterText = this["filterText"]?.jsonPrimitive?.contentOrNull ?: label,
            data = toString(),
        )
    }

    private data class OpenDocument(val uri: String, val version: Int, val text: String)

    private companion object {
        val RequestTimeout = 10.seconds

        val InitializeTimeout = 60.seconds

        val ShutdownTimeout = 2.seconds

        const val ImportingTitle = "Importing"
    }
}

// LSP CompletionItemKind(3.17).
private fun completionKind(kind: Int?): CodeCompletionKind =
    when (kind) {
        2, 3, 4 -> CodeCompletionKind.Function
        5, 6, 10, 12, 20, 21 -> CodeCompletionKind.Variable
        7, 8, 9, 13, 22, 25 -> CodeCompletionKind.Type
        14 -> CodeCompletionKind.Keyword
        else -> CodeCompletionKind.Other
    }

private fun JsonObject.toTextEdit(text: String, starts: IntArray, newText: String? = null): CodeTextEdit? {
    val range = (this["range"] as? JsonObject) ?: this
    val start = range["start"] as? JsonObject ?: return null
    val end = range["end"] as? JsonObject ?: return null

    return CodeTextEdit(
        start = offsetOf(text, start.int("line"), start.int("character"), starts),
        end = offsetOf(text, end.int("line"), end.int("character"), starts),
        text = newText ?: this["newText"]?.jsonPrimitive?.contentOrNull ?: return null,
    )
}

private fun JsonObject.int(key: String): Int = this[key]?.jsonPrimitive?.intOrNull ?: 0

internal fun uriOf(path: String): String = URI("file", "", path, null).toASCIIString()

// file 주소만 경로로 푼다. jar: 같은 라이브러리 안 주소는 null.
internal fun pathOf(uri: String): String? =
    runCatching { URI(uri) }.getOrNull()?.takeIf { it.scheme == "file" }?.path

private fun samePath(a: String, b: String): Boolean = pathOf(a) == pathOf(b)
