package io.github.taetae98coding.jarvis.ui.battery

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.taetae98coding.jarvis.designsystem.component.JarvisCard
import io.github.taetae98coding.jarvis.designsystem.component.JarvisCardHeader
import io.github.taetae98coding.jarvis.designsystem.component.JarvisLabeledValue
import io.github.taetae98coding.jarvis.designsystem.icon.JarvisIcons
import io.github.taetae98coding.jarvis.designsystem.theme.JarvisTheme
import io.github.taetae98coding.jarvis.domain.battery.Battery
import io.github.taetae98coding.jarvis.domain.battery.BatteryStatus
import org.koin.compose.viewmodel.koinViewModel

const val BatteryTestTag = "feature:battery"
const val BatteryLevelTestTag = "$BatteryTestTag:level"
const val BatteryChargingTestTag = "$BatteryTestTag:charging"
const val BatteryPowerSourceTestTag = "$BatteryTestTag:powerSource"
const val BatteryTemperatureTestTag = "$BatteryTestTag:temperature"
const val BatteryHealthTestTag = "$BatteryTestTag:health"
const val BatteryLowPowerModeTestTag = "$BatteryTestTag:lowPowerMode"
const val BatteryStatusTestTag = "$BatteryTestTag:status"

const val BatteryLoading = "확인 중"
const val BatteryNone = "배터리 없음"
const val BatteryUnavailable = "측정 불가"

@Composable
fun BatteryCard(modifier: Modifier = Modifier) {
    val viewModel = koinViewModel<BatteryViewModel>()
    val battery by viewModel.battery.collectAsStateWithLifecycle()

    BatteryCard(status = battery, modifier = modifier.testTag(BatteryTestTag))
}

@Composable
internal fun BatteryCard(
    status: BatteryStatus,
    modifier: Modifier = Modifier,
) {
    JarvisCard(modifier = modifier) {
        JarvisCardHeader(title = BatteryTitle, icon = JarvisIcons.Battery)

        when (status) {
            BatteryStatus.Loading -> StatusRow(BatteryLoading)
            BatteryStatus.NoBattery -> StatusRow(BatteryNone)
            BatteryStatus.Unavailable -> StatusRow(BatteryUnavailable)
            is BatteryStatus.Available -> BatteryRows(status.battery)
        }
    }
}

@Composable
private fun StatusRow(text: String) {
    InfoRow(BatteryStatusTestTag, "상태", text)
}

@Composable
private fun BatteryRows(battery: Battery) {
    Column(
        modifier = Modifier
            .testTag(BatteryLevelTestTag)
            .semantics(mergeDescendants = true) {},
        verticalArrangement = Arrangement.spacedBy(JarvisTheme.dimens.spacing.xs),
    ) {
        JarvisLabeledValue(label = "잔량", value = formatLevel(battery.levelPercent))

        LinearProgressIndicator(
            progress = { battery.levelPercent.coerceIn(0, 100) / 100f },
            modifier = Modifier.fillMaxWidth(),
            color = if (battery.isLow) JarvisTheme.colorScheme.error else ProgressIndicatorDefaults.linearColor,
        )
    }

    InfoRow(BatteryChargingTestTag, "상태", battery.charging.label)
    battery.powerSource?.let { InfoRow(BatteryPowerSourceTestTag, "전원", it.label) }
    battery.temperatureCelsius?.let { InfoRow(BatteryTemperatureTestTag, "온도", formatCelsius(it)) }
    battery.health?.let { InfoRow(BatteryHealthTestTag, "건강", it.label) }
    battery.lowPowerMode?.let { InfoRow(BatteryLowPowerModeTestTag, "저전력 모드", formatOnOff(it)) }
}

@Composable
private fun InfoRow(tag: String, label: String, value: String) {
    JarvisLabeledValue(
        label = label,
        value = value,
        modifier = Modifier
            .testTag(tag)
            .semantics(mergeDescendants = true) {},
    )
}
