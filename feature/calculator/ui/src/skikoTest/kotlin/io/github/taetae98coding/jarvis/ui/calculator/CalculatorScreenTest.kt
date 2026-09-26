package io.github.taetae98coding.jarvis.ui.calculator

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
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import io.github.taetae98coding.jarvis.designsystem.theme.JarvisTheme
import io.github.taetae98coding.jarvis.domain.calculator.CalculateBmiUseCase
import io.github.taetae98coding.jarvis.domain.calculator.CalculatePercentUseCase
import io.github.taetae98coding.jarvis.domain.calculator.CalculationHistoryEntry
import io.github.taetae98coding.jarvis.domain.calculator.CalculationResult
import io.github.taetae98coding.jarvis.domain.calculator.CalculatorField
import io.github.taetae98coding.jarvis.domain.calculator.CalculatorTab
import io.github.taetae98coding.jarvis.domain.calculator.EvaluateExpressionUseCase
import io.github.taetae98coding.jarvis.domain.calculator.PercentMode
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class CalculatorScreenTest {
    private val evaluate = EvaluateExpressionUseCase()

    @Test
    fun tabSelectsTool() = runComposeUiTest {
        val screen = setScreen()

        onNodeWithTag(calculatorTabTestTag(CalculatorTab.EXPRESSION)).assertIsSelected()
        onNodeWithTag(calculatorTabTestTag(CalculatorTab.BMI)).performClick()

        onNodeWithTag(calculatorTabTestTag(CalculatorTab.BMI)).assertIsSelected()
        onNodeWithTag(calculatorTabTestTag(CalculatorTab.EXPRESSION)).assertIsNotSelected()
        assertEquals(CalculatorTab.BMI, screen.tab)
        onNodeWithTag(calculatorInputTestTag(CalculatorField.BMI_HEIGHT)).assertExists()
    }

    @Test
    fun keypadBuildsExpressionAndShowsLiveResult() = runComposeUiTest {
        val screen = setScreen()

        listOf(CalculatorKey.ONE, CalculatorKey.TWO, CalculatorKey.MULTIPLY, CalculatorKey.OPEN_PARENTHESIS, CalculatorKey.THREE, CalculatorKey.ADD, CalculatorKey.FOUR)
            .forEach { onNodeWithTag(it.testTag).performClick() }

        assertEquals("12×(3+4", screen.text(CalculatorField.EXPRESSION))
        onNodeWithTag(CalculatorResultTestTag, useUnmergedTree = true).assertTextEquals("괄호가 닫히지 않았습니다")

        onNodeWithTag(CalculatorKey.CLOSE_PARENTHESIS.testTag).performClick()
        onNodeWithTag(CalculatorResultTestTag, useUnmergedTree = true).assertTextEquals("= 84")
    }

    @Test
    fun keypadInsertsAtCursorAndBackspaceDeletesBeforeIt() = runComposeUiTest {
        val screen = setScreen(inputs = mapOf(CalculatorField.EXPRESSION to "13"))
        screen.states.getValue(CalculatorField.EXPRESSION).edit { selection = TextRange(1) }

        onNodeWithTag(CalculatorKey.TWO.testTag).performClick()
        assertEquals("123", screen.text(CalculatorField.EXPRESSION))

        onNodeWithContentDescription("한 글자 지우기").performClick()
        onNodeWithContentDescription("한 글자 지우기").performClick()
        assertEquals("3", screen.text(CalculatorField.EXPRESSION))

        onNodeWithTag(CalculatorKey.CLEAR.testTag).performClick()
        assertEquals("", screen.text(CalculatorField.EXPRESSION))
        onNodeWithTag(CalculatorResultTestTag).assertDoesNotExist()
    }

    @Test
    fun errorsHaveNoCopyButton() = runComposeUiTest {
        setScreen(inputs = mapOf(CalculatorField.EXPRESSION to "1/0"))

        onNodeWithTag(CalculatorResultTestTag, useUnmergedTree = true).assertTextEquals("0 으로 나눌 수 없습니다")
        onNodeWithTag(CalculatorCopyTestTag).assertDoesNotExist()
    }

    @Test
    fun equalsReplacesInputWithAnswer() = runComposeUiTest {
        val screen = setScreen(inputs = mapOf(CalculatorField.EXPRESSION to "0.1+0.2"))

        onNodeWithTag(CalculatorEqualsTestTag).performClick()

        assertEquals(listOf("0.1+0.2"), screen.equalsCalls)
        assertEquals("0.3", screen.text(CalculatorField.EXPRESSION))
    }

    @Test
    fun imeActionActsAsEquals() = runComposeUiTest {
        val screen = setScreen(inputs = mapOf(CalculatorField.EXPRESSION to "2^10"))

        onNodeWithTag(calculatorInputTestTag(CalculatorField.EXPRESSION)).performImeAction()

        assertEquals("1024", screen.text(CalculatorField.EXPRESSION))
    }

    @Test
    fun equalsOnErrorKeepsInput() = runComposeUiTest {
        val screen = setScreen(inputs = mapOf(CalculatorField.EXPRESSION to "3+"))

        onNodeWithTag(CalculatorEqualsTestTag).performClick()

        assertEquals("3+", screen.text(CalculatorField.EXPRESSION))
    }

    @Test
    fun copyPutsAnswerOnClipboard() = runComposeUiTest {
        val clipboard = RecordingClipboardManager()
        setScreen(inputs = mapOf(CalculatorField.EXPRESSION to "1/3"), clipboard = clipboard)

        onNodeWithTag(CalculatorCopyTestTag).performClick()

        assertEquals(listOf("0.333333333333"), clipboard.copied)
    }

    @Test
    fun historyRowReusesExpressionAndClearButtonClears() = runComposeUiTest {
        val screen = setScreen(history = listOf(CalculationHistoryEntry("6×7", "42"), CalculationHistoryEntry("2^3", "8")))

        onNodeWithTag(calculatorHistoryTestTag(1)).performScrollTo().performClick()
        assertEquals("2^3", screen.text(CalculatorField.EXPRESSION))

        onNodeWithTag(CalculatorClearHistoryTestTag).performScrollTo().performClick()
        assertEquals(1, screen.clearCalls)
    }

    @Test
    fun emptyHistorySaysSo() = runComposeUiTest {
        setScreen()

        onNodeWithText("아직 계산한 식이 없습니다").assertExists()
        onNodeWithTag(CalculatorClearHistoryTestTag).assertDoesNotExist()
    }

    @Test
    fun percentTabShowsAllThreeForms() = runComposeUiTest {
        setScreen(
            tab = CalculatorTab.PERCENT,
            inputs = mapOf(
                CalculatorField.PERCENT_OF_BASE to "200",
                CalculatorField.PERCENT_OF_RATE to "15",
                CalculatorField.RATIO_PART to "50",
                CalculatorField.RATIO_WHOLE to "200",
                CalculatorField.CHANGE_FROM to "100",
                CalculatorField.CHANGE_TO to "80",
            ),
        )

        onNodeWithTag(calculatorPercentTestTag(PercentMode.PERCENT_OF), useUnmergedTree = true).assertTextEquals("= 30")
        onNodeWithTag(calculatorPercentTestTag(PercentMode.RATIO), useUnmergedTree = true).assertTextEquals("= 25%")
        onNodeWithTag(calculatorPercentTestTag(PercentMode.CHANGE), useUnmergedTree = true).assertTextEquals("20% 감소")
    }

    @Test
    fun percentAnswerIsMissingUntilBothFieldsAreFilled() = runComposeUiTest {
        setScreen(tab = CalculatorTab.PERCENT, inputs = mapOf(CalculatorField.RATIO_PART to "5", CalculatorField.CHANGE_FROM to "0", CalculatorField.CHANGE_TO to "3"))

        onNodeWithTag(calculatorPercentTestTag(PercentMode.RATIO)).assertDoesNotExist()
        onNodeWithTag(calculatorPercentTestTag(PercentMode.CHANGE), useUnmergedTree = true).assertTextEquals("X 가 0 이면 증감률이 없습니다")
    }

    @Test
    fun bmiTabShowsCategoryRangeAndCriteria() = runComposeUiTest {
        setScreen(tab = CalculatorTab.BMI, inputs = mapOf(CalculatorField.BMI_HEIGHT to "170", CalculatorField.BMI_WEIGHT to "75"))

        onNodeWithTag(CalculatorBmiTestTag, useUnmergedTree = true).assertTextEquals("BMI 26.0 · 1단계 비만")
        onNodeWithText("이 키의 정상 체중: 53.5–66.2 kg").assertExists()
        onNodeWithText("대한비만학회 성인 기준").assertExists()
        onNodeWithText("23–24.9").assertExists()
    }

    @Test
    fun bmiNamesTheInvalidField() = runComposeUiTest {
        setScreen(tab = CalculatorTab.BMI, inputs = mapOf(CalculatorField.BMI_HEIGHT to "17", CalculatorField.BMI_WEIGHT to "75"))

        onNodeWithTag(CalculatorBmiTestTag, useUnmergedTree = true).assertTextEquals("키는 30–300 cm 사이의 숫자여야 합니다")
    }

    private class Screen(val states: Map<CalculatorField, TextFieldState>) {
        var tab by mutableStateOf(CalculatorTab.EXPRESSION)
        val equalsCalls = mutableListOf<String>()
        var clearCalls = 0

        fun text(field: CalculatorField): String = states.getValue(field).text.toString()
    }

    private fun ComposeUiTest.setScreen(
        tab: CalculatorTab = CalculatorTab.EXPRESSION,
        inputs: Map<CalculatorField, String> = emptyMap(),
        history: List<CalculationHistoryEntry> = emptyList(),
        clipboard: RecordingClipboardManager = RecordingClipboardManager(),
    ): Screen {
        val screen = Screen(CalculatorField.entries.associateWith { TextFieldState(inputs[it].orEmpty()) }).apply { this.tab = tab }

        setContent {
            CompositionLocalProvider(LocalClipboardManager provides clipboard) {
                JarvisTheme {
                    // 입력 칸 글자를 컴포지션에서 읽어, 친 글자마다 답이 다시 셈해지게 한다.
                    val current = screen.states.mapValues { (_, state) -> state.text.toString() }
                    CalculatorScreen(
                        tab = screen.tab,
                        onSelectTab = { screen.tab = it },
                        input = { screen.states.getValue(it) },
                        results = calculatorResults(current, evaluate, CalculatePercentUseCase(), CalculateBmiUseCase()),
                        history = history,
                        onEquals = { expression ->
                            screen.equalsCalls += expression
                            (evaluate(expression) as? CalculationResult.Value)?.text
                        },
                        onClearHistory = { screen.clearCalls++ },
                        onBack = {},
                    )
                }
            }
        }
        return screen
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
