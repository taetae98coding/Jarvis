package io.github.taetae98coding.jarvis.ui.emulator

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import io.github.taetae98coding.jarvis.designsystem.component.JarvisCard
import io.github.taetae98coding.jarvis.designsystem.component.JarvisIconButton
import io.github.taetae98coding.jarvis.designsystem.component.JarvisIconButtonDefaults
import io.github.taetae98coding.jarvis.designsystem.component.JarvisTopBar
import io.github.taetae98coding.jarvis.designsystem.icon.JarvisIcons
import io.github.taetae98coding.jarvis.designsystem.theme.JarvisTheme
import io.github.taetae98coding.jarvis.domain.emulator.EmulatorDevice
import io.github.taetae98coding.jarvis.domain.emulator.EmulatorPlatform

const val EmulatorListTestTag = "emulator:list"

fun emulatorDeviceTestTag(id: String): String = "emulator:device:$id"

fun emulatorLaunchTestTag(id: String): String = "emulator:launch:$id"

fun emulatorWakeTestTag(id: String): String = "emulator:wake:$id"

@Composable
internal fun EmulatorListScreen(
    viewModel: EmulatorDevicesViewModel,
    onSelect: (EmulatorDevice) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val devices by viewModel.devices.collectAsState()
    val launchingIds by viewModel.launchingDevices.collectAsState()

    Column(
        modifier = modifier.fillMaxSize().testTag(EmulatorListTestTag),
        verticalArrangement = Arrangement.spacedBy(JarvisTheme.dimens.spacing.m),
    ) {
        JarvisTopBar(title = "기기 목록", onBack = onBack)

        if (devices.isEmpty()) {
            // 빈 화면만 남으면 기기가 없는 것인지 물어볼 곳이 없는 것인지 가릴 수 없다.
            Text(
                text = "연결된 기기가 없거나 개발자 머신에 물어볼 수 없습니다.",
                style = JarvisTheme.typography.bodyMedium,
                color = JarvisTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(JarvisTheme.dimens.spacing.s)) {
                items(devices, key = EmulatorDevice::id) { device ->
                    DeviceRow(
                        device = device,
                        isLaunching = device.id in launchingIds,
                        onClick = { onSelect(device) },
                        onLaunch = { viewModel.onLaunch(device) },
                        onWake = { viewModel.onWake(device) },
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
    JarvisCard(
        onClick = onClick,
        // 화면을 찍을 수 없는 기기는 눌러도 빈 화면만 나온다. 눌러도 되는 것처럼 보이지 않게
        // 카드째로 잠근다. 안의 실행 버튼은 그와 별개로 눌린다.
        enabled = device.canStream,
        modifier = Modifier.fillMaxWidth().testTag(emulatorDeviceTestTag(device.id)),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(JarvisTheme.dimens.spacing.m),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // 상태는 옆 설명 문구가 글자로 말한다. 아이콘 색은 그것을 한눈에 보이게 할 뿐이다.
            Icon(
                imageVector = JarvisIcons.Smartphone,
                contentDescription = null,
                tint = EmulatorDeviceDefaults.statusColor(device),
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(JarvisTheme.dimens.spacing.xs),
            ) {
                Text(
                    text = device.name,
                    style = JarvisTheme.typography.titleMedium,
                )

                Text(
                    text = device.describe(),
                    style = JarvisTheme.typography.bodySmall,
                    color = JarvisTheme.colorScheme.onSurfaceVariant,
                )
            }

            // 켤 것이 있는 기기는 아직 꺼져 있어서 입력을 받지 못한다. 둘이 한 줄에 같이 나오지 않는다.
            if (device.isAsleep && device.canControl) {
                JarvisIconButton(
                    icon = JarvisIcons.Sun,
                    contentDescription = "화면 켜기",
                    onClick = onWake,
                    modifier = Modifier.testTag(emulatorWakeTestTag(device.id)),
                )
            }

            if (device.canLaunch) {
                LaunchButton(
                    isLaunching = isLaunching,
                    onClick = onLaunch,
                    modifier = Modifier.testTag(emulatorLaunchTestTag(device.id)),
                )
            }
        }
    }
}

@Composable
private fun LaunchButton(
    isLaunching: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!isLaunching) {
        JarvisIconButton(
            icon = JarvisIcons.Play,
            contentDescription = "실행",
            onClick = onClick,
            modifier = modifier,
        )
        return
    }

    // 같은 기기에 요청을 두 번 보내면 두 번째는 "이미 실행 중" 으로 실패한다. 켜지는 동안은 잠근다.
    IconButton(
        onClick = onClick,
        modifier = modifier,
        enabled = false,
        colors = JarvisIconButtonDefaults.colors(),
    ) {
        CircularProgressIndicator(
            modifier = Modifier
                .size(EmulatorDeviceDefaults.progressSize)
                .semantics { contentDescription = "켜는 중…" },
            strokeWidth = EmulatorDeviceDefaults.progressStrokeWidth,
        )
    }
}

internal object EmulatorDeviceDefaults {
    val progressSize: Dp
        @Composable @ReadOnlyComposable get() = JarvisTheme.dimens.iconSize.small

    val progressStrokeWidth: Dp
        @Composable @ReadOnlyComposable get() = JarvisTheme.dimens.spacing.xxs

    @Composable
    @ReadOnlyComposable
    fun statusColor(device: EmulatorDevice): Color =
        when {
            !device.isRunning -> JarvisTheme.colorScheme.onSurfaceVariant
            device.isAsleep -> JarvisTheme.colors.warning
            else -> JarvisTheme.colors.success
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
