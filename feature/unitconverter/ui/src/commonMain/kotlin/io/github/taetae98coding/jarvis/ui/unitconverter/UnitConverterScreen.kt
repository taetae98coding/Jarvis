package io.github.taetae98coding.jarvis.ui.unitconverter

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.taetae98coding.jarvis.designsystem.component.JarvisCard
import io.github.taetae98coding.jarvis.designsystem.component.JarvisIconButton
import io.github.taetae98coding.jarvis.designsystem.component.JarvisTopBar
import io.github.taetae98coding.jarvis.designsystem.icon.JarvisIcons
import io.github.taetae98coding.jarvis.designsystem.theme.JarvisTheme
import io.github.taetae98coding.jarvis.domain.unitconverter.MeasureUnit
import io.github.taetae98coding.jarvis.domain.unitconverter.UnitAmount
import io.github.taetae98coding.jarvis.domain.unitconverter.UnitCategory
import io.github.taetae98coding.jarvis.domain.unitconverter.UnitConversion

const val UnitConverterScreenTestTag = "unitconverter:screen"
const val UnitConverterInputTestTag = "unitconverter:input"
const val UnitConverterSignTestTag = "unitconverter:sign"
const val UnitConverterFromTestTag = "unitconverter:from"
const val UnitConverterToTestTag = "unitconverter:to"
const val UnitConverterSwapTestTag = "unitconverter:swap"
const val UnitConverterResultTestTag = "unitconverter:result"
const val UnitConverterCopyTestTag = "unitconverter:copy"
const val UnitConverterErrorTestTag = "unitconverter:error"

fun unitConverterCategoryTestTag(category: UnitCategory): String = "unitconverter:category:${category.storedValue}"

fun unitConverterFromOptionTestTag(unit: MeasureUnit): String = "unitconverter:from:${unit.storedValue}"

fun unitConverterToOptionTestTag(unit: MeasureUnit): String = "unitconverter:to:${unit.storedValue}"

fun unitConverterRowTestTag(unit: MeasureUnit): String = "unitconverter:row:${unit.storedValue}"

@Composable
internal fun UnitConverterScreen(
    viewModel: UnitConverterViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val conversion by viewModel.conversion.collectAsStateWithLifecycle()

    // 입력 칸은 분류마다 한 번 채우고 스스로 든다. 저장소 값을 곧장 보이면 친 글자가 늦게 돌아온 옛 값에
    // 덮인다(docs/common/unit-converter.html#implementation).
    key(state.category) {
        val input = rememberTextFieldState(initialText = viewModel.inputOf(state))
        val category = state.category

        LaunchedEffect(input) {
            snapshotFlow { input.text.toString() }.collect { viewModel.onInputChange(category, it) }
        }

        UnitConverterScreen(
            category = category,
            from = state.selection.from,
            to = state.selection.to,
            input = input,
            conversion = conversion,
            onSelectCategory = viewModel::onSelectCategory,
            onSelectFrom = viewModel::onSelectFrom,
            onSelectTo = viewModel::onSelectTo,
            onSwap = viewModel::onSwap,
            onBack = onBack,
            modifier = modifier,
        )
    }
}

@Composable
internal fun UnitConverterScreen(
    category: UnitCategory,
    from: MeasureUnit,
    to: MeasureUnit,
    input: TextFieldState,
    conversion: UnitConversion,
    onSelectCategory: (UnitCategory) -> Unit,
    onSelectFrom: (MeasureUnit) -> Unit,
    onSelectTo: (MeasureUnit) -> Unit,
    onSwap: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = JarvisTheme.dimens.spacing
    // Compose Multiplatform 1.12.1 의 LocalClipboard 는 공통 코드에서 텍스트 ClipEntry 를 만들 수 없다. 개발자 도구
    // 복사(DevToolsScreen)와 같은 사정이라, ClipEntry 에 공통 팩토리가 생기면 함께 옮긴다.
    @Suppress("DEPRECATION")
    val clipboard = LocalClipboardManager.current
    val copy: (String) -> Unit = { text -> runCatching { clipboard.setText(AnnotatedString(text)) } }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .testTag(UnitConverterScreenTestTag),
        verticalArrangement = Arrangement.spacedBy(spacing.m),
    ) {
        JarvisTopBar(title = UnitConverterTitle, onBack = onBack)

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(spacing.s),
            verticalArrangement = Arrangement.spacedBy(spacing.xs),
        ) {
            UnitCategory.entries.forEach { option ->
                FilterChip(
                    selected = option == category,
                    onClick = { onSelectCategory(option) },
                    label = { Text(option.label) },
                    modifier = Modifier.testTag(unitConverterCategoryTestTag(option)),
                )
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(spacing.s),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                state = input,
                modifier = Modifier.weight(1f).testTag(UnitConverterInputTestTag),
                placeholder = { Text("값") },
                suffix = { Text(from.symbol) },
                textStyle = JarvisTheme.codeTextStyle,
                lineLimits = TextFieldLineLimits.SingleLine,
                // Decimal 키패드에는 빼기 기호가 없는 기기가 있어 부호는 옆 버튼으로 바꾼다(docs/platform/*.html#unit-converter).
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            )

            JarvisIconButton(
                icon = JarvisIcons.PlusMinus,
                contentDescription = "부호 바꾸기",
                onClick = { input.setTextAndPlaceCursorAtEnd(toggleSign(input.text.toString())) },
                modifier = Modifier.testTag(UnitConverterSignTestTag),
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(spacing.s),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            UnitPicker(
                selected = from,
                units = category.units,
                onSelect = onSelectFrom,
                testTag = UnitConverterFromTestTag,
                optionTestTag = ::unitConverterFromOptionTestTag,
                modifier = Modifier.weight(1f),
            )

            JarvisIconButton(
                icon = JarvisIcons.Swap,
                contentDescription = "단위 서로 바꾸기",
                onClick = onSwap,
                modifier = Modifier.testTag(UnitConverterSwapTestTag),
            )

            UnitPicker(
                selected = to,
                units = category.units,
                onSelect = onSelectTo,
                testTag = UnitConverterToTestTag,
                optionTestTag = ::unitConverterToOptionTestTag,
                modifier = Modifier.weight(1f),
            )
        }

        when (conversion) {
            UnitConversion.Empty -> Unit

            UnitConversion.InvalidNumber -> Text(
                text = "숫자가 아닙니다. 1234.5, -40, 1,000, 1e-3 처럼 적는다",
                style = JarvisTheme.typography.bodyMedium,
                color = JarvisTheme.colorScheme.error,
                modifier = Modifier.testTag(UnitConverterErrorTestTag),
            )

            is UnitConversion.Converted -> {
                ResultCard(conversion.result, onCopy = copy)

                Text(
                    text = "모든 단위",
                    style = JarvisTheme.typography.labelLarge,
                    color = JarvisTheme.colorScheme.onSurfaceVariant,
                )

                JarvisCard(modifier = Modifier.fillMaxWidth()) {
                    conversion.all.forEach { AmountRow(it, onCopy = copy) }
                }
            }
        }
    }
}

@Composable
private fun UnitPicker(
    selected: MeasureUnit,
    units: List<MeasureUnit>,
    onSelect: (MeasureUnit) -> Unit,
    testTag: String,
    optionTestTag: (MeasureUnit) -> String,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth().testTag(testTag)) {
            Text(
                text = selected.menuLabel,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Icon(imageVector = JarvisIcons.ChevronDown, contentDescription = null)
        }

        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            units.forEach { unit ->
                DropdownMenuItem(
                    text = { Text(unit.menuLabel) },
                    onClick = {
                        expanded = false
                        onSelect(unit)
                    },
                    trailingIcon = if (unit == selected) {
                        { Icon(imageVector = JarvisIcons.Check, contentDescription = "선택됨") }
                    } else {
                        null
                    },
                    modifier = Modifier.testTag(optionTestTag(unit)),
                )
            }
        }
    }
}

@Composable
private fun ResultCard(amount: UnitAmount, onCopy: (String) -> Unit) {
    JarvisCard(modifier = Modifier.fillMaxWidth().testTag(UnitConverterResultTestTag)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = amount.unit.label,
                style = JarvisTheme.typography.labelLarge,
                color = JarvisTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )

            val text = amount.text
            if (text != null) {
                JarvisIconButton(
                    icon = JarvisIcons.Copy,
                    contentDescription = "결과 복사",
                    onClick = { onCopy(text) },
                    modifier = Modifier.testTag(UnitConverterCopyTestTag),
                )
            }
        }

        SelectionContainer {
            Text(text = amountDisplay(amount), style = JarvisTheme.typography.headlineSmall)
        }
    }
}

@Composable
private fun AmountRow(amount: UnitAmount, onCopy: (String) -> Unit) {
    val text = amount.text
    val clickable = if (text == null) {
        Modifier
    } else {
        Modifier.clickable(onClickLabel = "${amount.unit.label} 값 복사") { onCopy(text) }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(clickable)
            .padding(vertical = JarvisTheme.dimens.spacing.xs)
            .testTag(unitConverterRowTestTag(amount.unit)),
        horizontalArrangement = Arrangement.spacedBy(JarvisTheme.dimens.spacing.s),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = amount.unit.label,
            style = JarvisTheme.typography.bodyMedium,
            color = JarvisTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )

        Text(
            text = amountDisplay(amount),
            style = JarvisTheme.codeTextStyle,
            color = if (text == null) JarvisTheme.colorScheme.error else JarvisTheme.colorScheme.onSurface,
        )

        if (text != null) {
            // 줄 전체가 버튼이라 아이콘은 표시일 뿐이다. 읽어 줄 이름은 onClickLabel 이 준다.
            Icon(
                imageVector = JarvisIcons.Copy,
                contentDescription = null,
                modifier = Modifier.size(JarvisTheme.dimens.iconSize.small),
                tint = JarvisTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun amountDisplay(amount: UnitAmount): String = amount.text?.let { amountText(it, amount.unit) } ?: "범위를 벗어남"

/** 빈 칸이면 빼기 기호만 넣어 이어 칠 수 있게 한다. */
internal fun toggleSign(input: String): String {
    val trimmed = input.trimStart()
    return when {
        trimmed.startsWith("-") -> trimmed.removePrefix("-")
        trimmed.startsWith("+") -> "-" + trimmed.removePrefix("+")
        else -> "-$trimmed"
    }
}
