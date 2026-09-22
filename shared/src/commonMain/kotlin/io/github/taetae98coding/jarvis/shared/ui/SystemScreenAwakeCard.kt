package io.github.taetae98coding.jarvis.shared.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.taetae98coding.jarvis.shared.platform.SystemScreenAwakeState
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

@Composable
internal fun SystemScreenAwakeCard(
    state: SystemScreenAwakeState,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    ToggleFeatureCard(
        title = "화면 꺼짐 방지 (시스템 전역)",
        description = state.describe(),
        checked = checked,
        onCheckedChange = { value ->
            onCheckedChange(value)

            // 권한이 없어도 설정값은 켜 둔다. 사용자가 설정 화면에서 허용하고 돌아오면 그때 효과가
            // 걸리고, 여기서 토글을 되돌리면 왜 꺼졌는지 알 수 없다.
            if (value && !state.permitted) state.requestPermission()
        },
        modifier = modifier,
        enabled = state.supported,
    )
}

private fun SystemScreenAwakeState.describe(): String =
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
