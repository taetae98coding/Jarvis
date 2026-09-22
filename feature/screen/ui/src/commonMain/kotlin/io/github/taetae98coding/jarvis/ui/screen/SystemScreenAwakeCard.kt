package io.github.taetae98coding.jarvis.ui.screen

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.taetae98coding.jarvis.domain.screen.SystemScreenAwakeStatus
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.testTag
import io.github.taetae98coding.jarvis.ui.component.ToggleFeatureCard
import org.koin.compose.viewmodel.koinViewModel
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

const val KeepSystemScreenAwakeTestTag = "feature:keepSystemScreenAwake"

@Composable
fun SystemScreenAwakeCard(modifier: Modifier = Modifier) {
    val viewModel = koinViewModel<ScreenAwakeViewModel>()
    val status by viewModel.systemScreenAwake.collectAsState()
    val checked by viewModel.keepSystemScreenAwake.collectAsState()

    SystemScreenAwakeCard(
        status = status,
        checked = checked,
        onCheckedChange = viewModel::onKeepSystemScreenAwakeChange,
        modifier = modifier.testTag(KeepSystemScreenAwakeTestTag),
    )
}

@Composable
internal fun SystemScreenAwakeCard(
    status: SystemScreenAwakeStatus,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    ToggleFeatureCard(
        title = "화면 꺼짐 방지 (시스템 전역)",
        description = status.describe(),
        checked = checked,
        // 권한이 없을 때 설정 화면을 여는 것은 SetKeepSystemScreenAwakeUseCase 가 판단한다.
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        enabled = status.supported,
    )
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

// 이 기능이 설정하는 값은 24일이 넘어서 초 단위까지 보여줄 이유가 없다. 큰 단위 하나로 줄인다.
private fun Duration.describe(): String =
    when {
        this >= 1.days -> "${inWholeDays}일"
        this >= 1.hours -> "${inWholeHours}시간"
        this >= 1.minutes -> "${inWholeMinutes}분"
        else -> "${inWholeSeconds}초"
    }
