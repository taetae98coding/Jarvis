package io.github.taetae98coding.jarvis.domain.mcp

/** MCP `tools/list` 한 줄. 인자 스키마를 JSON 으로 바꾸는 일은 data 가 한다. */
data class McpTool(
    val name: String,
    val description: String,
    val parameters: List<McpToolParameter> = emptyList(),
)

data class McpToolParameter(
    val name: String,
    val type: McpParameterType,
    val description: String,
    val required: Boolean = true,
)

enum class McpParameterType { STRING, INTEGER, BOOLEAN }

data class ToolResult(
    val content: List<ToolContent>,
    val isError: Boolean = false,
) {
    companion object {
        fun text(text: String): ToolResult = ToolResult(listOf(ToolContent.Text(text)))

        fun error(message: String): ToolResult = ToolResult(listOf(ToolContent.Text(message)), isError = true)
    }
}

sealed interface ToolContent {
    data class Text(val text: String) : ToolContent

    class Image(val bytes: ByteArray, val mimeType: String) : ToolContent
}
