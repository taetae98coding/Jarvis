package io.github.taetae98coding.jarvis.ui.unitconverter

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.text.AnnotatedString
import io.github.taetae98coding.jarvis.designsystem.theme.JarvisTheme
import io.github.taetae98coding.jarvis.domain.unitconverter.MeasureUnit
import io.github.taetae98coding.jarvis.domain.unitconverter.UnitCategory
import io.github.taetae98coding.jarvis.domain.unitconverter.UnitConverter
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class UnitConverterScreenTest {
    @Test
    fun chipSelectsCategory() = runComposeUiTest {
        var category by mutableStateOf(UnitCategory.LENGTH)
        setScreen(category = { category }, onSelectCategory = { category = it })

        onNodeWithTag(unitConverterCategoryTestTag(UnitCategory.LENGTH)).assertIsSelected()
        onNodeWithTag(unitConverterCategoryTestTag(UnitCategory.AREA)).performClick()

        onNodeWithTag(unitConverterCategoryTestTag(UnitCategory.AREA)).assertIsSelected()
        onNodeWithTag(unitConverterCategoryTestTag(UnitCategory.LENGTH)).assertIsNotSelected()
        assertEquals(UnitCategory.AREA, category)
    }

    @Test
    fun resultAndEveryUnitOfTheCategoryAreShown() = runComposeUiTest {
        setScreen(category = { UnitCategory.AREA }, from = MeasureUnit.PYEONG, to = MeasureUnit.SQUARE_METER, input = "1")

        onNode(hasText("3.30578512397 m²") and hasAnyAncestor(hasTestTag(UnitConverterResultTestTag)), useUnmergedTree = true)
            .assertExists()
        UnitCategory.AREA.units.forEach { onNodeWithTag(unitConverterRowTestTag(it)).assertExists() }
        onNode(hasText("1 평") and hasAnyAncestor(hasTestTag(unitConverterRowTestTag(MeasureUnit.PYEONG))), useUnmergedTree = true)
            .assertExists()
        onNode(hasText("0.000330578512397 ha") and hasAnyAncestor(hasTestTag(unitConverterRowTestTag(MeasureUnit.HECTARE))), useUnmergedTree = true)
            .assertExists()
    }

    @Test
    fun copyButtonAndRowsPutTheNumberOnClipboard() = runComposeUiTest {
        val clipboard = RecordingClipboardManager()
        setScreen(category = { UnitCategory.TEMPERATURE }, from = MeasureUnit.CELSIUS, to = MeasureUnit.FAHRENHEIT, input = "100", clipboard = clipboard)

        onNodeWithTag(UnitConverterCopyTestTag).performClick()
        onNodeWithTag(unitConverterRowTestTag(MeasureUnit.KELVIN)).performScrollTo().performClick()

        assertEquals(listOf("212", "373.15"), clipboard.copied)
    }

    @Test
    fun invalidInputShowsErrorWithoutResults() = runComposeUiTest {
        setScreen(category = { UnitCategory.LENGTH }, input = "12cm")

        onNodeWithTag(UnitConverterErrorTestTag).assertExists()
        onNodeWithTag(UnitConverterResultTestTag).assertDoesNotExist()
        onNodeWithTag(unitConverterRowTestTag(MeasureUnit.METER)).assertDoesNotExist()
    }

    @Test
    fun emptyInputShowsNothing() = runComposeUiTest {
        setScreen(category = { UnitCategory.LENGTH }, input = "")

        onNodeWithTag(UnitConverterErrorTestTag).assertDoesNotExist()
        onNodeWithTag(UnitConverterResultTestTag).assertDoesNotExist()
    }

    @Test
    fun pickersOfferOnlyTheCategoryUnits() = runComposeUiTest {
        val picked = mutableListOf<MeasureUnit>()
        setScreen(category = { UnitCategory.MASS }, from = MeasureUnit.KILOGRAM, to = MeasureUnit.POUND, onSelectTo = { picked += it })

        onNodeWithTag(UnitConverterToTestTag).performClick()
        onNodeWithTag(unitConverterToOptionTestTag(MeasureUnit.METER)).assertDoesNotExist()
        onNodeWithTag(unitConverterToOptionTestTag(MeasureUnit.GEUN)).performClick()

        assertEquals(listOf(MeasureUnit.GEUN), picked)
        onNodeWithTag(unitConverterToOptionTestTag(MeasureUnit.GEUN)).assertDoesNotExist()
    }

    @Test
    fun swapButtonAsksToSwap() = runComposeUiTest {
        var swapped = 0
        setScreen(category = { UnitCategory.LENGTH }, onSwap = { swapped++ })

        onNodeWithTag(UnitConverterSwapTestTag).performClick()

        assertEquals(1, swapped)
    }

    @Test
    fun signButtonTogglesMinus() = runComposeUiTest {
        val input = setScreen(category = { UnitCategory.TEMPERATURE }, from = MeasureUnit.CELSIUS, to = MeasureUnit.FAHRENHEIT, input = "40")

        onNodeWithTag(UnitConverterSignTestTag).performClick()
        waitForIdle()
        assertEquals("-40", input.text.toString())

        onNodeWithTag(UnitConverterSignTestTag).performClick()
        waitForIdle()
        assertEquals("40", input.text.toString())
    }

    @Test
    fun cardShowsLastConversionOrDescription() = runComposeUiTest {
        var summary by mutableStateOf<UnitConverterSummary?>(null)
        setContent { JarvisTheme { UnitConverterCard(summary = summary, onClick = {}) } }

        onNodeWithTag(UnitConverterSummaryTestTag).assertDoesNotExist()

        summary = UnitConverterSummary(input = "84", from = MeasureUnit.SQUARE_METER, result = "25.41", to = MeasureUnit.PYEONG)
        onNodeWithText("84 m² = 25.41 평").assertExists()
    }

    @Test
    fun toggleSignKeepsTheNumber() {
        assertEquals("-", toggleSign(""))
        assertEquals("-1.5", toggleSign("1.5"))
        assertEquals("1.5", toggleSign("-1.5"))
        assertEquals("-3", toggleSign("+3"))
    }

    private fun ComposeUiTest.setScreen(
        category: () -> UnitCategory,
        from: MeasureUnit? = null,
        to: MeasureUnit? = null,
        input: String = "",
        onSelectCategory: (UnitCategory) -> Unit = {},
        onSelectTo: (MeasureUnit) -> Unit = {},
        onSwap: () -> Unit = {},
        clipboard: RecordingClipboardManager = RecordingClipboardManager(),
    ): TextFieldState {
        val state = TextFieldState(input)

        setContent {
            CompositionLocalProvider(LocalClipboardManager provides clipboard) {
                JarvisTheme {
                    val current = category()
                    val fromUnit = from?.takeIf { it.category == current } ?: current.defaultFrom
                    val toUnit = to?.takeIf { it.category == current } ?: current.defaultTo
                    UnitConverterScreen(
                        category = current,
                        from = fromUnit,
                        to = toUnit,
                        input = state,
                        conversion = UnitConverter.convert(input, fromUnit, toUnit),
                        onSelectCategory = onSelectCategory,
                        onSelectFrom = {},
                        onSelectTo = onSelectTo,
                        onSwap = onSwap,
                        onBack = {},
                    )
                }
            }
        }
        return state
    }
}

// 기본 관리자(AwtClipboardManager 등)는 테스트 중에 실제 시스템 클립보드를 덮어쓴다. 기록만 하는 것으로 바꾼다.
@Suppress("DEPRECATION")
private class RecordingClipboardManager : ClipboardManager {
    val copied = mutableListOf<String>()

    override fun setText(annotatedString: AnnotatedString) {
        copied += annotatedString.text
    }

    override fun getText(): AnnotatedString? = copied.lastOrNull()?.let(::AnnotatedString)
}
