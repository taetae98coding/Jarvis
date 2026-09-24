package io.github.taetae98coding.jarvis.data.emulator.automation

import io.github.taetae98coding.jarvis.automation.AutomationException
import kotlinx.coroutines.future.await
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import kotlin.io.encoding.Base64

/**
 * 기기 안 WebDriverAgent 러너의 HTTP 클라이언트(docs/platform/jvm.html#mcp-server). 좌표는 포인트다. 세션은 처음
 * 필요할 때 만들고, 러너가 다시 떠서 세션이 사라졌으면 한 번 새로 만든다.
 */
internal class WebDriverAgent(
    private val baseUrl: String,
) {
    private val client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build()

    @Volatile
    private var sessionId: String? = null

    suspend fun isAlive(): Boolean =
        runCatching { request("GET", "/status", timeoutSeconds = 3).statusCode() == 200 }.getOrDefault(false)

    suspend fun screenshotPng(): ByteArray {
        val value = call("GET", "/screenshot") as? JsonPrimitive ?: throw AutomationException("iOS 화면을 가져오지 못했습니다")

        return Base64.decode(value.content)
    }

    suspend fun windowSize(): Pair<Int, Int> {
        val value = sessionCall("GET", "/window/size") as? JsonObject ?: throw AutomationException("iOS 화면 크기를 가져오지 못했습니다")
        val width = (value["width"] as? JsonPrimitive)?.content?.toDouble()?.toInt()
        val height = (value["height"] as? JsonPrimitive)?.content?.toDouble()?.toInt()
        if (width == null || height == null) throw AutomationException("iOS 화면 크기를 읽지 못했습니다")

        return width to height
    }

    suspend fun tap(x: Int, y: Int) {
        sessionCall("POST", "/wda/tap", buildJsonObject { put("x", x); put("y", y) })
    }

    suspend fun touchAndHold(x: Int, y: Int, seconds: Double) {
        sessionCall("POST", "/wda/touchAndHold", buildJsonObject { put("x", x); put("y", y); put("duration", seconds) })
    }

    // /wda/dragfromtoforduration 의 duration 은 끌기 전에 누르고 있는 시간이라 스크롤이 길게 누르기로 바뀐다.
    // W3C 동작은 움직이는 시간을 준다.
    suspend fun swipe(fromX: Int, fromY: Int, toX: Int, toY: Int, millis: Long) {
        val actions = buildJsonObject {
            putJsonArray("actions") {
                add(
                    buildJsonObject {
                        put("type", "pointer")
                        put("id", "finger")
                        putJsonObject("parameters") { put("pointerType", "touch") }
                        putJsonArray("actions") {
                            add(buildJsonObject { put("type", "pointerMove"); put("duration", 0); put("x", fromX); put("y", fromY) })
                            add(buildJsonObject { put("type", "pointerDown"); put("button", 0) })
                            add(buildJsonObject { put("type", "pointerMove"); put("duration", millis); put("x", toX); put("y", toY) })
                            add(buildJsonObject { put("type", "pointerUp"); put("button", 0) })
                        }
                    },
                )
            }
        }
        sessionCall("POST", "/actions", actions)
    }

    suspend fun type(text: String) {
        sessionCall("POST", "/wda/keys", buildJsonObject { put("value", buildJsonArray { add(JsonPrimitive(text)) }) })
    }

    suspend fun pressButton(name: String) {
        sessionCall("POST", "/wda/pressButton", buildJsonObject { put("name", name) })
    }

    suspend fun home() {
        call("POST", "/wda/homescreen", JsonObject(emptyMap()))
    }

    suspend fun source(): JsonElement = call("GET", "/source?format=json") ?: throw AutomationException("UI 트리를 가져오지 못했습니다")

    suspend fun launch(bundleId: String) {
        sessionCall("POST", "/wda/apps/launch", buildJsonObject { put("bundleId", bundleId) })
    }

    private suspend fun sessionCall(method: String, path: String, body: JsonObject? = null): JsonElement? {
        val first = session()
        val response = request(method, "/session/$first$path", body)
        if (response.statusCode() != 404 || !isInvalidSession(response.body())) return value(response)

        sessionId = null
        return value(request(method, "/session/${session()}$path", body))
    }

    private suspend fun session(): String {
        sessionId?.let { return it }

        val body = buildJsonObject { putJsonObject("capabilities") { putJsonObject("alwaysMatch") {} } }
        val response = Json.parseToJsonElement(request("POST", "/session", body).body()).jsonObject
        val id = (response["sessionId"] as? JsonPrimitive)?.contentOrNull
            ?: ((response["value"] as? JsonObject)?.get("sessionId") as? JsonPrimitive)?.contentOrNull
            ?: throw AutomationException("WebDriverAgent 세션을 만들지 못했습니다")

        return id.also { sessionId = it }
    }

    private suspend fun call(method: String, path: String, body: JsonObject? = null): JsonElement? = value(request(method, path, body))

    private fun value(response: HttpResponse<String>): JsonElement? {
        val json = runCatching { Json.parseToJsonElement(response.body()).jsonObject }.getOrNull()
        val value = json?.get("value")

        if (response.statusCode() !in 200..299) {
            val message = ((value as? JsonObject)?.get("message") as? JsonPrimitive)?.contentOrNull ?: response.body().take(200)
            throw AutomationException("WebDriverAgent 오류(${response.statusCode()}): $message")
        }

        return value
    }

    private suspend fun request(method: String, path: String, body: JsonObject? = null, timeoutSeconds: Long = RequestTimeoutSeconds): HttpResponse<String> {
        val request = HttpRequest.newBuilder(URI("$baseUrl$path"))
            .timeout(Duration.ofSeconds(timeoutSeconds))
            .header("Content-Type", "application/json")
            .method(method, body?.let { HttpRequest.BodyPublishers.ofString(it.toString()) } ?: HttpRequest.BodyPublishers.noBody())
            .build()

        return try {
            client.sendAsync(request, HttpResponse.BodyHandlers.ofString()).await()
        } catch (e: java.io.IOException) {
            throw AutomationException("WebDriverAgent 에 닿지 못했습니다($baseUrl): ${e.message}")
        }
    }

    private fun isInvalidSession(body: String): Boolean = "invalid session id" in body || "Session does not exist" in body

    private companion object {
        // 트리가 큰 화면의 /source 는 수 초 걸린다.
        const val RequestTimeoutSeconds = 30L
    }
}
