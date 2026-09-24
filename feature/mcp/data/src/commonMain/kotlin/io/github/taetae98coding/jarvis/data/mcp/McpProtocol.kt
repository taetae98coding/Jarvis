package io.github.taetae98coding.jarvis.data.mcp

import io.github.taetae98coding.jarvis.automation.AgentServer
import io.github.taetae98coding.jarvis.domain.mcp.McpParameterType
import io.github.taetae98coding.jarvis.domain.mcp.McpTool
import io.github.taetae98coding.jarvis.domain.mcp.McpToolbox
import io.github.taetae98coding.jarvis.domain.mcp.ToolContent
import io.github.taetae98coding.jarvis.domain.mcp.ToolResult
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import kotlin.io.encoding.Base64

/**
 * JSON-RPC 2.0 한 통을 [McpToolbox] 호출로 바꾼다(docs/common/mcp-server.html#protocol). HTTP 와 떼어 두어
 * 테스트가 소켓 없이 돈다. 응답이 없는 요청(알림·응답)이면 null 이고, 서버는 202 로 답한다.
 */
class McpProtocol(
    private val toolbox: McpToolbox,
) {
    suspend fun handle(body: String, sessionId: String?): String? {
        val message = runCatching { Json.parseToJsonElement(body) }.getOrNull()
            ?: return error(JsonNull, ParseError, "JSON 을 읽지 못했습니다").toString()

        // 2025-03-26 은 배열로 여러 통을 묶어 보낼 수 있었다. 2025-06-18 에서 빠졌지만 받아는 준다.
        if (message is JsonArray) {
            val responses = message.mapNotNull { handleOne(it, sessionId) }
            return if (responses.isEmpty()) null else JsonArray(responses).toString()
        }

        return handleOne(message, sessionId)?.toString()
    }

    private suspend fun handleOne(message: JsonElement, sessionId: String?): JsonObject? {
        val request = message as? JsonObject ?: return error(JsonNull, InvalidRequest, "요청이 객체가 아닙니다")
        val id = request["id"]
        val method = (request["method"] as? JsonPrimitive)?.contentOrNull

        // id 가 없으면 알림이고, method 가 없으면 클라이언트가 보낸 응답이다. 둘 다 답하지 않는다.
        if (id == null || id is JsonNull || method == null) return null

        val params = request["params"] as? JsonObject ?: JsonObject(emptyMap())

        return when (method) {
            "initialize" -> result(id, initialize(params))
            "ping" -> result(id, JsonObject(emptyMap()))
            "tools/list" -> result(id, buildJsonObject { put("tools", JsonArray(toolbox.tools.map(::toolJson))) })
            "tools/call" -> {
                val name = (params["name"] as? JsonPrimitive)?.contentOrNull
                    ?: return error(id, InvalidParams, "name 이 없습니다")
                val arguments = (params["arguments"] as? JsonObject)?.toArguments().orEmpty()
                result(id, resultJson(toolbox.call(sessionId, name, arguments)))
            }
            else -> error(id, MethodNotFound, "모르는 메서드입니다: $method")
        }
    }

    private fun initialize(params: JsonObject): JsonObject {
        val requested = (params["protocolVersion"] as? JsonPrimitive)?.contentOrNull

        return buildJsonObject {
            put("protocolVersion", requested?.takeIf { it in SupportedVersions } ?: LatestVersion)
            putJsonObject("capabilities") { putJsonObject("tools") { put("listChanged", false) } }
            putJsonObject("serverInfo") {
                put("name", AgentServer.Name)
                put("version", "1.0.0")
            }
            put(
                "instructions",
                "Jarvis 앱의 브라우저 탭(Chromium)과 Android·iOS 기기를 조작한다. 먼저 browser_screenshot·device_screenshot 으로 화면을 보고 " +
                    "그 이미지의 좌표로 누른다. 쓴 브라우저·기기는 사용자가 볼 수 있게 이 Claude 의 패널에 탭으로 붙는다.",
            )
        }
    }

    private companion object {
        const val LatestVersion = "2025-06-18"
        val SupportedVersions = setOf("2024-11-05", "2025-03-26", "2025-06-18", "2025-11-25")

        const val ParseError = -32700
        const val InvalidRequest = -32600
        const val MethodNotFound = -32601
        const val InvalidParams = -32602
    }
}

private fun result(id: JsonElement, result: JsonElement): JsonObject =
    buildJsonObject {
        put("jsonrpc", "2.0")
        put("id", id)
        put("result", result)
    }

private fun error(id: JsonElement, code: Int, message: String): JsonObject =
    buildJsonObject {
        put("jsonrpc", "2.0")
        put("id", id)
        putJsonObject("error") {
            put("code", code)
            put("message", message)
        }
    }

internal fun toolJson(tool: McpTool): JsonObject =
    buildJsonObject {
        put("name", tool.name)
        put("description", tool.description)
        putJsonObject("inputSchema") {
            put("type", "object")
            putJsonObject("properties") {
                tool.parameters.forEach { parameter ->
                    putJsonObject(parameter.name) {
                        put(
                            "type",
                            when (parameter.type) {
                                McpParameterType.STRING -> "string"
                                McpParameterType.INTEGER -> "integer"
                                McpParameterType.BOOLEAN -> "boolean"
                            },
                        )
                        put("description", parameter.description)
                    }
                }
            }
            putJsonArray("required") { tool.parameters.filter { it.required }.forEach { add(JsonPrimitive(it.name)) } }
        }
    }

internal fun resultJson(result: ToolResult): JsonObject =
    buildJsonObject {
        put(
            "content",
            buildJsonArray {
                result.content.forEach { content ->
                    add(
                        when (content) {
                            is ToolContent.Text -> buildJsonObject {
                                put("type", "text")
                                put("text", content.text)
                            }
                            is ToolContent.Image -> buildJsonObject {
                                put("type", "image")
                                put("data", Base64.encode(content.bytes))
                                put("mimeType", content.mimeType)
                            }
                        },
                    )
                }
            },
        )
        put("isError", result.isError)
    }

private fun JsonObject.toArguments(): Map<String, Any?> = mapValues { (_, value) -> value.toValue() }

private fun JsonElement.toValue(): Any? =
    when (this) {
        is JsonNull -> null
        is JsonPrimitive -> when {
            isString -> content
            booleanOrNull != null -> booleanOrNull
            longOrNull != null -> longOrNull
            else -> doubleOrNull
        }
        is JsonObject -> toArguments()
        is JsonArray -> map { it.toValue() }
    }
