package io.github.taetae98coding.jarvis.data.terminal

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.io.BufferedInputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread
import kotlin.time.Duration

/**
 * stdio 로 주고받는 LSP JSON-RPC(docs/platform/jvm.html#terminal-code-navigation). 메시지는 `Content-Length` 머리와 JSON 본문이다.
 * 읽기는 막히는 IO 라 전용 스레드에서 한다. 서버가 보내는 요청([onRequest])·알림([onNotification])도 그 스레드에서 부르므로 오래 걸리면 안 된다.
 */
internal class LspConnection(
    input: InputStream,
    private val output: OutputStream,
    private val onRequest: (method: String, params: JsonElement?) -> JsonElement,
    private val onNotification: (method: String, params: JsonElement?) -> Unit,
) {
    private val input = BufferedInputStream(input)
    private val nextId = AtomicInteger(1)
    private val pending = ConcurrentHashMap<Int, CompletableDeferred<JsonElement>>()

    /** 서버 출력이 끝나면(서버가 죽으면) 끝난다. */
    val closed = CompletableDeferred<Unit>()

    fun start() {
        thread(isDaemon = true, name = "lsp-reader") { readLoop() }
    }

    /** 결과. [timeout] 을 넘기면 `$/cancelRequest` 를 보내고 null 이다. 서버가 오류로 답하거나 연결이 끊기면 예외다. */
    suspend fun request(method: String, params: JsonElement?, timeout: Duration): JsonElement? {
        val id = nextId.getAndIncrement()
        val response = CompletableDeferred<JsonElement>()
        pending[id] = response
        try {
            send(message(method, params, id))
            val result = withTimeoutOrNull(timeout) { response.await() }
            if (result == null) notify("\$/cancelRequest", buildJsonObject { put("id", id) })
            return result
        } finally {
            pending.remove(id)
        }
    }

    fun notify(method: String, params: JsonElement?) = send(message(method, params, id = null))

    private fun message(method: String, params: JsonElement?, id: Int?): JsonObject =
        buildJsonObject {
            put("jsonrpc", "2.0")
            if (id != null) put("id", id)
            put("method", method)
            if (params != null) put("params", params)
        }

    private fun send(message: JsonObject) {
        val body = message.toString().encodeToByteArray()
        synchronized(output) {
            output.write("Content-Length: ${body.size}\r\n\r\n".encodeToByteArray())
            output.write(body)
            output.flush()
        }
    }

    private fun readLoop() {
        try {
            while (true) {
                val length = readHeaders() ?: break
                val body = input.readNBytes(length)
                if (body.size < length) break
                dispatch(Json.parseToJsonElement(body.decodeToString()).jsonObject)
            }
        } catch (_: IOException) {
        } catch (_: IllegalArgumentException) {
            // 틀이 깨진 출력(서버가 stdout 에 로그를 쓴 것 등)이면 더 읽을 수 없다.
        } finally {
            closed.complete(Unit)
            pending.values.forEach { it.completeExceptionally(IOException("언어 서버 연결이 끊겼습니다")) }
        }
    }

    // 본문 길이. 출력이 끝났으면 null.
    private fun readHeaders(): Int? {
        var length: Int? = null
        while (true) {
            val line = readLine() ?: return null
            if (line.isEmpty()) {
                if (length != null) return length
                continue
            }
            if (line.startsWith("Content-Length:", ignoreCase = true)) length = line.substringAfter(':').trim().toIntOrNull()
        }
    }

    private fun readLine(): String? {
        val bytes = java.io.ByteArrayOutputStream()
        while (true) {
            val byte = input.read()
            if (byte == -1) return null
            if (byte == '\n'.code) return bytes.toString(Charsets.US_ASCII).trimEnd('\r')
            bytes.write(byte)
        }
    }

    private fun dispatch(message: JsonObject) {
        val method = (message["method"] as? JsonPrimitive)?.content
        val id = message["id"]
        when {
            method != null && id != null && id !is JsonNull -> {
                val result = runCatching { onRequest(method, message["params"]) }.getOrDefault(JsonNull)
                runCatching {
                    send(buildJsonObject {
                        put("jsonrpc", "2.0")
                        put("id", id)
                        put("result", result)
                    })
                }
            }
            method != null -> runCatching { onNotification(method, message["params"]) }
            else -> {
                val response = id?.jsonPrimitive?.intOrNull?.let(pending::get) ?: return
                val error = message["error"]
                if (error != null && error !is JsonNull) {
                    response.completeExceptionally(LspException(error.toString()))
                } else {
                    response.complete(message["result"] ?: JsonNull)
                }
            }
        }
    }
}

internal class LspException(message: String) : Exception(message)
