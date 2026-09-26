package io.github.taetae98coding.jarvis.domain.devtools

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

object ColorConverter {
    fun convert(input: String): List<DevToolOutput> {
        if (input.isBlank()) return emptyList()

        val color = parse(input)
            ?: return listOf(DevToolOutput(DevToolOutputKind.HEX, DevToolValue.Error(DevToolError.InvalidColor)))

        return listOf(
            DevToolOutput(DevToolOutputKind.HEX, DevToolValue.Text(color.toHex())),
            DevToolOutput(DevToolOutputKind.RGB, DevToolValue.Text(color.toRgbString())),
            DevToolOutput(DevToolOutputKind.HSL, DevToolValue.Text(color.toHsl().toString())),
        )
    }

    /** `#RGB`·`#RRGGBB`(`#` 생략 가능), `rgb(r, g, b)`·`r, g, b`, `hsl(h, s%, l%)`. 범위를 벗어나면 null. */
    fun parse(input: String): RgbColor? {
        val text = input.trim().lowercase()

        return when {
            text.startsWith("hsl") -> parseHsl(text.removeFunction("hsl") ?: return null)
            text.startsWith("rgb") -> parseRgb(text.removeFunction("rgb") ?: return null)
            text.any { it == ',' || it == ' ' } -> parseRgb(text)
            else -> parseHex(text.removePrefix("#"))
        }
    }

    private fun parseHex(hex: String): RgbColor? {
        if (hex.any { it.digitToIntOrNull(16) == null }) return null

        val full = when (hex.length) {
            3 -> hex.map { "$it$it" }.joinToString("")
            6 -> hex
            else -> return null
        }

        return RgbColor(full.substring(0, 2).toInt(16), full.substring(2, 4).toInt(16), full.substring(4, 6).toInt(16))
    }

    private fun parseRgb(body: String): RgbColor? {
        val channels = body.splitArguments().map { it.toIntOrNull()?.takeIf { value -> value in 0..255 } ?: return null }
        if (channels.size != 3) return null

        return RgbColor(channels[0], channels[1], channels[2])
    }

    private fun parseHsl(body: String): RgbColor? {
        val parts = body.splitArguments()
        if (parts.size != 3) return null

        val hue = parts[0].removeSuffix("deg").toDoubleOrNull()?.takeIf { it in 0.0..360.0 } ?: return null
        val saturation = parts[1].removeSuffix("%").toDoubleOrNull()?.takeIf { it in 0.0..100.0 } ?: return null
        val lightness = parts[2].removeSuffix("%").toDoubleOrNull()?.takeIf { it in 0.0..100.0 } ?: return null

        return Hsl(hue, saturation, lightness).toRgb()
    }

    private fun String.removeFunction(name: String): String? {
        val body = removePrefix(name).trim()
        if (!body.startsWith("(") || !body.endsWith(")")) return null

        return body.substring(1, body.length - 1)
    }

    private fun String.splitArguments(): List<String> = split(',', ' ').filter(String::isNotEmpty)
}

/** 도 단위 색상과 백분율 채도·명도. */
data class Hsl(val hue: Double, val saturation: Double, val lightness: Double) {
    override fun toString(): String = "hsl(${hue.roundToInt() % 360}, ${saturation.roundToInt()}%, ${lightness.roundToInt()}%)"

    fun toRgb(): RgbColor {
        val s = saturation / 100
        val l = lightness / 100
        val chroma = (1 - abs(2 * l - 1)) * s
        val sector = (hue % 360) / 60
        val x = chroma * (1 - abs(sector % 2 - 1))
        val (r, g, b) = when (sector.toInt()) {
            0 -> Triple(chroma, x, 0.0)
            1 -> Triple(x, chroma, 0.0)
            2 -> Triple(0.0, chroma, x)
            3 -> Triple(0.0, x, chroma)
            4 -> Triple(x, 0.0, chroma)
            else -> Triple(chroma, 0.0, x)
        }
        val m = l - chroma / 2

        fun channel(value: Double): Int = ((value + m) * 255).roundToInt().coerceIn(0, 255)

        return RgbColor(channel(r), channel(g), channel(b))
    }
}

fun RgbColor.toHex(): String = buildString {
    append('#')
    listOf(red, green, blue).forEach {
        append(HexDigits[it ushr 4])
        append(HexDigits[it and 0x0F])
    }
}

fun RgbColor.toRgbString(): String = "rgb($red, $green, $blue)"

fun RgbColor.toHsl(): Hsl {
    val r = red / 255.0
    val g = green / 255.0
    val b = blue / 255.0
    val high = max(r, max(g, b))
    val low = min(r, min(g, b))
    val lightness = (high + low) / 2
    val delta = high - low

    if (delta == 0.0) return Hsl(0.0, 0.0, lightness * 100)

    val saturation = delta / (1 - abs(2 * lightness - 1))
    val hue = when (high) {
        r -> 60 * (((g - b) / delta).mod(6.0))
        g -> 60 * ((b - r) / delta + 2)
        else -> 60 * ((r - g) / delta + 4)
    }

    return Hsl(hue, saturation * 100, lightness * 100)
}
