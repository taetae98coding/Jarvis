package io.github.taetae98coding.jarvis.data.terminal

import io.github.taetae98coding.jarvis.domain.terminal.CodeAnalysisStatus
import io.github.taetae98coding.jarvis.domain.terminal.CodeCompletionKind
import io.github.taetae98coding.jarvis.domain.terminal.CodeEdit
import io.github.taetae98coding.jarvis.domain.terminal.CodeLanguage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.BufferedInputStream
import java.io.File
import java.io.OutputStream
import java.io.PipedInputStream
import java.io.PipedOutputStream
import java.nio.file.Files
import java.util.Collections
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class LspCodeAnalysisTest {
    private val path = "/r/Main.kt"

    @Test
    fun kotlinSessionIsReadyAfterImportAndReportsProgress() = runBlocking<Unit> {
        val server = FakeServer()
        val session = server.session(readyOnInitialize = false)
        assertTrue(session.start())

        assertEquals(CodeAnalysisStatus.Starting(null), session.status.first())
        server.notify("\$/progress", """{"token":"t","value":{"kind":"report","percentage":42}}""")
        session.status.first { it == CodeAnalysisStatus.Starting(42) }
        assertNull(session.complete(path, "gre", 3))

        server.notify("intellij/workspaceImportState", """{"phase":"FINISHED"}""")
        session.status.first { it == CodeAnalysisStatus.Ready }
    }

    @Test
    fun documentOpensOnceThenChangesBeforeRequests() = runBlocking<Unit> {
        val server = FakeServer()
        server.respond("textDocument/completion") {
            """{"isIncomplete":true,"items":[{"label":"greet","kind":3,"labelDetails":{"detail":"(name: String)","description":"String"},"sortText":"1"}]}"""
        }
        val session = server.session(readyOnInitialize = true).also { it.start() }

        session.complete(path, "gre", 3)
        val items = session.complete(path, "val a = gre", 11)!!

        assertEquals(listOf("initialize", "initialized", "textDocument/didOpen", "textDocument/completion", "textDocument/didChange", "textDocument/completion"), server.methods())
        assertEquals("val a = gre", server.last("textDocument/didChange")["contentChanges"]!!.jsonArray[0].jsonObject["text"]!!.jsonPrimitive.content)
        assertEquals("greet", items.single().label)
        assertEquals(CodeCompletionKind.Function, items.single().kind)
        assertEquals("(name: String)", items.single().signature)
        assertEquals("String", items.single().detail)
    }

    @Test
    fun kotlinCompletionCommandAppliesServerEditsAndCursor() = runBlocking<Unit> {
        val server = FakeServer()
        val text = "package a\n\nval x = len"
        server.respond("textDocument/completion") {
            """[{"label":"length","kind":10,"textEdit":{"newText":"","insert":{"start":{"line":2,"character":11},"end":{"line":2,"character":11}}},"command":{"title":"Apply","command":"jetbrains.kotlin.completion.apply","arguments":[1]}}]"""
        }
        server.respond("workspace/executeCommand") { writer ->
            writer.request("workspace/applyEdit", """{"edit":{"changes":{"file:///r/Main.kt":[{"range":{"start":{"line":2,"character":11},"end":{"line":2,"character":11}},"newText":"gth"},{"range":{"start":{"line":1,"character":0},"end":{"line":1,"character":0}},"newText":"import b.length\n"}]}}}""")
            writer.request("window/showDocument", """{"uri":"file:///r/Main.kt","selection":{"start":{"line":3,"character":14},"end":{"line":3,"character":14}}}""")
            "true"
        }
        val session = server.session(readyOnInitialize = true).also { it.start() }

        val item = session.complete(path, text, text.length)!!.single()
        val edit = session.applyCompletion(path, text, text.length, item)

        assertEquals(CodeEdit("package a\nimport b.length\n\nval x = length", "package a\nimport b.length\n\nval x = length".length), edit)
    }

    @Test
    fun plainTextEditsAreAppliedWithoutAServerRoundTrip() = runBlocking<Unit> {
        val server = FakeServer()
        val text = "ContentView().ignoresSa"
        server.respond("textDocument/completion") {
            """[{"label":"ignoresSafeArea()","filterText":"ignoresSafeArea()","insertText":"ignoresSafeArea()","kind":2,"textEdit":{"range":{"start":{"line":0,"character":14},"end":{"line":0,"character":23}},"newText":"ignoresSafeArea()"}}]"""
        }
        val session = server.session(readyOnInitialize = true).also { it.start() }

        val item = session.complete("/r/App.swift", text, text.length)!!.single()
        val edit = session.applyCompletion("/r/App.swift", text, text.length, item)

        assertEquals(CodeEdit("ContentView().ignoresSafeArea()", 31), edit)
        assertTrue("workspace/executeCommand" !in server.methods())
    }

    @Test
    fun locationsKeepFilesAndCountLibraryResults() = runBlocking<Unit> {
        val directory = Files.createTempDirectory("jarvis-lsp").toFile().canonicalFile
        val other = File(directory, "Other.kt").apply { writeText("package a\n\nfun greet() = Unit\n") }
        val server = FakeServer()
        server.respond("textDocument/definition") {
            """[{"uri":"${uriOf(other.path)}","range":{"start":{"line":2,"character":4},"end":{"line":2,"character":9}}},{"uri":"jar:///lib.jar!/String.kt","range":{"start":{"line":1,"character":0},"end":{"line":1,"character":1}}}]"""
        }
        server.respond("textDocument/references") { "[]" }
        val session = server.session(readyOnInitialize = true).also { it.start() }

        val found = session.definition(path, "greet()", 1)!!
        session.usages(path, "greet()", 1)

        assertEquals(1, found.skipped)
        assertEquals(listOf(other.path to "fun greet() = Unit"), found.locations.map { it.path to it.lineText })
        assertEquals("false", server.last("textDocument/references")["context"]!!.jsonObject["includeDeclaration"].toString())
    }

    @Test
    fun serverExitFailsTheSession() = runBlocking<Unit> {
        val server = FakeServer()
        val session = server.session(readyOnInitialize = true).also { it.start() }

        server.close()
        withTimeout(5.seconds) { session.closed.await() }
        session.onClosed()

        assertEquals(CodeAnalysisStatus.Failed("분석 서버가 멈췄습니다"), session.status.first())
    }

    @Test
    fun oneServerPerRootIsSharedAndStoppedAfterIdle() = runBlocking<Unit> {
        val launches = AtomicInteger()
        val stops = AtomicInteger()
        val source = LspCodeAnalysisDataSource(
            launcher = { _, _ ->
                launches.incrementAndGet()
                LaunchResult.Running(FakeServer().session(readyOnInitialize = true)) { stops.incrementAndGet() }
            },
            idleTimeout = 100.milliseconds,
        )

        withContext(Dispatchers.Default) {
            val first = source.observe(CodeLanguage.Kotlin, "/r", "/r/A.kt").launchIn(this)
            val second = source.observe(CodeLanguage.Kotlin, "/r", "/r/B.kt").launchIn(this)
            source.observe(CodeLanguage.Kotlin, "/r", "/r/A.kt").first { it == CodeAnalysisStatus.Ready }
            assertEquals(1, launches.get())
            first.cancel()
            second.cancel()
        }
        withTimeout(5.seconds) { while (stops.get() == 0) kotlinx.coroutines.delay(20) }
        assertEquals(1, stops.get())
        assertIs<CodeAnalysisStatus.Unavailable>(
            LspCodeAnalysisDataSource({ _, _ -> LaunchResult.Unavailable("없음") }).observe(CodeLanguage.Swift, "/s", "/s/A.swift").first(),
        )
    }

    /** 미리 적은 답을 주는 언어 서버. 받은 메시지를 모두 적어 둔다. */
    private class FakeServer {
        private val toServer = PipedOutputStream()
        private val serverInput = BufferedInputStream(PipedInputStream(toServer, PipeSize))
        private val toClient = PipedOutputStream()
        private val clientInput = PipedInputStream(toClient, PipeSize)
        private val received = Collections.synchronizedList(mutableListOf<JsonObject>())
        private val handlers = mutableMapOf<String, (Writer) -> String>()
        private val writer = Writer(toClient)

        init {
            respond("initialize") { """{"capabilities":{}}""" }
            thread(isDaemon = true) { loop() }
        }

        fun session(readyOnInitialize: Boolean) = LspSession(CodeLanguage.Kotlin, "/r", clientInput, toServer, readyOnInitialize, requestTimeout = 5.seconds)

        fun respond(method: String, handler: (Writer) -> String) {
            handlers[method] = handler
        }

        fun notify(method: String, params: String) = writer.write("""{"jsonrpc":"2.0","method":"$method","params":$params}""")

        fun methods(): List<String> = synchronized(received) { received.mapNotNull { (it["method"] as? JsonPrimitive)?.content } }

        fun last(method: String): JsonObject =
            synchronized(received) { received.last { (it["method"] as? JsonPrimitive)?.content == method } }["params"]!!.jsonObject

        fun close() = toClient.close()

        private fun loop() {
            runCatching {
                while (true) {
                    var length = 0
                    while (true) {
                        val line = readLine() ?: return
                        if (line.isEmpty()) break
                        if (line.startsWith("Content-Length:")) length = line.substringAfter(':').trim().toInt()
                    }
                    val message = Json.parseToJsonElement(serverInput.readNBytes(length).decodeToString()).jsonObject
                    val method = (message["method"] as? JsonPrimitive)?.content ?: continue
                    received += message
                    val id = message["id"] ?: continue
                    val result = handlers[method]?.invoke(writer) ?: "null"
                    writer.write("""{"jsonrpc":"2.0","id":$id,"result":$result}""")
                }
            }
        }

        private fun readLine(): String? {
            val bytes = StringBuilder()
            while (true) {
                val byte = serverInput.read()
                if (byte == -1) return null
                if (byte == '\n'.code) return bytes.toString().trimEnd('\r')
                bytes.append(byte.toChar())
            }
        }

        class Writer(private val output: OutputStream) {
            private val ids = AtomicInteger(1000)

            fun request(method: String, params: String) = write("""{"jsonrpc":"2.0","id":${ids.getAndIncrement()},"method":"$method","params":$params}""")

            fun write(body: String) {
                val bytes = body.encodeToByteArray()
                synchronized(output) {
                    output.write("Content-Length: ${bytes.size}\r\n\r\n".encodeToByteArray())
                    output.write(bytes)
                    output.flush()
                }
            }
        }

        private companion object {
            const val PipeSize = 1 shl 16
        }
    }
}
