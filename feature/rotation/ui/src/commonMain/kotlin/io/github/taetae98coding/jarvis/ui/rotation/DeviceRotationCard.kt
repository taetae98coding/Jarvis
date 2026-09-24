package io.github.taetae98coding.jarvis.ui.rotation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.taetae98coding.jarvis.designsystem.component.JarvisCard
import io.github.taetae98coding.jarvis.designsystem.component.JarvisCardHeader
import io.github.taetae98coding.jarvis.designsystem.component.JarvisSwitchRow
import io.github.taetae98coding.jarvis.designsystem.icon.JarvisIcons
import io.github.taetae98coding.jarvis.designsystem.theme.JarvisTheme
import io.github.taetae98coding.jarvis.domain.rotation.DeviceRotationNotificationStatus
import io.github.taetae98coding.jarvis.domain.rotation.DeviceRotationStatus
import io.github.taetae98coding.jarvis.domain.rotation.RotationAngle
import org.koin.compose.viewmodel.koinViewModel

const val DeviceRotationTestTag = "feature:deviceRotation"
const val DeviceRotationLockTestTag = "deviceRotation:lock"
const val DeviceRotationBackwardTestTag = "deviceRotation:rotateBackward"
const val DeviceRotationForwardTestTag = "deviceRotation:rotateForward"
const val DeviceRotationNotificationTestTag = "deviceRotation:notification"

fun deviceRotationAngleTestTag(angle: RotationAngle): String =
    "deviceRotation:angle:${angle.degrees}"

@Composable
fun DeviceRotationCard(modifier: Modifier = Modifier) {
    val viewModel = koinViewModel<DeviceRotationViewModel>()
    val status by viewModel.deviceRotation.collectAsStateWithLifecycle()
    val notification by viewModel.notification.collectAsStateWithLifecycle()

    DeviceRotationCard(
        status = status,
        notification = notification,
        onAngleClick = viewModel::onDeviceRotationAngleClick,
        onRotate = viewModel::onDeviceRotate,
        onLockedChange = viewModel::onDeviceRotationLockChange,
        onNotificationPinnedChange = viewModel::onNotificationPinnedChange,
        modifier = modifier.testTag(DeviceRotationTestTag),
    )
}

@Composable
internal fun DeviceRotationCard(
    status: DeviceRotationStatus,
    notification: DeviceRotationNotificationStatus,
    onAngleClick: (RotationAngle) -> Unit,
    onRotate: (Int) -> Unit,
    onLockedChange: (Boolean) -> Unit,
    onNotificationPinnedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    JarvisCard(modifier = modifier) {
        JarvisCardHeader(
            title = "화면 회전",
            icon = JarvisIcons.RotateRight,
            enabled = status.supported,
            trailing = {
                // 카드 전체를 toggleable 로 만들지 않는다. 각도 버튼도 눌러야 해서 카드가 통째로
                // 스위치 역할을 하면 버튼의 클릭이 시맨틱에 묻힌다.
                Switch(
                    checked = status.locked,
                    onCheckedChange = onLockedChange,
                    modifier = Modifier.testTag(DeviceRotationLockTestTag),
                    enabled = status.supported,
                )
            },
        )

        Text(
            text = status.describe(),
            style = JarvisTheme.typography.bodySmall,
            color = JarvisTheme.colorScheme.onSurfaceVariant,
        )

        // 카드 최소 너비가 220dp 라 칩 네 개가 한 줄에 들어가지 않는다.
        AngleRow(RotationAngle.Degrees0, RotationAngle.Degrees90, status, onAngleClick)
        AngleRow(RotationAngle.Degrees180, RotationAngle.Degrees270, status, onAngleClick)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(JarvisTheme.dimens.spacing.s),
        ) {
            RotateButton(
                icon = JarvisIcons.RotateLeft,
                label = "-90°",
                onClick = { onRotate(-1) },
                enabled = status.supported,
                modifier = Modifier.weight(1f).testTag(DeviceRotationBackwardTestTag),
            )

            RotateButton(
                icon = JarvisIcons.RotateRight,
                label = "+90°",
                onClick = { onRotate(1) },
                enabled = status.supported,
                modifier = Modifier.weight(1f).testTag(DeviceRotationForwardTestTag),
            )
        }

        // 알림을 만들 수 있는 플랫폼에서만 행이 있다. 문구는 docs/common/notification-widget.html#behavior 가 기준이다.
        if (notification.supported) {
            JarvisSwitchRow(
                title = "알림에 고정",
                checked = notification.pinned,
                onCheckedChange = onNotificationPinnedChange,
                supporting = notification.describe(),
                switchModifier = Modifier.testTag(DeviceRotationNotificationTestTag),
            )
        }
    }
}

private fun DeviceRotationNotificationStatus.describe(): String? =
    if (pinned && !permitted) "알림 권한이 없어 표시되지 않습니다. 허용하면 바로 나타납니다." else null

@Composable
private fun RotateButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
    ) {
        // 글자가 같은 것을 말하므로 아이콘은 읽지 않는다.
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(JarvisTheme.dimens.iconSize.small),
        )
        Spacer(modifier = Modifier.width(ButtonDefaults.IconSpacing))
        Text(text = label)
    }
}

@Composable
private fun AngleRow(
    first: RotationAngle,
    second: RotationAngle,
    status: DeviceRotationStatus,
    onAngleClick: (RotationAngle) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(JarvisTheme.dimens.spacing.s),
    ) {
        listOf(first, second).forEach { angle ->
            FilterChip(
                selected = status.angle == angle,
                onClick = { onAngleClick(angle) },
                label = { Text(text = "${angle.degrees}°") },
                modifier = Modifier.weight(1f).testTag(deviceRotationAngleTestTag(angle)),
                enabled = status.supported,
            )
        }
    }
}

private fun DeviceRotationStatus.describe(): String {
    val degrees = angle?.degrees

    return when {
        !supported -> "이 플랫폼에서는 화면을 돌릴 수 없습니다."
        !permitted -> "시스템 설정 변경 권한이 필요합니다. 누르면 권한 설정 화면이 열립니다."
        degrees == null -> "현재 각도를 읽을 수 없습니다."
        locked -> "현재 $degrees°에 고정되어 있습니다."
        else -> "현재 $degrees°, 센서를 따라 회전합니다."
    }
}
