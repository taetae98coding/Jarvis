package io.github.taetae98coding.jarvis.ui.emulator

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.taetae98coding.jarvis.designsystem.component.JarvisCard
import io.github.taetae98coding.jarvis.designsystem.component.JarvisTopBar
import io.github.taetae98coding.jarvis.designsystem.theme.JarvisTheme
import io.github.taetae98coding.jarvis.domain.emulator.PairingResult
import io.github.taetae98coding.jarvis.domain.emulator.PairingService
import io.github.taetae98coding.jarvis.domain.emulator.QrPairingState

const val WifiPairingTestTag = "emulator:pairing"
const val WifiPairingQrTabTestTag = "emulator:pairing:tab:qr"
const val WifiPairingCodeTabTestTag = "emulator:pairing:tab:code"
const val WifiPairingQrTestTag = "emulator:pairing:qr"
const val WifiPairingStatusTestTag = "emulator:pairing:status"
const val WifiPairingIosTestTag = "emulator:pairing:ios"

fun wifiPairingServiceTestTag(name: String): String = "emulator:pairing:service:$name"

fun wifiPairingCodeTestTag(name: String): String = "emulator:pairing:code:$name"

fun wifiPairingPairTestTag(name: String): String = "emulator:pairing:pair:$name"

@Composable
internal fun WifiPairingScreen(
    viewModel: WifiPairingViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tab by viewModel.tab.collectAsStateWithLifecycle()

    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(JarvisTheme.dimens.spacing.m),
    ) {
        JarvisTopBar(title = "Wi-Fi 로 기기 페어링", onBack = onBack)

        PrimaryTabRow(selectedTabIndex = tab.ordinal) {
            Tab(
                selected = tab == WifiPairingTab.QrCode,
                onClick = { viewModel.onSelectTab(WifiPairingTab.QrCode) },
                text = { Text("QR 코드") },
                modifier = Modifier.testTag(WifiPairingQrTabTestTag),
            )
            Tab(
                selected = tab == WifiPairingTab.PairingCode,
                onClick = { viewModel.onSelectTab(WifiPairingTab.PairingCode) },
                text = { Text("페어링 코드") },
                modifier = Modifier.testTag(WifiPairingCodeTabTestTag),
            )
        }

        when (tab) {
            WifiPairingTab.QrCode -> QrCodeTab(viewModel)
            WifiPairingTab.PairingCode -> PairingCodeTab(viewModel)
        }

        // iPhone·iPad 는 공개 도구로 무선 첫 페어링을 할 수 없다. docs/common/wireless-pairing.html#research
        Text(
            text = "iPhone·iPad 는 처음 한 번 케이블로 연결해 '이 컴퓨터 신뢰' 를 눌러야 합니다. " +
                "그 뒤로는 같은 네트워크에서 케이블 없이 기기 목록에 나타납니다.",
            style = JarvisTheme.typography.bodySmall,
            color = JarvisTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.testTag(WifiPairingIosTestTag),
        )
    }
}

@Composable
private fun ColumnScope.QrCodeTab(viewModel: WifiPairingViewModel) {
    val qrCode by viewModel.qrCode.collectAsStateWithLifecycle()
    val state by viewModel.qrState.collectAsStateWithLifecycle()

    Guide("기기에서 설정 › 개발자 옵션 › 무선 디버깅 › QR 코드로 기기 페어링을 열고 이 코드를 스캔하세요.")

    QrCodeImage(
        text = qrCode.payload,
        contentDescription = "페어링 QR 코드",
        modifier = Modifier.align(Alignment.CenterHorizontally).testTag(WifiPairingQrTestTag),
    )

    Text(
        text = state.describe(),
        style = JarvisTheme.typography.bodyMedium,
        modifier = Modifier.testTag(WifiPairingStatusTestTag),
    )

    if (state is QrPairingState.Finished) {
        OutlinedButton(onClick = viewModel::onNewQrCode) {
            Text("새 QR 코드")
        }
    }
}

@Composable
private fun PairingCodeTab(viewModel: WifiPairingViewModel) {
    val services by viewModel.services.collectAsStateWithLifecycle()
    val codePairing by viewModel.codePairing.collectAsStateWithLifecycle()

    Guide("기기에서 무선 디버깅 › 페어링 코드로 기기 페어링을 열면 여기에 나타납니다.")

    codePairing.lastResult?.let { last ->
        Text(
            text = "${last.service.name}: ${last.result.describe()}",
            style = JarvisTheme.typography.bodyMedium,
            modifier = Modifier.testTag(WifiPairingStatusTestTag),
        )
    }

    val waiting = services

    when {
        waiting == null -> Guide(UnavailableMessage)
        waiting.isEmpty() -> Guide("페어링을 기다리는 기기가 없습니다.")
        else -> waiting.forEach { service ->
            PairingServiceRow(
                service = service,
                code = codePairing.codeOf(service),
                isPairing = codePairing.isPairing(service),
                canPair = codePairing.canPair(service),
                onCodeChange = { viewModel.onCodeChange(service, it) },
                onPair = { viewModel.onPair(service) },
            )
        }
    }
}

@Composable
private fun PairingServiceRow(
    service: PairingService,
    code: String,
    isPairing: Boolean,
    canPair: Boolean,
    onCodeChange: (String) -> Unit,
    onPair: () -> Unit,
) {
    JarvisCard(modifier = Modifier.fillMaxWidth().testTag(wifiPairingServiceTestTag(service.name))) {
        Text(text = service.name, style = JarvisTheme.typography.titleMedium)

        Text(
            text = service.address,
            style = JarvisTheme.typography.bodySmall,
            color = JarvisTheme.colorScheme.onSurfaceVariant,
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(JarvisTheme.dimens.spacing.s),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = code,
                onValueChange = onCodeChange,
                label = { Text("페어링 코드") },
                singleLine = true,
                enabled = !isPairing,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                modifier = Modifier.weight(1f).testTag(wifiPairingCodeTestTag(service.name)),
            )

            TextButton(
                onClick = onPair,
                enabled = canPair,
                modifier = Modifier.testTag(wifiPairingPairTestTag(service.name)),
            ) {
                Text(if (isPairing) "페어링하는 중…" else "페어링")
            }
        }
    }
}

@Composable
private fun Guide(text: String) {
    Text(
        text = text,
        style = JarvisTheme.typography.bodyMedium,
        color = JarvisTheme.colorScheme.onSurfaceVariant,
    )
}

private const val UnavailableMessage = "개발자 머신에서 페어링 대기 기기를 찾을 수 없습니다."

private fun QrPairingState.describe(): String =
    when (this) {
        QrPairingState.Unavailable -> UnavailableMessage
        QrPairingState.Waiting -> "스캔을 기다리는 중…"
        is QrPairingState.Pairing -> "${service.address} 와 페어링하는 중…"
        is QrPairingState.Finished -> result.describe()
    }

private fun PairingResult.describe(): String =
    when (this) {
        is PairingResult.Paired ->
            if (isConnected) "페어링했습니다. 기기 목록에 곧 나타납니다." else "페어링했지만 아직 연결되지 않았습니다."

        is PairingResult.Failed -> "페어링하지 못했습니다: $reason"
    }
