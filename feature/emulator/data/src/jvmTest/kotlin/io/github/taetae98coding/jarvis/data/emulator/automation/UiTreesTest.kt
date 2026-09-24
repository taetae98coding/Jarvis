package io.github.taetae98coding.jarvis.data.emulator.automation

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class UiTreesTest {
    private val dump = """
        <?xml version='1.0' encoding='UTF-8' standalone='yes' ?><hierarchy rotation="0"><node index="0" text="" resource-id="" class="android.widget.FrameLayout" package="a" content-desc="" clickable="false" focused="false" bounds="[0,0][1080,2400]"><node index="0" text="확인 &amp; 닫기" resource-id="android:id/button1" class="android.widget.Button" package="a" content-desc="" clickable="true" focused="false" bounds="[100,200][300,400]" /><node index="1" text="" resource-id="" class="android.view.View" package="a" content-desc="" clickable="false" focused="false" bounds="[0,0][10,10]" /></node></hierarchy>
    """.trimIndent()

    @Test
    fun keepsNodesWithTextOrIdAndScalesCenters() {
        assertEquals(1848 to 2960, parseWmSize("Physical size: 1848x2960\n"))
        assertEquals(1080 to 1920, parseWmSize("Physical size: 1848x2960\nOverride size: 1080x1920\n"))
        assertEquals("Button \"확인 & 닫기\" id=button1 clickable (100, 150)", parseUiAutomatorTree(dump, scale = 0.5))
    }

    @Test
    fun wdaTreeUsesPointCenters() {
        val source = Json.parseToJsonElement(
            """{"type":"Application","label":" ","name":null,"isVisible":"1","rect":{"x":0,"y":0,"width":440,"height":956},
               "children":[{"type":"Button","label":"설정","name":"settings","isVisible":"1","rect":{"x":10,"y":20,"width":40,"height":30}},
                           {"type":"StaticText","label":"숨음","isVisible":"0","rect":{"x":0,"y":0,"width":1,"height":1}}]}""",
        )

        assertEquals("Button \"설정\" id=settings (30, 35)", parseWdaTree(source))
    }

    @Test
    fun fitScaleCapsTheLongSide() {
        assertEquals(1.0, fitScale(878, 1280))
        assertEquals(0.625, fitScale(1280, 2048))
        assertEquals(800, scaled(1280, 0.625))
    }
}
