package io.github.taetae98coding.jarvis.ui.devtools

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.taetae98coding.jarvis.designsystem.component.JarvisCard
import io.github.taetae98coding.jarvis.designsystem.component.JarvisIconButton
import io.github.taetae98coding.jarvis.designsystem.component.JarvisSwitchRow
import io.github.taetae98coding.jarvis.designsystem.component.JarvisTopBar
import io.github.taetae98coding.jarvis.designsystem.icon.JarvisIcons
import io.github.taetae98coding.jarvis.designsystem.theme.JarvisTheme
import io.github.taetae98coding.jarvis.domain.devtools.DevTool
import io.github.taetae98coding.jarvis.domain.devtools.DevToolOutput
import io.github.taetae98coding.jarvis.domain.devtools.DevToolOutputKind
import io.github.taetae98coding.jarvis.domain.devtools.DevToolValue
import io.github.taetae98coding.jarvis.domain.devtools.RgbColor

const val DevToolsScreenTestTag = "devtools:screen"
const val DevToolsInputTestTag = "devtools:input"
const val DevToolsNowTestTag = "devtools:now"
const val DevToolsGenerateTestTag = "devtools:generate"
const val DevToolsSwatchTestTag = "devtools:swatch"

fun devToolsToolTestTag(tool: DevTool): String = "devtools:tool:${tool.storedValue}"

fun devToolsOutputTestTag(kind: DevToolOutputKind): String = "devtools:output:${kind.name.lowercase()}"

fun devToolsCopyTestTag(kind: DevToolOutputKind): String = "devtools:copy:${kind.name.lowercase()}"

@Composable
internal fun DevToolsScreen(
    viewModel: DevToolsViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val results by viewModel.results.collectAsStateWithLifecycle()
    val base64UrlSafe by viewModel.base64UrlSafe.collectAsStateWithLifecycle()
    val uuidCount by viewModel.uuidCount.collectAsStateWithLifecycle()
    val uuidUppercase by viewModel.uuidUppercase.collectAsStateWithLifecycle()
    val uuids by viewModel.uuids.collectAsStateWithLifecycle()

    // 입력 칸은 도구마다 한 번 채우고 스스로 든다. 저장소 값을 곧장 보이면 친 글자가 늦게 돌아온 옛 값에
    // 덮인다(docs/common/dev-utilities.html#implementation).
    key(state.tool) {
        val input = rememberTextFieldState(initialText = viewModel.inputOf(state))
        val tool = state.tool

        LaunchedEffect(input) {
            snapshotFlow { input.text.toString() }.collect { viewModel.onInputChange(tool, it) }
        }

        DevToolsScreen(
            tool = tool,
            input = input,
            results = results,
            onSelectTool = viewModel::onSelectTool,
            onBack = onBack,
            onNow = { input.setTextAndPlaceCursorAtEnd(viewModel.currentEpochSecondsText()) },
            base64UrlSafe = base64UrlSafe,
            onBase64UrlSafeChange = viewModel::onBase64UrlSafeChange,
            uuids = uuids,
            uuidCount = uuidCount,
            onUuidCountChange = viewModel::onUuidCountChange,
            uuidUppercase = uuidUppercase,
            onUuidUppercaseChange = viewModel::onUuidUppercaseChange,
            onGenerateUuids = viewModel::onGenerateUuids,
            modifier = modifier,
        )
    }
}

@Composable
internal fun DevToolsScreen(
    tool: DevTool,
    input: TextFieldState,
    results: DevToolResults,
    onSelectTool: (DevTool) -> Unit,
    onBack: () -> Unit,
    onNow: () -> Unit,
    base64UrlSafe: Boolean,
    onBase64UrlSafeChange: (Boolean) -> Unit,
    uuids: List<String>,
    uuidCount: Int,
    onUuidCountChange: (Int) -> Unit,
    uuidUppercase: Boolean,
    onUuidUppercaseChange: (Boolean) -> Unit,
    onGenerateUuids: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = JarvisTheme.dimens.spacing

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .testTag(DevToolsScreenTestTag),
        verticalArrangement = Arrangement.spacedBy(spacing.m),
    ) {
        JarvisTopBar(title = "개발자 도구", onBack = onBack)

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(spacing.s),
            verticalArrangement = Arrangement.spacedBy(spacing.xs),
        ) {
            DevTool.entries.forEach { option ->
                FilterChip(
                    selected = option == tool,
                    onClick = { onSelectTool(option) },
                    label = { Text(option.label) },
                    modifier = Modifier.testTag(devToolsToolTestTag(option)),
                )
            }
        }

        if (tool == DevTool.UUID) {
            UuidControls(
                count = uuidCount,
                onCountChange = onUuidCountChange,
                uppercase = uuidUppercase,
                onUppercaseChange = onUuidUppercaseChange,
                onGenerate = onGenerateUuids,
            )
            OutputRow(DevToolOutput(DevToolOutputKind.UUID, DevToolValue.Text(uuids.joinToString("\n"))))
        } else {
            OutlinedTextField(
                state = input,
                modifier = Modifier.fillMaxWidth().testTag(DevToolsInputTestTag),
                placeholder = { Text(tool.placeholder) },
                textStyle = JarvisTheme.codeTextStyle,
            )

            when (tool) {
                DevTool.TIMESTAMP -> OutlinedButton(onClick = onNow, modifier = Modifier.testTag(DevToolsNowTestTag)) {
                    Text("지금")
                }

                DevTool.BASE64 -> JarvisSwitchRow(
                    title = "URL-safe (- _ 알파벳)",
                    checked = base64UrlSafe,
                    onCheckedChange = onBase64UrlSafeChange,
                )

                else -> Unit
            }

            results.swatch?.let { Swatch(it) }

            results.outputs.forEach { OutputRow(it) }
        }
    }
}

@Composable
private fun UuidControls(
    count: Int,
    onCountChange: (Int) -> Unit,
    uppercase: Boolean,
    onUppercaseChange: (Boolean) -> Unit,
    onGenerate: () -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(JarvisTheme.dimens.spacing.s),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        UuidCounts.forEach { option ->
            FilterChip(
                selected = option == count,
                onClick = { onCountChange(option) },
                label = { Text("${option}개") },
            )
        }

        OutlinedButton(onClick = onGenerate, modifier = Modifier.testTag(DevToolsGenerateTestTag)) {
            Text("생성")
        }
    }

    JarvisSwitchRow(title = "대문자", checked = uppercase, onCheckedChange = onUppercaseChange)
}

@Composable
private fun OutputRow(output: DevToolOutput) {
    // Compose Multiplatform 1.12.1 의 LocalClipboard 는 공통 코드에서 텍스트 ClipEntry 를 만들 수 없다. 터미널 선택
    // 복사(TerminalPane)와 같은 사정이라, ClipEntry 에 공통 팩토리가 생기면 두 곳을 함께 옮긴다.
    @Suppress("DEPRECATION")
    val clipboard = LocalClipboardManager.current
    val label = output.kind.label

    JarvisCard(modifier = Modifier.fillMaxWidth().testTag(devToolsOutputTestTag(output.kind))) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = label,
                style = JarvisTheme.typography.labelLarge,
                color = JarvisTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )

            val value = output.value
            if (value is DevToolValue.Text) {
                JarvisIconButton(
                    icon = JarvisIcons.Copy,
                    contentDescription = "$label 복사",
                    onClick = { runCatching { clipboard.setText(AnnotatedString(value.text)) } },
                    modifier = Modifier.testTag(devToolsCopyTestTag(output.kind)),
                )
            }
        }

        when (val value = output.value) {
            is DevToolValue.Text -> SelectionContainer {
                Text(text = displayValue(output.kind, value.text), style = JarvisTheme.codeTextStyle)
            }

            is DevToolValue.Error -> Text(
                text = value.error.message,
                style = JarvisTheme.typography.bodyMedium,
                color = JarvisTheme.colorScheme.error,
            )
        }
    }
}

@Composable
private fun Swatch(color: RgbColor) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(DevToolsDefaults.swatchHeight)
            // 사용자가 넣은 색이라 테마 토큰이 될 수 없다(docs/common/dev-utilities.html#implementation).
            .background(Color(red = color.red, green = color.green, blue = color.blue), JarvisTheme.shapes.medium)
            .border(JarvisTheme.dimens.stroke.thin, JarvisTheme.colorScheme.outlineVariant, JarvisTheme.shapes.medium)
            .testTag(DevToolsSwatchTestTag),
    )
}

internal object DevToolsDefaults {
    val swatchHeight: Dp
        @Composable @ReadOnlyComposable get() = JarvisTheme.dimens.spacing.xxl * 2
}
