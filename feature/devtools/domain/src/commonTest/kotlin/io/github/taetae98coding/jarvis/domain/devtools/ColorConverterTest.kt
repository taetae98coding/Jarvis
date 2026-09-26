package io.github.taetae98coding.jarvis.domain.devtools

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ColorConverterTest {
    @Test
    fun hexToAllNotations() {
        assertEquals(
            listOf(
                DevToolOutput(DevToolOutputKind.HEX, DevToolValue.Text("#2E5DAA")),
                DevToolOutput(DevToolOutputKind.RGB, DevToolValue.Text("rgb(46, 93, 170)")),
                DevToolOutput(DevToolOutputKind.HSL, DevToolValue.Text("hsl(217, 57%, 42%)")),
            ),
            ColorConverter.convert("#2e5daa"),
        )
    }

    @Test
    fun parsesEveryNotation() {
        val blue = RgbColor(46, 93, 170)

        assertEquals(blue, ColorConverter.parse("2E5DAA"))
        assertEquals(blue, ColorConverter.parse("rgb(46, 93, 170)"))
        assertEquals(blue, ColorConverter.parse(" 46,93,170 "))
        assertEquals(blue, ColorConverter.parse("46 93 170"))
        assertEquals(RgbColor(0xAA, 0xBB, 0xCC), ColorConverter.parse("#abc"))
        assertEquals(RgbColor(0, 128, 0), ColorConverter.parse("hsl(120, 100%, 25%)"))
        assertEquals(RgbColor(128, 128, 128), ColorConverter.parse("HSL(0 0% 50%)"))
    }

    @Test
    fun rgbToHsl() {
        assertEquals("hsl(0, 100%, 50%)", RgbColor(255, 0, 0).toHsl().toString())
        assertEquals("hsl(0, 0%, 100%)", RgbColor(255, 255, 255).toHsl().toString())
        assertEquals("hsl(0, 0%, 0%)", RgbColor(0, 0, 0).toHsl().toString())
        assertEquals("hsl(210, 65%, 20%)", RgbColor(18, 52, 86).toHsl().toString())
        assertEquals("hsl(210, 25%, 73%)", RgbColor(170, 187, 204).toHsl().toString())
    }

    @Test
    fun hslRoundTripKeepsRgb() {
        listOf(RgbColor(46, 93, 170), RgbColor(255, 0, 0), RgbColor(12, 200, 99), RgbColor(250, 250, 5)).forEach { color ->
            assertEquals(color, color.toHsl().toRgb())
        }
    }

    @Test
    fun rejectsOutOfRangeAndGarbage() {
        listOf("#12345", "#GGGGGG", "rgb(256, 0, 0)", "rgb(1, 2)", "hsl(361, 50%, 50%)", "hsl(10, 101%, 50%)", "rgb 1,2,3", "blue")
            .forEach { assertNull(ColorConverter.parse(it), it) }

        assertEquals(
            listOf(DevToolOutput(DevToolOutputKind.HEX, DevToolValue.Error(DevToolError.InvalidColor))),
            ColorConverter.convert("blue"),
        )
        assertEquals(emptyList(), ColorConverter.convert(""))
    }
}
