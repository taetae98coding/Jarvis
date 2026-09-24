package io.github.taetae98coding.jarvis.ui.screen

import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.taetae98coding.jarvis.designsystem.component.JarvisCard
import io.github.taetae98coding.jarvis.designsystem.component.JarvisCardHeader
import io.github.taetae98coding.jarvis.designsystem.component.JarvisSwitchRow
import io.github.taetae98coding.jarvis.designsystem.icon.JarvisIcons
import io.github.taetae98coding.jarvis.designsystem.theme.JarvisTheme
import io.github.taetae98coding.jarvis.domain.screen.SystemScreenAwakeNotificationStatus
import io.github.taetae98coding.jarvis.domain.screen.SystemScreenAwakeStatus
import org.koin.compose.viewmodel.koinViewModel
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

const val KeepSystemScreenAwakeTestTag = "feature:keepSystemScreenAwake"
const val KeepSystemScreenAwakeToggleTestTag = "keepSystemScreenAwake:toggle"
const val KeepSystemScreenAwakeNotificationTestTag = "keepSystemScreenAwake:notification"

@Composable
fun SystemScreenAwakeCard(modifier: Modifier = Modifier) {
    val viewModel = koinViewModel<ScreenAwakeViewModel>()
    val status by viewModel.systemScreenAwake.collectAsStateWithLifecycle()
    val checked by viewModel.keepSystemScreenAwake.collectAsStateWithLifecycle()
    val notification by viewModel.systemScreenAwakeNotification.collectAsStateWithLifecycle()

    SystemScreenAwakeCard(
        status = status,
        checked = checked,
        notification = notification,
        onCheckedChange = viewModel::onKeepSystemScreenAwakeChange,
        onNotificationPinnedChange = viewModel::onSystemScreenAwakeNotificationPinnedChange,
        modifier = modifier.testTag(KeepSystemScreenAwakeTestTag),
    )
}

@Composable
internal fun SystemScreenAwakeCard(
    status: SystemScreenAwakeStatus,
    checked: Boolean,
    notification: SystemScreenAwakeNotificationStatus,
    onCheckedChange: (Boolean) -> Unit,
    onNotificationPinnedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    JarvisCard(modifier = modifier) {
        JarvisCardHeader(
            title = "화면 꺼짐 방지 (시스템 전역)",
            icon = JarvisIcons.Monitor,
            enabled = status.supported,
            trailing = {
                // 카드 전체를 toggleable 로 만들지 않는다(ToggleFeatureCard 를 쓰지 않는 이유). 아래 "알림에 고정"
                // 스위치가 함께 있어서 카드가 통째로 스위치면 두 토글의 시맨틱이 한 노드로 합쳐진다.
                // 권한이 없을 때 설정 화면을 여는 것은 SetKeepSystemScreenAwakeUseCase 가 판단한다.
                Switch(
                    checked = checked,
                    onCheckedChange = onCheckedChange,
                    modifier = Modifier.testTag(KeepSystemScreenAwakeToggleTestTag),
                    enabled = status.supported,
                )
            },
        )

        Text(
            text = status.describe(),
            style = JarvisTheme.typography.bodySmall,
            color = JarvisTheme.colorScheme.onSurfaceVariant,
        )

        // 알림을 만들 수 있는 플랫폼에서만 행이 있다. 문구는 docs/common/notification-widget.html#behavior 가 기준이다.
        if (notification.supported) {
            JarvisSwitchRow(
                title = "알림에 고정",
                checked = notification.pinned,
                onCheckedChange = onNotificationPinnedChange,
                supporting = notification.describe(),
                switchModifier = Modifier.testTag(KeepSystemScreenAwakeNotificationTestTag),
            )
        }
    }
}

private fun SystemScreenAwakeStatus.describe(): String =
    when {
        !supported -> "이 플랫폼에서는 앱이 없는 동안의 화면 꺼짐을 막을 수 없습니다."
        !permitted -> "시스템 설정 변경 권한이 필요합니다. 켜면 권한 설정 화면이 열립니다."
        else -> {
            val current = screenOffTimeout?.let { " 현재 시스템 설정은 ${it.describe()}입니다." } ?: ""

            "앱을 닫아도 화면이 꺼지지 않습니다.$current"
        }
    }

private fun SystemScreenAwakeNotificationStatus.describe(): String? =
    if (pinned && !permitted) "알림 권한이 없어 표시되지 않습니다. 허용하면 바로 나타납니다." else null

// 이 기능이 설정하는 값은 24일이 넘어서 초 단위까지 보여줄 이유가 없다. 큰 단위 하나로 줄인다.
private fun Duration.describe(): String =
    when {
        this >= 1.days -> "${inWholeDays}일"
        this >= 1.hours -> "${inWholeHours}시간"
        this >= 1.minutes -> "${inWholeMinutes}분"
        else -> "${inWholeSeconds}초"
    }
