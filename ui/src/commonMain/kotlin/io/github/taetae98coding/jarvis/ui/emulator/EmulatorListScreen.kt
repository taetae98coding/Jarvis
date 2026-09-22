package io.github.taetae98coding.jarvis.ui.emulator

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import io.github.taetae98coding.jarvis.domain.emulator.EmulatorDevice
import io.github.taetae98coding.jarvis.domain.emulator.EmulatorPlatform

internal const val EmulatorListTestTag = "emulator:list"

internal fun emulatorDeviceTestTag(id: String): String = "emulator:device:$id"

@Composable
internal fun EmulatorListScreen(
    devices: List<EmulatorDevice>,
    onSelect: (EmulatorDevice) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().testTag(EmulatorListTestTag),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        EmulatorTopBar(title = "에뮬레이터 목록", onBack = onBack)

        if (devices.isEmpty()) {
            // 빈 화면만 남으면 기기가 없는 것인지 물어볼 곳이 없는 것인지 가릴 수 없다.
            Text(
                text = "가상 기기가 없거나 개발자 머신에 물어볼 수 없습니다.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(devices, key = EmulatorDevice::id) { device ->
                    DeviceRow(device = device, onClick = { onSelect(device) })
                }
            }
        }
    }
}

@Composable
private fun DeviceRow(
    device: EmulatorDevice,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        // 꺼져 있는 기기는 찍을 화면이 없다. 눌러도 되는 것처럼 보이지 않게 카드째로 잠근다.
        enabled = device.isRunning,
        modifier = Modifier.fillMaxWidth().testTag(emulatorDeviceTestTag(device.id)),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = device.name,
                style = MaterialTheme.typography.titleMedium,
            )

            Text(
                text = "${device.platform.label} · ${if (device.isRunning) "실행 중" else "꺼짐"}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private val EmulatorPlatform.label: String
    get() = when (this) {
        EmulatorPlatform.ANDROID -> "Android"
        EmulatorPlatform.IOS -> "iOS"
    }
