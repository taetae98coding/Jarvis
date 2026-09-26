package io.github.taetae98coding.jarvis.ui.app

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.runComposeUiTest
import io.github.taetae98coding.jarvis.designsystem.component.JarvisTopBarDefaults
import io.github.taetae98coding.jarvis.domain.unitconverter.MeasureUnit
import io.github.taetae98coding.jarvis.domain.unitconverter.UnitCategory
import io.github.taetae98coding.jarvis.domain.unitconverter.UnitSelection
import io.github.taetae98coding.jarvis.ui.unitconverter.UnitConverterCopyTestTag
import io.github.taetae98coding.jarvis.ui.unitconverter.UnitConverterFromTestTag
import io.github.taetae98coding.jarvis.ui.unitconverter.UnitConverterInputTestTag
import io.github.taetae98coding.jarvis.ui.unitconverter.UnitConverterResultTestTag
import io.github.taetae98coding.jarvis.ui.unitconverter.UnitConverterScreenTestTag
import io.github.taetae98coding.jarvis.ui.unitconverter.UnitConverterSummaryTestTag
import io.github.taetae98coding.jarvis.ui.unitconverter.UnitConverterSwapTestTag
import io.github.taetae98coding.jarvis.ui.unitconverter.UnitConverterTestTag
import io.github.taetae98coding.jarvis.ui.unitconverter.unitConverterCategoryTestTag
import kotlin.test.Test
import kotlin.test.assertEquals

/** docs/common/unit-converter.html */
@OptIn(ExperimentalTestApi::class)
class JarvisAppUnitConverterTest {
    @Test
    fun cardOpensScreenAndBackReturnsHome() = runComposeUiTest {
        setContent { TestJarvisApp() }

        openCard()
        onNodeWithTag(UnitConverterScreenTestTag).assertIsDisplayed()

        onNodeWithContentDescription(JarvisTopBarDefaults.BackContentDescription).performClick()
        onNodeWithTag(UnitConverterTestTag).assertExists()
    }

    @Test
    fun typedValueIsConvertedPersistedSwappedAndCopied() = runComposeUiTest {
        val settings = FakeUnitConverterSettingsRepository()
        val clipboard = RecordingClipboardManager()
        setContent { TestJarvisApp(unitConverter = settings, clipboard = clipboard) }

        openCard()
        onNodeWithTag(unitConverterCategoryTestTag(UnitCategory.AREA)).performClick().assertIsSelected()
        onNodeWithTag(UnitConverterInputTestTag).performTextInput("84")

        // 넓이의 기본 방향은 평 → m² 다. 바꾸면 m² → 평 이 된다.
        onNode(hasText("277.685950413 m²") and hasAnyAncestor(hasTestTag(UnitConverterResultTestTag)), useUnmergedTree = true).assertExists()

        onNodeWithTag(UnitConverterSwapTestTag).performClick()
        onNodeWithTag(UnitConverterFromTestTag).assertTextContains("제곱미터", substring = true)
        onNode(hasText("25.41 평") and hasAnyAncestor(hasTestTag(UnitConverterResultTestTag)), useUnmergedTree = true).assertExists()
        onNodeWithTag(UnitConverterCopyTestTag).performClick()

        assertEquals(UnitCategory.AREA, settings.category.value)
        assertEquals(UnitSelection(MeasureUnit.SQUARE_METER, MeasureUnit.PYEONG, "84"), settings.selections.value[UnitCategory.AREA])
        assertEquals(listOf("25.41"), clipboard.copied)
    }

    @Test
    fun savedConversionIsRestoredOnCardAndScreen() = runComposeUiTest {
        val settings = FakeUnitConverterSettingsRepository().apply {
            category.value = UnitCategory.TEMPERATURE
            selections.value = mapOf(UnitCategory.TEMPERATURE to UnitSelection(MeasureUnit.CELSIUS, MeasureUnit.FAHRENHEIT, "100"))
        }
        setContent { TestJarvisApp(unitConverter = settings) }

        scrollToCard()
        onNodeWithTag(UnitConverterSummaryTestTag, useUnmergedTree = true).assertTextContains("100 °C = 212 °F")

        onNodeWithTag(UnitConverterTestTag).performClick()
        onNodeWithTag(unitConverterCategoryTestTag(UnitCategory.TEMPERATURE)).assertIsSelected()
        onNodeWithTag(UnitConverterInputTestTag).assertTextContains("100")
        onNode(hasText("212 °F") and hasAnyAncestor(hasTestTag(UnitConverterResultTestTag)), useUnmergedTree = true).assertExists()
    }

    // 카드는 그리드 끝에 있어 창이 작으면 아직 구성되지 않았을 수 있다.
    private fun ComposeUiTest.scrollToCard() {
        onNode(hasScrollToNodeAction()).performScrollToNode(hasTestTag(UnitConverterTestTag))
    }

    private fun ComposeUiTest.openCard() {
        scrollToCard()
        onNodeWithTag(UnitConverterTestTag).performClick()
    }
}
