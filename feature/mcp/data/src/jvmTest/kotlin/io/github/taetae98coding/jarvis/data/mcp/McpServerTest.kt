package io.github.taetae98coding.jarvis.data.mcp

import io.github.taetae98coding.jarvis.automation.AutomationDevice
import io.github.taetae98coding.jarvis.automation.AutomationImage
import io.github.taetae98coding.jarvis.automation.AutomationPlatform
import io.github.taetae98coding.jarvis.automation.DeviceAutomation
import io.github.taetae98coding.jarvis.automation.DeviceKey
import io.github.taetae98coding.jarvis.domain.mcp.McpToolbox
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.net.ServerSocket
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class McpServerTest {
    private val port = ServerSocket(0).use { it.localPort }
    private val server = startMcpServer(McpToolbox(tabs = null, browser = null, devices = FakeDevices()), port)
    private val client = HttpClient.newHttpClient()

    @AfterTest
    fun stop() = server.close()

    @Test
    fun initializeEchoesAKnownProtocolVersion() {
        val response = post("""{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2025-03-26","capabilities":{},"clientInfo":{"name":"t","version":"1"}}}""")

        assertEquals(200, response.statusCode())
        val result = Json.parseToJsonElement(response.body()).jsonObject["result"]!!.jsonObject
        assertEquals("2025-03-26", result["protocolVersion"]!!.jsonPrimitive.content)
        assertEquals("jarvis", result["serverInfo"]!!.jsonObject["name"]!!.jsonPrimitive.content)
    }

    @Test
    fun notificationsAreAccepted() {
        val response = post("""{"jsonrpc":"2.0","method":"notifications/initialized"}""")

        assertEquals(202, response.statusCode())
        assertEquals("", response.body())
    }

    @Test
    fun listsAndCallsTools() {
        val list = Json.parseToJsonElement(post("""{"jsonrpc":"2.0","id":2,"method":"tools/list"}""").body())
        val names = list.jsonObject["result"]!!.jsonObject["tools"]!!.jsonArray.map { it.jsonObject["name"]!!.jsonPrimitive.content }
        assertTrue("device_list" in names)
        assertTrue(names.none { it.startsWith("browser_") })

        val call = Json.parseToJsonElement(
            post("""{"jsonrpc":"2.0","id":3,"method":"tools/call","params":{"name":"device_list","arguments":{}}}""").body(),
        ).jsonObject["result"]!!.jsonObject
        assertEquals("false", call["isError"]!!.jsonPrimitive.content)
        assertTrue("emulator-5554" in call["content"]!!.jsonArray.single().jsonObject["text"]!!.jsonPrimitive.content)
    }

    @Test
    fun imagesAreBase64() {
        val call = Json.parseToJsonElement(
            post("""{"jsonrpc":"2.0","id":4,"method":"tools/call","params":{"name":"device_screenshot","arguments":{"deviceId":"emulator-5554"}}}""").body(),
        ).jsonObject["result"]!!.jsonObject
        val image = call["content"]!!.jsonArray.first().jsonObject

        assertEquals("image", image["type"]!!.jsonPrimitive.content)
        assertEquals("AQID", image["data"]!!.jsonPrimitive.content)
        assertEquals("image/png", image["mimeType"]!!.jsonPrimitive.content)
    }

    @Test
    fun unknownMethodIsAnError() {
        val body = Json.parseToJsonElement(post("""{"jsonrpc":"2.0","id":5,"method":"resources/list"}""").body()).jsonObject

        assertEquals(-32601, body["error"]!!.jsonObject["code"]!!.jsonPrimitive.content.toInt())
    }

    @Test
    fun getIsNotAllowed() {
        val response = client.send(HttpRequest.newBuilder(uri()).GET().build(), HttpResponse.BodyHandlers.ofString())

        assertEquals(405, response.statusCode())
    }

    @Test
    fun requestsFromWebPagesAreRejected() {
        val response = post("""{"jsonrpc":"2.0","id":6,"method":"ping"}""", origin = "https://evil.test")

        assertEquals(403, response.statusCode())
    }

    @Test
    fun secondServerOnTheSamePortIsANoOp() {
        startMcpServer(McpToolbox(null, null, null), port).close()

        assertEquals(200, post("""{"jsonrpc":"2.0","id":7,"method":"ping"}""").statusCode())
    }

    private fun uri(): URI = URI("http://127.0.0.1:$port/mcp")

    private fun post(body: String, origin: String? = null): HttpResponse<String> {
        val request = HttpRequest.newBuilder(uri())
            .header("Content-Type", "application/json")
            .header("Accept", "application/json, text/event-stream")
            .header("X-Jarvis-Session", "session-1")
            .apply { origin?.let { header("Origin", it) } }
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build()

        return client.send(request, HttpResponse.BodyHandlers.ofString())
    }

    private class FakeDevices : DeviceAutomation {
        override suspend fun devices(): List<AutomationDevice> =
            listOf(AutomationDevice("emulator-5554", "Pixel", AutomationPlatform.ANDROID, isPhysical = false, isRunning = true, canControl = true))

        override suspend fun boot(deviceId: String) = Unit

        override suspend fun screenshot(deviceId: String): AutomationImage = AutomationImage(byteArrayOf(1, 2, 3), "image/png", 1, 1)

        override suspend fun tap(deviceId: String, x: Int, y: Int, durationMs: Long) = Unit

        override suspend fun swipe(deviceId: String, fromX: Int, fromY: Int, toX: Int, toY: Int, durationMs: Long) = Unit

        override suspend fun type(deviceId: String, text: String) = Unit

        override suspend fun press(deviceId: String, key: DeviceKey) = Unit

        override suspend fun uiTree(deviceId: String): String = ""

        override suspend fun launchApp(deviceId: String, appId: String) = Unit
    }
}
