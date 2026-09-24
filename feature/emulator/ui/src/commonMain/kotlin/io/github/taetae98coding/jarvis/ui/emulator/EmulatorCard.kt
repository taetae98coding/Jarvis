package io.github.taetae98coding.jarvis.ui.emulator

import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import io.github.taetae98coding.jarvis.designsystem.component.JarvisCard
import io.github.taetae98coding.jarvis.designsystem.component.JarvisCardHeader
import io.github.taetae98coding.jarvis.designsystem.component.JarvisLabeledValue
import io.github.taetae98coding.jarvis.designsystem.icon.JarvisIcons
import io.github.taetae98coding.jarvis.designsystem.theme.JarvisTheme
import io.github.taetae98coding.jarvis.domain.emulator.EmulatorStatus
import io.github.taetae98coding.jarvis.domain.emulator.EmulatorSummary
import io.github.taetae98coding.jarvis.ui.navigation.LocalNavigator
import org.koin.compose.viewmodel.koinViewModel

const val EmulatorTestTag = "feature:emulator"

@Composable
fun EmulatorCard(modifier: Modifier = Modifier) {
    val viewModel = koinViewModel<EmulatorStatusViewModel>()
    val status by viewModel.status.collectAsState()
    val navigator = LocalNavigator.current

    EmulatorCard(
        status = status,
        onClick = { navigator.goTo(EmulatorRoute.Devices) },
        modifier = modifier.testTag(EmulatorTestTag),
    )
}

@Composable
internal fun EmulatorCard(
    status: EmulatorStatus?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    JarvisCard(onClick = onClick, modifier = modifier) {
        JarvisCardHeader(
            title = "에뮬레이터",
            icon = JarvisIcons.Smartphone,
            trailing = { Icon(imageVector = JarvisIcons.ChevronRight, contentDescription = null) },
        )

        Text(
            text = "개발자 머신의 Android 에뮬레이터·iOS 시뮬레이터와 연결된 실물 기기. 눌러서 " +
                "목록을 보고, 화면을 조작하거나 꺼진 에뮬레이터를 켤 수 있다.",
            style = JarvisTheme.typography.bodySmall,
            color = JarvisTheme.colorScheme.onSurfaceVariant,
        )

        JarvisLabeledValue(label = "Android", value = status.describe(EmulatorStatus::android))
        JarvisLabeledValue(label = "iOS", value = status.describe(EmulatorStatus::ios))
    }
}

// 아직 세는 중 / 셀 방법이 없음 / 개수 세 가지를 구분한다. 뒤의 둘이 같아 보이면 SDK 도구를 못 찾은
// 것과 에뮬레이터가 없는 것을 읽는 사람이 가를 수 없다.
private fun EmulatorStatus?.describe(select: (EmulatorStatus) -> EmulatorSummary?): String {
    if (this == null) return "확인 중…"

    val summary = select(this) ?: return "셀 수 없음"

    return "실행 중 ${summary.running}개 / 전체 ${summary.total}개"
}
