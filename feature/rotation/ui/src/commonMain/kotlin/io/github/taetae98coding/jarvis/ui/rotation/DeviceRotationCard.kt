package io.github.taetae98coding.jarvis.ui.rotation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import io.github.taetae98coding.jarvis.domain.rotation.DeviceRotationStatus
import io.github.taetae98coding.jarvis.domain.rotation.RotationAngle
import org.koin.compose.viewmodel.koinViewModel

const val DeviceRotationTestTag = "feature:deviceRotation"
const val DeviceRotationLockTestTag = "deviceRotation:lock"
const val DeviceRotationBackwardTestTag = "deviceRotation:rotateBackward"
const val DeviceRotationForwardTestTag = "deviceRotation:rotateForward"

fun deviceRotationAngleTestTag(angle: RotationAngle): String =
    "deviceRotation:angle:${angle.degrees}"

@Composable
fun DeviceRotationCard(modifier: Modifier = Modifier) {
    val viewModel = koinViewModel<DeviceRotationViewModel>()
    val status by viewModel.deviceRotation.collectAsState()

    DeviceRotationCard(
        status = status,
        onAngleClick = viewModel::onDeviceRotationAngleClick,
        onRotate = viewModel::onDeviceRotate,
        onLockedChange = viewModel::onDeviceRotationLockChange,
        modifier = modifier.testTag(DeviceRotationTestTag),
    )
}

@Composable
internal fun DeviceRotationCard(
    status: DeviceRotationStatus,
    onAngleClick: (RotationAngle) -> Unit,
    onRotate: (Int) -> Unit,
    onLockedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "화면 회전",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                // 카드 전체를 toggleable 로 만들지 않는다. 각도 버튼도 눌러야 해서 카드가 통째로
                // 스위치 역할을 하면 버튼의 클릭이 시맨틱에 묻힌다.
                Switch(
                    checked = status.locked,
                    onCheckedChange = onLockedChange,
                    modifier = Modifier.testTag(DeviceRotationLockTestTag),
                    enabled = status.supported,
                )
            }

            Text(
                text = status.describe(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            // 카드 최소 너비가 220dp 라 칩 네 개가 한 줄에 들어가지 않는다.
            AngleRow(RotationAngle.Degrees0, RotationAngle.Degrees90, status, onAngleClick)
            AngleRow(RotationAngle.Degrees180, RotationAngle.Degrees270, status, onAngleClick)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    onClick = { onRotate(-1) },
                    modifier = Modifier.weight(1f).testTag(DeviceRotationBackwardTestTag),
                    enabled = status.supported,
                ) {
                    Text(text = "-90°")
                }

                OutlinedButton(
                    onClick = { onRotate(1) },
                    modifier = Modifier.weight(1f).testTag(DeviceRotationForwardTestTag),
                    enabled = status.supported,
                ) {
                    Text(text = "+90°")
                }
            }
        }
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
        horizontalArrangement = Arrangement.spacedBy(8.dp),
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
