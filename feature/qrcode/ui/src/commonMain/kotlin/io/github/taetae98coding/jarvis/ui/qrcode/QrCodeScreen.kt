package io.github.taetae98coding.jarvis.ui.qrcode

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.taetae98coding.jarvis.designsystem.component.JarvisCard
import io.github.taetae98coding.jarvis.designsystem.component.JarvisSwitchRow
import io.github.taetae98coding.jarvis.designsystem.component.JarvisTopBar
import io.github.taetae98coding.jarvis.designsystem.icon.JarvisIcons
import io.github.taetae98coding.jarvis.designsystem.theme.JarvisTheme
import io.github.taetae98coding.jarvis.domain.qrcode.QrCodeInput
import io.github.taetae98coding.jarvis.domain.qrcode.QrCodeResult
import io.github.taetae98coding.jarvis.domain.qrcode.QrContentType
import io.github.taetae98coding.jarvis.domain.qrcode.QrErrorCorrection
import io.github.taetae98coding.jarvis.domain.qrcode.QrField
import io.github.taetae98coding.jarvis.domain.qrcode.WifiSecurity

const val QrCodeScreenTestTag = "qrcode:screen"
const val QrCodeImageTestTag = "qrcode:image"
const val QrCodePayloadTestTag = "qrcode:payload"
const val QrCodeCopyTestTag = "qrcode:copy"
const val QrCodeMessageTestTag = "qrcode:message"
const val QrCodeHiddenTestTag = "qrcode:hidden"

fun qrCodeTypeTestTag(type: QrContentType): String = "qrcode:type:${type.storedValue}"

fun qrCodeFieldTestTag(field: QrField): String = "qrcode:field:${field.storedValue}"

fun qrCodeErrorCorrectionTestTag(errorCorrection: QrErrorCorrection): String = "qrcode:ecc:${errorCorrection.storedValue}"

fun qrCodeSecurityTestTag(security: WifiSecurity): String = "qrcode:security:${security.storedValue}"

@Composable
internal fun QrCodeScreen(
    viewModel: QrCodeViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val input by viewModel.input.collectAsStateWithLifecycle()
    val result by viewModel.result.collectAsStateWithLifecycle()

    QrCodeScreen(
        input = input,
        result = result,
        initialFieldValue = viewModel::fieldOf,
        onSelectType = viewModel::onSelectType,
        onFieldChange = viewModel::onFieldChange,
        onErrorCorrectionChange = viewModel::onErrorCorrectionChange,
        onBack = onBack,
        modifier = modifier,
    )
}

@Composable
internal fun QrCodeScreen(
    input: QrCodeInput,
    result: QrCodeResult,
    initialFieldValue: (QrField) -> String,
    onSelectType: (QrContentType) -> Unit,
    onFieldChange: (QrField, String) -> Unit,
    onErrorCorrectionChange: (QrErrorCorrection) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = JarvisTheme.dimens.spacing

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .testTag(QrCodeScreenTestTag),
        verticalArrangement = Arrangement.spacedBy(spacing.m),
    ) {
        JarvisTopBar(title = "QR 코드", onBack = onBack)

        ChipRow {
            QrContentType.entries.forEach { type ->
                FilterChip(
                    selected = type == input.type,
                    onClick = { onSelectType(type) },
                    label = { Text(type.label) },
                    modifier = Modifier.testTag(qrCodeTypeTestTag(type)),
                )
            }
        }

        // 입력 칸은 종류마다 한 번 채우고 스스로 든다. 저장소 값을 곧장 보이면 친 글자가 늦게 돌아온 옛 값에
        // 덮인다(docs/common/qr-code.html#implementation).
        key(input.type) {
            when (input.type) {
                QrContentType.TEXT -> InputField(QrField.TEXT, initialFieldValue, onFieldChange, singleLine = false, keyboardType = KeyboardType.Uri)
                QrContentType.WIFI -> WifiFields(input, initialFieldValue, onFieldChange)
                QrContentType.CONTACT -> {
                    InputField(QrField.CONTACT_NAME, initialFieldValue, onFieldChange)
                    InputField(QrField.CONTACT_PHONE, initialFieldValue, onFieldChange, keyboardType = KeyboardType.Phone)
                    InputField(QrField.CONTACT_EMAIL, initialFieldValue, onFieldChange, keyboardType = KeyboardType.Email)
                }

                QrContentType.PHONE -> InputField(QrField.PHONE_NUMBER, initialFieldValue, onFieldChange, keyboardType = KeyboardType.Phone)
                QrContentType.SMS -> {
                    InputField(QrField.SMS_NUMBER, initialFieldValue, onFieldChange, keyboardType = KeyboardType.Phone)
                    InputField(QrField.SMS_MESSAGE, initialFieldValue, onFieldChange, singleLine = false)
                }

                QrContentType.EMAIL -> {
                    InputField(QrField.EMAIL_ADDRESS, initialFieldValue, onFieldChange, keyboardType = KeyboardType.Email)
                    InputField(QrField.EMAIL_SUBJECT, initialFieldValue, onFieldChange)
                    InputField(QrField.EMAIL_BODY, initialFieldValue, onFieldChange, singleLine = false)
                }
            }
        }

        Text(text = "오류 정정", style = JarvisTheme.typography.labelLarge, color = JarvisTheme.colorScheme.onSurfaceVariant)
        ChipRow {
            QrErrorCorrection.entries.forEach { level ->
                FilterChip(
                    selected = level == input.errorCorrection,
                    onClick = { onErrorCorrectionChange(level) },
                    label = { Text(level.label) },
                    modifier = Modifier.testTag(qrCodeErrorCorrectionTestTag(level)),
                )
            }
        }

        ResultCard(result)
    }
}

@Composable
private fun ChipRow(content: @Composable () -> Unit) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(JarvisTheme.dimens.spacing.s),
        verticalArrangement = Arrangement.spacedBy(JarvisTheme.dimens.spacing.xs),
    ) {
        content()
    }
}

@Composable
private fun WifiFields(
    input: QrCodeInput,
    initialFieldValue: (QrField) -> String,
    onFieldChange: (QrField, String) -> Unit,
) {
    val security = WifiSecurity.fromStored(input[QrField.WIFI_SECURITY])

    InputField(QrField.WIFI_SSID, initialFieldValue, onFieldChange)

    Text(text = QrField.WIFI_SECURITY.label, style = JarvisTheme.typography.labelLarge, color = JarvisTheme.colorScheme.onSurfaceVariant)
    ChipRow {
        WifiSecurity.entries.forEach { option ->
            FilterChip(
                selected = option == security,
                onClick = { onFieldChange(QrField.WIFI_SECURITY, option.storedValue) },
                label = { Text(option.label) },
                modifier = Modifier.testTag(qrCodeSecurityTestTag(option)),
            )
        }
    }

    if (security != WifiSecurity.NONE) {
        InputField(
            field = QrField.WIFI_PASSWORD,
            initialFieldValue = initialFieldValue,
            onFieldChange = onFieldChange,
            keyboardType = KeyboardType.Password,
            supporting = "비밀번호는 기기에 저장하지 않습니다. 앱을 다시 열면 다시 넣어야 합니다.",
        )
    }

    JarvisSwitchRow(
        title = QrField.WIFI_HIDDEN.label,
        checked = input[QrField.WIFI_HIDDEN] == "true",
        onCheckedChange = { onFieldChange(QrField.WIFI_HIDDEN, if (it) "true" else "") },
        switchModifier = Modifier.testTag(QrCodeHiddenTestTag),
    )
}

@Composable
private fun InputField(
    field: QrField,
    initialFieldValue: (QrField) -> String,
    onFieldChange: (QrField, String) -> Unit,
    singleLine: Boolean = true,
    keyboardType: KeyboardType = KeyboardType.Text,
    supporting: String? = null,
) {
    val state: TextFieldState = rememberTextFieldState(initialText = initialFieldValue(field))

    LaunchedEffect(state) {
        snapshotFlow { state.text.toString() }.collect { onFieldChange(field, it) }
    }

    OutlinedTextField(
        state = state,
        modifier = Modifier.fillMaxWidth().testTag(qrCodeFieldTestTag(field)),
        label = { Text(field.label) },
        placeholder = field.placeholder.takeIf { it.isNotEmpty() }?.let { { Text(it) } },
        supportingText = supporting?.let { { Text(it) } },
        lineLimits = if (singleLine) TextFieldLineLimits.SingleLine else TextFieldLineLimits.Default,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
    )
}

@Composable
private fun ResultCard(result: QrCodeResult) {
    // Compose Multiplatform 1.12.1 의 LocalClipboard 는 공통 코드에서 텍스트 ClipEntry 를 만들 수 없다.
    // 개발자 도구(DevToolsScreen)·터미널 선택 복사와 같은 사정이라, ClipEntry 에 공통 팩토리가 생기면 함께 옮긴다.
    @Suppress("DEPRECATION")
    val clipboard = LocalClipboardManager.current
    val spacing = JarvisTheme.dimens.spacing

    JarvisCard(modifier = Modifier.fillMaxWidth()) {
        when (result) {
            is QrCodeResult.Ready -> Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(spacing.s),
            ) {
                QrCodeImage(
                    matrix = result.code.matrix,
                    modifier = Modifier.widthIn(max = QrCodeScreenDefaults.maxImageSize).fillMaxWidth().testTag(QrCodeImageTestTag),
                )

                Text(
                    text = "버전 ${result.code.version} · ${result.code.matrix.size}×${result.code.matrix.size} 모듈 · 오류 정정 ${result.code.errorCorrection.name}",
                    style = JarvisTheme.typography.bodySmall,
                    color = JarvisTheme.colorScheme.onSurfaceVariant,
                )

                SelectionContainer(modifier = Modifier.fillMaxWidth()) {
                    Text(text = result.payload, style = JarvisTheme.codeTextStyle, modifier = Modifier.testTag(QrCodePayloadTestTag))
                }

                OutlinedButton(
                    onClick = { runCatching { clipboard.setText(AnnotatedString(result.payload)) } },
                    modifier = Modifier.testTag(QrCodeCopyTestTag),
                ) {
                    Icon(imageVector = JarvisIcons.Copy, contentDescription = null, modifier = Modifier.size(JarvisTheme.dimens.iconSize.small))
                    Text(text = "텍스트 복사", modifier = Modifier.padding(start = spacing.s))
                }
            }

            else -> Text(
                text = result.message.orEmpty(),
                style = JarvisTheme.typography.bodyMedium,
                color = if (result is QrCodeResult.TooLong) JarvisTheme.colorScheme.error else JarvisTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.testTag(QrCodeMessageTestTag),
            )
        }
    }
}

internal object QrCodeScreenDefaults {
    val maxImageSize: Dp
        @Composable @ReadOnlyComposable get() = JarvisTheme.dimens.layout.gridMinCellWidth * 2
}
