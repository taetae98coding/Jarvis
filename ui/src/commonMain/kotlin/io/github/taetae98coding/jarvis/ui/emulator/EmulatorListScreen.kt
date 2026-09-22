package io.github.taetae98coding.jarvis.ui.emulator

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import io.github.taetae98coding.jarvis.domain.emulator.EmulatorDevice
import io.github.taetae98coding.jarvis.domain.emulator.EmulatorPlatform

internal const val EmulatorListTestTag = "emulator:list"

internal fun emulatorDeviceTestTag(id: String): String = "emulator:device:$id"

internal fun emulatorLaunchTestTag(id: String): String = "emulator:launch:$id"

internal fun emulatorWakeTestTag(id: String): String = "emulator:wake:$id"

@Composable
internal fun EmulatorListScreen(
    devices: List<EmulatorDevice>,
    launchingIds: Set<String>,
    onSelect: (EmulatorDevice) -> Unit,
    onLaunch: (EmulatorDevice) -> Unit,
    onWake: (EmulatorDevice) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().testTag(EmulatorListTestTag),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        EmulatorTopBar(title = "기기 목록", onBack = onBack)

        if (devices.isEmpty()) {
            // 빈 화면만 남으면 기기가 없는 것인지 물어볼 곳이 없는 것인지 가릴 수 없다.
            Text(
                text = "연결된 기기가 없거나 개발자 머신에 물어볼 수 없습니다.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(devices, key = EmulatorDevice::id) { device ->
                    DeviceRow(
                        device = device,
                        isLaunching = device.id in launchingIds,
                        onClick = { onSelect(device) },
                        onLaunch = { onLaunch(device) },
                        onWake = { onWake(device) },
                    )
                }
            }
        }
    }
}

@Composable
private fun DeviceRow(
    device: EmulatorDevice,
    isLaunching: Boolean,
    onClick: () -> Unit,
    onLaunch: () -> Unit,
    onWake: () -> Unit,
) {
    Card(
        onClick = onClick,
        // 화면을 찍을 수 없는 기기는 눌러도 빈 화면만 나온다. 눌러도 되는 것처럼 보이지 않게
        // 카드째로 잠근다. 안의 실행 버튼은 그와 별개로 눌린다.
        enabled = device.canStream,
        modifier = Modifier.fillMaxWidth().testTag(emulatorDeviceTestTag(device.id)),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = device.name,
                    style = MaterialTheme.typography.titleMedium,
                )

                Text(
                    text = device.describe(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // 켤 것이 있는 기기는 아직 꺼져 있어서 입력을 받지 못한다. 둘이 한 줄에 같이 나오지 않는다.
            if (device.isAsleep && device.canControl) {
                TextButton(
                    onClick = onWake,
                    modifier = Modifier.testTag(emulatorWakeTestTag(device.id)),
                ) {
                    Text(text = "화면 켜기")
                }
            }

            if (device.canLaunch) {
                TextButton(
                    onClick = onLaunch,
                    // 같은 기기에 요청을 두 번 보내면 두 번째는 "이미 실행 중" 으로 실패한다.
                    enabled = !isLaunching,
                    modifier = Modifier.testTag(emulatorLaunchTestTag(device.id)),
                ) {
                    Text(text = if (isLaunching) "켜는 중…" else "실행")
                }
            }
        }
    }
}

// 가상 기기인지 실물인지, 지금 무슨 상태인지, 눌리지 않는다면 왜 그런지 순서로 잇는다.
private fun EmulatorDevice.describe(): String =
    listOfNotNull(
        platform.label,
        "실물 기기".takeIf { isPhysical },
        when {
            !isRunning -> "꺼짐"
            isPhysical -> "연결됨"
            else -> "실행 중"
        },
        "화면 꺼짐".takeIf { isAsleep },
        "화면을 볼 수 없음".takeIf { isRunning && !canStream },
    ).joinToString(" · ")

private val EmulatorPlatform.label: String
    get() = when (this) {
        EmulatorPlatform.ANDROID -> "Android"
        EmulatorPlatform.IOS -> "iOS"
    }
