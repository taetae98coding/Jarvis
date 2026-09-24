package io.github.taetae98coding.jarvis.data.emulator.automation

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlin.math.roundToInt

// 트리가 깊은 화면에서 Claude 의 문맥을 다 쓰지 않게 자른다.
internal const val UiTreeMaxLines = 400

/**
 * `uiautomator dump` 의 XML 에서 글자·설명·식별자가 있거나 누를 수 있는 노드만 한 줄씩 뽑는다. `bounds` 는
 * 디스플레이 픽셀이라 [scale] 을 곱해 캡처 이미지 좌표로 바꾼다. XML 파서를 쓰지 않는다 — 노드마다 속성이
 * 한 줄에 다 있고, 필요한 것은 속성 몇 개뿐이다.
 */
internal fun parseUiAutomatorTree(xml: String, scale: Double): String {
    val lines = NodePattern.findAll(xml).mapNotNull { match ->
        val attributes = AttributePattern.findAll(match.groupValues[1]).associate { it.groupValues[1] to unescapeXml(it.groupValues[2]) }
        val bounds = attributes["bounds"]?.let(BoundsPattern::matchEntire) ?: return@mapNotNull null
        val (left, top, right, bottom) = bounds.destructured.toList().map(String::toInt)

        val text = attributes["text"].orEmpty()
        val description = attributes["content-desc"].orEmpty()
        val id = attributes["resource-id"].orEmpty()
        val clickable = attributes["clickable"] == "true"
        if (text.isEmpty() && description.isEmpty() && id.isEmpty() && !clickable) return@mapNotNull null

        val kind = attributes["class"].orEmpty().substringAfterLast('.')
        val x = ((left + right) / 2.0 * scale).roundToInt()
        val y = ((top + bottom) / 2.0 * scale).roundToInt()

        buildString {
            append(kind)
            if (text.isNotEmpty()) append(" \"").append(text).append('"')
            if (description.isNotEmpty()) append(" desc=\"").append(description).append('"')
            if (id.isNotEmpty()) append(" id=").append(id.substringAfter(":id/"))
            if (clickable) append(" clickable")
            if (attributes["focused"] == "true") append(" focused")
            append(" (").append(x).append(", ").append(y).append(')')
        }
    }.toList()

    return lines.take(UiTreeMaxLines).joinToString("\n").ifEmpty { "UI 요소가 없습니다." }
}

/** `wm size` 의 `Physical size: 1848x2960`, 바꿔 둔 크기가 있으면 `Override size:` 쪽. 회전과 무관한 값이다. */
internal fun parseWmSize(output: String): Pair<Int, Int>? {
    val sizes = WmSizePattern.findAll(output).associate { it.groupValues[1] to (it.groupValues[2].toInt() to it.groupValues[3].toInt()) }

    return sizes["Override"] ?: sizes["Physical"]
}

/**
 * WebDriverAgent `/source?format=json` 의 트리. 좌표는 포인트이고 캡처도 포인트 크기라 그대로 쓴다. 이름·라벨·값이
 * 있는 요소만 한 줄씩이다.
 */
internal fun parseWdaTree(root: JsonElement): String {
    val lines = mutableListOf<String>()

    fun visit(element: JsonElement) {
        val node = element as? JsonObject ?: return
        val type = node.string("type").orEmpty().removePrefix("XCUIElementType")
        val label = node.string("label").orEmpty().trim()
        val name = node.string("name").orEmpty().trim()
        val value = node.string("value").orEmpty().trim()
        val visible = (node["isVisible"] as? JsonPrimitive)?.let { it.booleanOrNull ?: (it.contentOrNull == "1") } ?: true
        val rect = node["rect"] as? JsonObject

        if (visible && rect != null && (label.isNotBlank() || name.isNotBlank() || value.isNotBlank())) {
            val x = (rect.number("x") + rect.number("width") / 2).roundToInt()
            val y = (rect.number("y") + rect.number("height") / 2).roundToInt()
            lines += buildString {
                append(type)
                if (label.isNotEmpty()) append(" \"").append(label).append('"')
                if (name.isNotEmpty() && name != label) append(" id=").append(name)
                if (value.isNotEmpty() && value != label) append(" value=\"").append(value).append('"')
                append(" (").append(x).append(", ").append(y).append(')')
            }
        }

        (node["children"] as? JsonArray)?.forEach(::visit)
    }

    visit(root)

    return lines.take(UiTreeMaxLines).joinToString("\n").ifEmpty { "UI 요소가 없습니다." }
}

private fun JsonObject.string(key: String): String? = (this[key] as? JsonPrimitive)?.contentOrNull?.takeIf { it != "null" }

private fun JsonObject.number(key: String): Double = (this[key] as? JsonPrimitive)?.doubleOrNull ?: 0.0

private fun unescapeXml(value: String): String =
    value.replace("&lt;", "<").replace("&gt;", ">").replace("&quot;", "\"").replace("&apos;", "'").replace("&#10;", "\n").replace("&amp;", "&")

// 자기 닫는 노드(`<node … />`)도 같은 패턴에 걸린다. 끝의 `/` 는 속성 패턴에 걸리지 않는다.
private val NodePattern = Regex("""<node\b([^>]*)>""")

private val AttributePattern = Regex("""([\w-]+)="([^"]*)"""")

private val WmSizePattern = Regex("""(Physical|Override) size: (\d+)x(\d+)""")

private val BoundsPattern = Regex("""\[(-?\d+),(-?\d+)]\[(-?\d+),(-?\d+)]""")
