package io.github.taetae98coding.jarvis.ui.calculator

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.taetae98coding.jarvis.designsystem.component.JarvisCard
import io.github.taetae98coding.jarvis.designsystem.component.JarvisIconButton
import io.github.taetae98coding.jarvis.designsystem.component.JarvisLabeledValue
import io.github.taetae98coding.jarvis.designsystem.component.JarvisTopBar
import io.github.taetae98coding.jarvis.designsystem.icon.JarvisIcons
import io.github.taetae98coding.jarvis.designsystem.theme.JarvisTheme
import io.github.taetae98coding.jarvis.domain.calculator.BmiCategory
import io.github.taetae98coding.jarvis.domain.calculator.BmiResult
import io.github.taetae98coding.jarvis.domain.calculator.CalculationHistoryEntry
import io.github.taetae98coding.jarvis.domain.calculator.CalculationResult
import io.github.taetae98coding.jarvis.domain.calculator.CalculatorField
import io.github.taetae98coding.jarvis.domain.calculator.CalculatorTab
import io.github.taetae98coding.jarvis.domain.calculator.PercentMode

const val CalculatorScreenTestTag = "calculator:screen"
const val CalculatorResultTestTag = "calculator:result"
const val CalculatorCopyTestTag = "calculator:copy"
const val CalculatorEqualsTestTag = "calculator:key:equals"
const val CalculatorClearHistoryTestTag = "calculator:history:clear"
const val CalculatorBmiTestTag = "calculator:bmi"

fun calculatorTabTestTag(tab: CalculatorTab): String = "calculator:tab:${tab.storedValue}"

fun calculatorInputTestTag(field: CalculatorField): String = "calculator:input:${field.storedValue}"

fun calculatorHistoryTestTag(index: Int): String = "calculator:history:$index"

fun calculatorPercentTestTag(mode: PercentMode): String = "calculator:percent:${mode.storedValue}"

@Composable
internal fun CalculatorScreen(
    viewModel: CalculatorViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tab by viewModel.tab.collectAsStateWithLifecycle()
    val results by viewModel.results.collectAsStateWithLifecycle()
    val history by viewModel.history.collectAsStateWithLifecycle()

    CalculatorScreen(
        tab = tab,
        onSelectTab = viewModel::onSelectTab,
        // 입력 칸은 한 번 채우고 스스로 든다. 저장소 값을 곧장 보이면 친 글자가 늦게 돌아온 옛 값에
        // 덮인다(docs/common/calculator.html#implementation).
        input = { field ->
            val state = rememberTextFieldState(initialText = viewModel.inputOf(field))
            LaunchedEffect(state) {
                snapshotFlow { state.text.toString() }.collect { viewModel.onInputChange(field, it) }
            }
            state
        },
        results = results,
        history = history,
        onEquals = viewModel::onEquals,
        onClearHistory = viewModel::onClearHistory,
        onBack = onBack,
        modifier = modifier,
    )
}

/** [input] 은 칸마다 한 번 불리고, 부른 자리에 기억된 상태를 준다. */
@Composable
internal fun CalculatorScreen(
    tab: CalculatorTab,
    onSelectTab: (CalculatorTab) -> Unit,
    input: @Composable (CalculatorField) -> TextFieldState,
    results: CalculatorResults,
    history: List<CalculationHistoryEntry>,
    onEquals: (String) -> String?,
    onClearHistory: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .testTag(CalculatorScreenTestTag),
        verticalArrangement = Arrangement.spacedBy(JarvisTheme.dimens.spacing.m),
    ) {
        JarvisTopBar(title = "계산기", onBack = onBack)

        PrimaryTabRow(selectedTabIndex = tab.ordinal) {
            CalculatorTab.entries.forEach { option ->
                Tab(
                    selected = option == tab,
                    onClick = { onSelectTab(option) },
                    text = { Text(option.label) },
                    modifier = Modifier.testTag(calculatorTabTestTag(option)),
                )
            }
        }

        when (tab) {
            CalculatorTab.EXPRESSION -> ExpressionTab(
                input = input(CalculatorField.EXPRESSION),
                result = results.expression,
                history = history,
                onEquals = onEquals,
                onClearHistory = onClearHistory,
            )

            CalculatorTab.PERCENT -> PercentMode.entries.forEach { mode ->
                val (first, second) = mode.fields
                PercentCard(mode = mode, first = input(first), second = input(second), answer = mode.answer(results.percent.getValue(mode)))
            }

            CalculatorTab.BMI -> BmiTab(
                height = input(CalculatorField.BMI_HEIGHT),
                weight = input(CalculatorField.BMI_WEIGHT),
                result = results.bmi,
            )
        }
    }
}

@Composable
private fun ExpressionTab(
    input: TextFieldState,
    result: CalculationResult,
    history: List<CalculationHistoryEntry>,
    onEquals: (String) -> String?,
    onClearHistory: () -> Unit,
) {
    val equals = { onEquals(input.text.toString())?.let(input::setTextAndPlaceCursorAtEnd) }

    OutlinedTextField(
        state = input,
        modifier = Modifier.fillMaxWidth().testTag(calculatorInputTestTag(CalculatorField.EXPRESSION)),
        placeholder = { Text("12×(3+4)") },
        textStyle = CalculatorDefaults.expressionTextStyle,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        onKeyboardAction = { equals() },
        lineLimits = TextFieldLineLimits.SingleLine,
    )

    ResultRow(result)

    Keypad(
        onKey = { key ->
            when (key) {
                CalculatorKey.CLEAR -> input.setTextAndPlaceCursorAtEnd("")
                CalculatorKey.BACKSPACE -> input.deleteBeforeCursor()
                CalculatorKey.EQUALS -> equals()
                else -> key.insert?.let(input::insertAtCursor)
            }
        },
    )

    HistoryCard(
        history = history,
        onReuse = { input.setTextAndPlaceCursorAtEnd(it.expression) },
        onClear = onClearHistory,
    )
}

@Composable
private fun ResultRow(result: CalculationResult) {
    when (result) {
        CalculationResult.Empty -> Unit

        is CalculationResult.Value -> Row(verticalAlignment = Alignment.CenterVertically) {
            // Compose Multiplatform 1.12.1 의 LocalClipboard 는 공통 코드에서 텍스트 ClipEntry 를 만들 수 없다. 개발자 도구
            // 결과 줄·터미널 선택 복사와 같은 사정이라, ClipEntry 에 공통 팩토리가 생기면 세 곳을 함께 옮긴다.
            @Suppress("DEPRECATION")
            val clipboard = LocalClipboardManager.current

            SelectionContainer(modifier = Modifier.weight(1f)) {
                Text(
                    text = "= ${result.text}",
                    style = CalculatorDefaults.resultTextStyle,
                    modifier = Modifier.testTag(CalculatorResultTestTag),
                )
            }

            JarvisIconButton(
                icon = JarvisIcons.Copy,
                contentDescription = "결과 복사",
                onClick = { runCatching { clipboard.setText(AnnotatedString(result.text)) } },
                modifier = Modifier.testTag(CalculatorCopyTestTag),
            )
        }

        is CalculationResult.Error -> Text(
            text = result.error.message,
            style = JarvisTheme.typography.bodyMedium,
            color = if (result.error.isIncomplete) JarvisTheme.colorScheme.onSurfaceVariant else JarvisTheme.colorScheme.error,
            modifier = Modifier.testTag(CalculatorResultTestTag),
        )
    }
}

@Composable
private fun Keypad(onKey: (CalculatorKey) -> Unit) {
    val spacing = CalculatorDefaults.keySpacing

    Column(verticalArrangement = Arrangement.spacedBy(spacing)) {
        CalculatorKey.Rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(spacing)) {
                row.forEach { key -> Key(key = key, onClick = { onKey(key) }) }
            }
        }
    }
}

@Composable
private fun RowScope.Key(key: CalculatorKey, onClick: () -> Unit) {
    // = 은 줄의 남은 세 칸을 차지한다.
    val modifier = Modifier
        .weight(if (key == CalculatorKey.EQUALS) 3f else 1f)
        .height(CalculatorDefaults.keyHeight)
        .testTag(key.testTag)
    val content: @Composable RowScope.() -> Unit = {
        if (key == CalculatorKey.BACKSPACE) {
            Icon(imageVector = JarvisIcons.Backspace, contentDescription = key.label)
        } else {
            Text(text = key.label, style = CalculatorDefaults.keyTextStyle)
        }
    }

    when {
        key == CalculatorKey.EQUALS -> Button(onClick = onClick, modifier = modifier, content = content)
        key.isDigit -> FilledTonalButton(onClick = onClick, modifier = modifier, content = content)
        else -> OutlinedButton(onClick = onClick, modifier = modifier, content = content)
    }
}

@Composable
private fun HistoryCard(
    history: List<CalculationHistoryEntry>,
    onReuse: (CalculationHistoryEntry) -> Unit,
    onClear: () -> Unit,
) {
    JarvisCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "최근 계산",
                style = JarvisTheme.typography.labelLarge,
                color = JarvisTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )

            if (history.isNotEmpty()) {
                TextButton(onClick = onClear, modifier = Modifier.testTag(CalculatorClearHistoryTestTag)) {
                    Text("기록 지우기")
                }
            }
        }

        if (history.isEmpty()) {
            Text(
                text = "아직 계산한 식이 없습니다",
                style = JarvisTheme.typography.bodyMedium,
                color = JarvisTheme.colorScheme.onSurfaceVariant,
            )
        }

        history.forEachIndexed { index, entry ->
            JarvisLabeledValue(
                label = entry.expression,
                value = "= ${entry.resultText}",
                labelStyle = JarvisTheme.codeTextStyle,
                valueStyle = JarvisTheme.codeTextStyle,
                modifier = Modifier
                    .clickable { onReuse(entry) }
                    .testTag(calculatorHistoryTestTag(index)),
            )
        }
    }
}

@Composable
private fun PercentCard(
    mode: PercentMode,
    first: TextFieldState,
    second: TextFieldState,
    answer: String?,
) {
    JarvisCard(modifier = Modifier.fillMaxWidth()) {
        Text(text = mode.title, style = JarvisTheme.typography.titleSmall)

        Row(horizontalArrangement = Arrangement.spacedBy(JarvisTheme.dimens.spacing.s)) {
            val (firstField, secondField) = mode.fields
            NumberField(state = first, label = mode.firstLabel, field = firstField, modifier = Modifier.weight(1f))
            NumberField(state = second, label = mode.secondLabel, field = secondField, modifier = Modifier.weight(1f))
        }

        if (answer != null) {
            SelectionContainer {
                Text(
                    text = answer,
                    style = CalculatorDefaults.answerTextStyle,
                    modifier = Modifier.testTag(calculatorPercentTestTag(mode)),
                )
            }
        }
    }
}

@Composable
private fun BmiTab(
    height: TextFieldState,
    weight: TextFieldState,
    result: BmiResult,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(JarvisTheme.dimens.spacing.s)) {
        NumberField(state = height, label = "키 (cm)", field = CalculatorField.BMI_HEIGHT, modifier = Modifier.weight(1f))
        NumberField(state = weight, label = "몸무게 (kg)", field = CalculatorField.BMI_WEIGHT, modifier = Modifier.weight(1f))
    }

    val value = result as? BmiResult.Value
    val message = when (result) {
        BmiResult.Empty -> null
        BmiResult.InvalidHeight -> "키는 30–300 cm 사이의 숫자여야 합니다"
        BmiResult.InvalidWeight -> "몸무게는 1–500 kg 사이의 숫자여야 합니다"
        is BmiResult.Value -> "BMI ${result.text} · ${result.category.label}"
    }

    JarvisCard(modifier = Modifier.fillMaxWidth()) {
        if (message != null) {
            Text(
                text = message,
                style = if (value != null) CalculatorDefaults.answerTextStyle else JarvisTheme.typography.bodyMedium,
                color = if (value != null) JarvisTheme.colorScheme.onSurface else JarvisTheme.colorScheme.error,
                modifier = Modifier.testTag(CalculatorBmiTestTag),
            )
        }

        if (value != null) {
            Text(
                text = "이 키의 정상 체중: ${value.normalWeightMinText}–${value.normalWeightMaxText} kg",
                style = JarvisTheme.typography.bodyMedium,
                color = JarvisTheme.colorScheme.onSurfaceVariant,
            )
        }

        Text(text = BmiCriteria, style = JarvisTheme.typography.labelLarge, color = JarvisTheme.colorScheme.onSurfaceVariant)

        BmiCategory.entries.forEach { category ->
            val current = category == value?.category
            JarvisLabeledValue(
                label = category.range,
                value = category.label,
                labelColor = if (current) JarvisTheme.colorScheme.primary else JarvisTheme.colorScheme.onSurfaceVariant,
                valueStyle = if (current) CalculatorDefaults.currentCategoryTextStyle else JarvisTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun NumberField(
    state: TextFieldState,
    label: String,
    field: CalculatorField,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        state = state,
        modifier = modifier.testTag(calculatorInputTestTag(field)),
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
        lineLimits = TextFieldLineLimits.SingleLine,
    )
}

internal object CalculatorDefaults {
    val keyHeight: Dp
        @Composable @ReadOnlyComposable get() = JarvisTheme.dimens.spacing.xxl * 2

    val keySpacing: Dp
        @Composable @ReadOnlyComposable get() = JarvisTheme.dimens.spacing.s

    val keyTextStyle: TextStyle
        @Composable @ReadOnlyComposable get() = JarvisTheme.typography.titleLarge

    val expressionTextStyle: TextStyle
        @Composable @ReadOnlyComposable get() = JarvisTheme.codeTextStyle.merge(JarvisTheme.typography.titleLarge.copy(fontFamily = null))

    val resultTextStyle: TextStyle
        @Composable @ReadOnlyComposable get() = JarvisTheme.typography.headlineSmall

    val answerTextStyle: TextStyle
        @Composable @ReadOnlyComposable get() = JarvisTheme.typography.titleMedium

    val currentCategoryTextStyle: TextStyle
        @Composable @ReadOnlyComposable get() = JarvisTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
}
