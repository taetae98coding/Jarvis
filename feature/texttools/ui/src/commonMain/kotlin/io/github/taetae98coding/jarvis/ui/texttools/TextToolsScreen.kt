package io.github.taetae98coding.jarvis.ui.texttools

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.taetae98coding.jarvis.designsystem.component.JarvisCard
import io.github.taetae98coding.jarvis.designsystem.component.JarvisIconButton
import io.github.taetae98coding.jarvis.designsystem.component.JarvisLabeledValue
import io.github.taetae98coding.jarvis.designsystem.component.JarvisSwitchRow
import io.github.taetae98coding.jarvis.designsystem.component.JarvisTopBar
import io.github.taetae98coding.jarvis.designsystem.icon.JarvisIcons
import io.github.taetae98coding.jarvis.designsystem.theme.JarvisTheme
import io.github.taetae98coding.jarvis.domain.texttools.LimitBasis
import io.github.taetae98coding.jarvis.domain.texttools.LimitProgress
import io.github.taetae98coding.jarvis.domain.texttools.PasswordOptions
import io.github.taetae98coding.jarvis.domain.texttools.PasswordStrength
import io.github.taetae98coding.jarvis.domain.texttools.TextStats
import io.github.taetae98coding.jarvis.domain.texttools.TextTool
import io.github.taetae98coding.jarvis.domain.texttools.TextTransform
import io.github.taetae98coding.jarvis.domain.texttools.TextTransformOutput
import kotlin.math.roundToInt

const val TextToolsScreenTestTag = "texttools:screen"
const val TextToolsInputTestTag = "texttools:input"
const val TextToolsLimitTestTag = "texttools:limit"
const val TextToolsProgressTestTag = "texttools:progress"
const val TextToolsPasswordTestTag = "texttools:password"
const val TextToolsPasswordCopyTestTag = "texttools:password:copy"
const val TextToolsPasswordRegenerateTestTag = "texttools:password:regenerate"
const val TextToolsPasswordStrengthTestTag = "texttools:password:strength"
const val TextToolsPasswordLengthTestTag = "texttools:password:length"

fun textToolsTabTestTag(tool: TextTool): String = "texttools:tab:${tool.storedValue}"

fun textToolsStatTestTag(kind: String): String = "texttools:stat:$kind"

fun textToolsBasisTestTag(basis: LimitBasis): String = "texttools:basis:${basis.storedValue}"

fun textToolsPasswordOptionTestTag(name: String): String = "texttools:password:option:$name"

fun textToolsTransformTestTag(transform: TextTransform): String = "texttools:transform:${transform.name.lowercase()}"

fun textToolsTransformCopyTestTag(transform: TextTransform): String = "texttools:transform:copy:${transform.name.lowercase()}"

@Composable
internal fun TextToolsScreen(
    viewModel: TextToolsViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val stats by viewModel.stats.collectAsStateWithLifecycle()
    val limit by viewModel.limit.collectAsStateWithLifecycle()
    val progress by viewModel.progress.collectAsStateWithLifecycle()
    val transforms by viewModel.transforms.collectAsStateWithLifecycle()
    val password by viewModel.password.collectAsStateWithLifecycle()
    val passwordOptions by viewModel.passwordOptions.collectAsStateWithLifecycle()

    // 입력 칸은 한 번 채우고 스스로 든다. 저장소 값을 곧장 보이면 친 글자가 늦게 돌아온 옛 값에 덮인다
    // (docs/common/text-tools.html#implementation). 글자 수·대소문자 탭이 같은 칸을 나눠 쓴다.
    val input = rememberTextFieldState(initialText = viewModel.inputOf(settings))
    val limitInput = rememberTextFieldState(initialText = viewModel.limitTextOf(limit))

    LaunchedEffect(input) {
        snapshotFlow { input.text.toString() }.collect(viewModel::onInputChange)
    }
    LaunchedEffect(limitInput) {
        snapshotFlow { limitInput.text.toString() }.collect(viewModel::onLimitTextChange)
    }

    TextToolsScreen(
        tool = settings.tool,
        onSelectTool = viewModel::onSelectTool,
        onBack = onBack,
        input = input,
        stats = stats,
        limitInput = limitInput,
        limitBasis = limit.basis,
        onLimitBasisChange = viewModel::onLimitBasisChange,
        progress = progress,
        password = password,
        passwordOptions = passwordOptions,
        onPasswordOptionsChange = viewModel::onPasswordOptionsChange,
        onRegeneratePassword = viewModel::onRegeneratePassword,
        transforms = transforms,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TextToolsScreen(
    tool: TextTool,
    onSelectTool: (TextTool) -> Unit,
    onBack: () -> Unit,
    input: TextFieldState,
    stats: TextStats,
    limitInput: TextFieldState,
    limitBasis: LimitBasis,
    onLimitBasisChange: (LimitBasis) -> Unit,
    progress: LimitProgress?,
    password: String,
    passwordOptions: PasswordOptions,
    onPasswordOptionsChange: (PasswordOptions) -> Unit,
    onRegeneratePassword: () -> Unit,
    transforms: List<TextTransformOutput>,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .testTag(TextToolsScreenTestTag),
        verticalArrangement = Arrangement.spacedBy(JarvisTheme.dimens.spacing.m),
    ) {
        JarvisTopBar(title = "텍스트 도구", onBack = onBack)

        PrimaryTabRow(selectedTabIndex = tool.ordinal) {
            TextTool.entries.forEach { option ->
                Tab(
                    selected = option == tool,
                    onClick = { onSelectTool(option) },
                    text = { Text(option.label) },
                    modifier = Modifier.testTag(textToolsTabTestTag(option)),
                )
            }
        }

        when (tool) {
            TextTool.COUNT -> {
                TextInput(input)
                Stats(stats)
                Limit(
                    limitInput = limitInput,
                    basis = limitBasis,
                    onBasisChange = onLimitBasisChange,
                    progress = progress,
                )
            }

            TextTool.PASSWORD -> Password(
                password = password,
                options = passwordOptions,
                onOptionsChange = onPasswordOptionsChange,
                onRegenerate = onRegeneratePassword,
            )

            TextTool.CASE -> {
                TextInput(input)
                transforms.forEach { TransformRow(it) }
            }
        }
    }
}

@Composable
private fun TextInput(input: TextFieldState) {
    OutlinedTextField(
        state = input,
        modifier = Modifier.fillMaxWidth().testTag(TextToolsInputTestTag),
        placeholder = { Text("세거나 바꿀 글을 붙여 넣으세요") },
        lineLimits = TextFieldLineLimits.MultiLine(minHeightInLines = TextToolsDefaults.InputMinLines),
    )
}

@Composable
private fun Stats(stats: TextStats) {
    JarvisCard(modifier = Modifier.fillMaxWidth()) {
        StatKind.entries.forEach { kind ->
            JarvisLabeledValue(
                label = kind.label,
                value = kind.value(stats),
                modifier = Modifier.testTag(textToolsStatTestTag(kind.name.lowercase())),
            )
        }
    }
}

@Composable
private fun Limit(
    limitInput: TextFieldState,
    basis: LimitBasis,
    onBasisChange: (LimitBasis) -> Unit,
    progress: LimitProgress?,
) {
    JarvisCard(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            state = limitInput,
            modifier = Modifier.fillMaxWidth().testTag(TextToolsLimitTestTag),
            label = { Text("목표 글자 수") },
            placeholder = { Text("예: 500") },
            inputTransformation = DigitsOnly,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            lineLimits = TextFieldLineLimits.SingleLine,
        )

        FlowRow(horizontalArrangement = Arrangement.spacedBy(JarvisTheme.dimens.spacing.s)) {
            LimitBasis.entries.forEach { option ->
                FilterChip(
                    selected = option == basis,
                    onClick = { onBasisChange(option) },
                    label = { Text(option.label) },
                    modifier = Modifier.testTag(textToolsBasisTestTag(option)),
                )
            }
        }

        if (progress != null) {
            val color = if (progress.isOver) JarvisTheme.colorScheme.error else JarvisTheme.colorScheme.primary

            LinearProgressIndicator(
                progress = { progress.fraction },
                modifier = Modifier.fillMaxWidth().testTag(TextToolsProgressTestTag),
                color = color,
            )

            val summary = "${progress.current.grouped()} / ${progress.target.grouped()}${basis.unit}"
            Text(
                text = if (progress.isOver) "$summary · ${progress.overBy.grouped()}${basis.unit} 초과" else summary,
                style = JarvisTheme.typography.bodyMedium,
                color = if (progress.isOver) color else JarvisTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun Password(
    password: String,
    options: PasswordOptions,
    onOptionsChange: (PasswordOptions) -> Unit,
    onRegenerate: () -> Unit,
) {
    // Compose Multiplatform 1.12.1 의 LocalClipboard 는 공통 코드에서 텍스트 ClipEntry 를 만들 수 없다. 개발자 도구·터미널과
    // 같은 사정이라, ClipEntry 에 공통 팩토리가 생기면 세 곳을 함께 옮긴다.
    @Suppress("DEPRECATION")
    val clipboard = LocalClipboardManager.current

    JarvisCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SelectionContainer(modifier = Modifier.weight(1f)) {
                Text(
                    text = password,
                    style = JarvisTheme.codeTextStyle,
                    modifier = Modifier.testTag(TextToolsPasswordTestTag),
                )
            }

            JarvisIconButton(
                icon = JarvisIcons.Copy,
                contentDescription = "비밀번호 복사",
                onClick = { runCatching { clipboard.setText(AnnotatedString(password)) } },
                modifier = Modifier.testTag(TextToolsPasswordCopyTestTag),
            )
            JarvisIconButton(
                icon = JarvisIcons.RotateRight,
                contentDescription = "다시 만들기",
                onClick = onRegenerate,
                modifier = Modifier.testTag(TextToolsPasswordRegenerateTestTag),
            )
        }

        val strength = options.strength
        Text(
            text = "강도: ${strength.label} (${options.entropyBits.toInt()}비트)",
            style = JarvisTheme.typography.labelLarge,
            color = TextToolsDefaults.strengthColor(strength),
            modifier = Modifier.testTag(TextToolsPasswordStrengthTestTag),
        )
    }

    JarvisCard(modifier = Modifier.fillMaxWidth()) {
        Text(text = "길이 ${options.length}", style = JarvisTheme.typography.bodyMedium)
        Slider(
            value = options.length.toFloat(),
            onValueChange = { onOptionsChange(options.copy(length = it.roundToInt())) },
            valueRange = PasswordOptions.MinLength.toFloat()..PasswordOptions.MaxLength.toFloat(),
            steps = PasswordOptions.MaxLength - PasswordOptions.MinLength - 1,
            modifier = Modifier.fillMaxWidth().testTag(TextToolsPasswordLengthTestTag),
        )

        // 켜진 종류가 하나뿐이면 그 스위치를 잠근다(docs/common/text-tools.html R6).
        val onlyOneClass = options.classes.size == 1
        PasswordClassSwitch("대문자 (A–Z)", "uppercase", options.uppercase, onlyOneClass) { onOptionsChange(options.copy(uppercase = it)) }
        PasswordClassSwitch("소문자 (a–z)", "lowercase", options.lowercase, onlyOneClass) { onOptionsChange(options.copy(lowercase = it)) }
        PasswordClassSwitch("숫자 (0–9)", "digits", options.digits, onlyOneClass) { onOptionsChange(options.copy(digits = it)) }
        PasswordClassSwitch("기호 (!@#…)", "symbols", options.symbols, onlyOneClass) { onOptionsChange(options.copy(symbols = it)) }
        JarvisSwitchRow(
            title = "헷갈리는 글자 빼기 (0 O 1 l I)",
            checked = options.excludeAmbiguous,
            onCheckedChange = { onOptionsChange(options.copy(excludeAmbiguous = it)) },
            switchModifier = Modifier.testTag(textToolsPasswordOptionTestTag("exclude_ambiguous")),
        )
    }
}

@Composable
private fun PasswordClassSwitch(
    title: String,
    name: String,
    checked: Boolean,
    onlyOneClass: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    JarvisSwitchRow(
        title = title,
        checked = checked,
        onCheckedChange = onCheckedChange,
        enabled = !(checked && onlyOneClass),
        switchModifier = Modifier.testTag(textToolsPasswordOptionTestTag(name)),
    )
}

@Composable
private fun TransformRow(output: TextTransformOutput) {
    @Suppress("DEPRECATION")
    val clipboard = LocalClipboardManager.current
    val label = output.transform.label

    JarvisCard(modifier = Modifier.fillMaxWidth().testTag(textToolsTransformTestTag(output.transform))) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = label,
                style = JarvisTheme.typography.labelLarge,
                color = JarvisTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            JarvisIconButton(
                icon = JarvisIcons.Copy,
                contentDescription = "$label 복사",
                onClick = { runCatching { clipboard.setText(AnnotatedString(output.text)) } },
                modifier = Modifier.testTag(textToolsTransformCopyTestTag(output.transform)),
            )
        }

        SelectionContainer {
            Text(text = output.text, style = JarvisTheme.codeTextStyle)
        }
    }
}

private val DigitsOnly = InputTransformation {
    if (!asCharSequence().all(Char::isDigit)) revertAllChanges()
}

internal object TextToolsDefaults {
    const val InputMinLines = 6

    @Composable
    @ReadOnlyComposable
    fun strengthColor(strength: PasswordStrength): Color =
        when (strength) {
            PasswordStrength.WEAK -> JarvisTheme.colorScheme.error
            PasswordStrength.FAIR -> JarvisTheme.colorScheme.tertiary
            PasswordStrength.STRONG, PasswordStrength.VERY_STRONG -> JarvisTheme.colorScheme.primary
        }
}
