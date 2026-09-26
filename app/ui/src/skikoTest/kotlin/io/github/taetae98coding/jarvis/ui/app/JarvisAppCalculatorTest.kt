package io.github.taetae98coding.jarvis.ui.app

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.runComposeUiTest
import io.github.taetae98coding.jarvis.designsystem.component.JarvisTopBarDefaults
import io.github.taetae98coding.jarvis.domain.calculator.CalculatorField
import io.github.taetae98coding.jarvis.domain.calculator.CalculatorTab
import io.github.taetae98coding.jarvis.ui.calculator.CalculatorBmiTestTag
import io.github.taetae98coding.jarvis.ui.calculator.CalculatorEqualsTestTag
import io.github.taetae98coding.jarvis.ui.calculator.CalculatorScreenTestTag
import io.github.taetae98coding.jarvis.ui.calculator.CalculatorTestTag
import io.github.taetae98coding.jarvis.ui.calculator.calculatorHistoryTestTag
import io.github.taetae98coding.jarvis.ui.calculator.calculatorInputTestTag
import io.github.taetae98coding.jarvis.ui.calculator.calculatorTabTestTag
import kotlin.test.Test
import kotlin.test.assertEquals

/** docs/common/calculator.html */
@OptIn(ExperimentalTestApi::class)
class JarvisAppCalculatorTest {
    @Test
    fun cardOpensScreenAndBackReturnsHome() = runComposeUiTest {
        setContent { TestJarvisApp() }

        openCalculator()
        onNodeWithTag(CalculatorScreenTestTag).assertIsDisplayed()

        onNodeWithContentDescription(JarvisTopBarDefaults.BackContentDescription).performClick()
        onNodeWithTag(CalculatorTestTag).assertIsDisplayed()
    }

    @Test
    fun typedExpressionAndEqualsArePersisted() = runComposeUiTest {
        val calculator = FakeCalculatorSettingsRepository()
        setContent { TestJarvisApp(calculator = calculator) }

        openCalculator()
        onNodeWithTag(calculatorInputTestTag(CalculatorField.EXPRESSION)).performTextInput("6×7")
        onNodeWithTag(CalculatorEqualsTestTag).performClick()
        waitForIdle()

        onNodeWithTag(calculatorInputTestTag(CalculatorField.EXPRESSION)).assertTextContains("42")
        assertEquals("42", calculator.inputs.value[CalculatorField.EXPRESSION])
        assertEquals(listOf("6×7"), calculator.history.value)
        onNodeWithTag(calculatorHistoryTestTag(0)).assertExists()
    }

    @Test
    fun savedTabAndInputsAreRestored() = runComposeUiTest {
        val calculator = FakeCalculatorSettingsRepository().apply {
            tab.value = CalculatorTab.BMI
            inputs.value = mapOf(CalculatorField.BMI_HEIGHT to "170", CalculatorField.BMI_WEIGHT to "60")
        }
        setContent { TestJarvisApp(calculator = calculator) }

        openCalculator()

        onNodeWithTag(calculatorTabTestTag(CalculatorTab.BMI)).assertIsSelected()
        onNodeWithTag(calculatorInputTestTag(CalculatorField.BMI_HEIGHT)).assertTextContains("170")
        onNodeWithTag(CalculatorBmiTestTag, useUnmergedTree = true).assertTextEquals("BMI 20.8 · 정상")
    }

    // 카드가 그리드 끝에 있어 창이 작으면 아직 컴포즈되지 않았다.
    private fun ComposeUiTest.openCalculator() {
        onNode(hasScrollToNodeAction()).performScrollToNode(hasTestTag(CalculatorTestTag))
        onNodeWithTag(CalculatorTestTag).performClick()
    }
}
