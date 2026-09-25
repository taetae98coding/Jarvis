package io.github.taetae98coding.jarvis.ui.appinfo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.style.Style
import androidx.compose.foundation.style.contentPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import io.github.taetae98coding.jarvis.designsystem.component.JarvisCard
import io.github.taetae98coding.jarvis.designsystem.component.JarvisLabeledValue
import io.github.taetae98coding.jarvis.designsystem.theme.JarvisTheme
import io.github.taetae98coding.jarvis.designsystem.theme.jarvisDimens
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.taetae98coding.jarvis.domain.appinfo.AppInfo
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun AppInfoCard(modifier: Modifier = Modifier) {
    val viewModel = koinViewModel<AppInfoViewModel>()
    val update by viewModel.update.collectAsStateWithLifecycle()

    AppInfoCard(
        appInfo = viewModel.appInfo,
        update = update,
        onUpdateClick = viewModel::onUpdateClick,
        modifier = modifier,
    )
}

@Composable
internal fun AppInfoCard(
    appInfo: AppInfo,
    update: AppUpdateUiState,
    onUpdateClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    JarvisCard(
        modifier = modifier,
        style = AppInfoCardDefaults.style,
        verticalArrangement = AppInfoCardDefaults.contentArrangement,
    ) {
        Text(
            text = "Jarvis",
            style = AppInfoCardDefaults.titleStyle,
        )

        JarvisLabeledValue(label = "앱 버전", value = appInfo.version)
        JarvisLabeledValue(label = "플랫폼", value = appInfo.platform)
        // 플랫폼이 값을 주지 못하면(macOS 밖 데스크톱) 빈 줄을 그리지 않는다(docs/common/app-info.html R7).
        if (appInfo.deviceName.isNotBlank()) JarvisLabeledValue(label = DeviceNameLabel, value = appInfo.deviceName)
        if (appInfo.deviceId.isNotBlank()) JarvisLabeledValue(label = DeviceIdLabel, value = appInfo.deviceId)
        AppUpdateRow(state = update, onUpdateClick = onUpdateClick)
    }
}

const val DeviceNameLabel = "기기 이름"
const val DeviceIdLabel = "기기 식별자"

/** 홈 맨 위의 머리 카드라 기능 카드보다 여백과 제목이 한 단계 크다. */
internal object AppInfoCardDefaults {
    val style: Style = Style {
        contentPadding(jarvisDimens.spacing.xl)
    }

    val contentArrangement: Arrangement.Vertical
        @Composable @ReadOnlyComposable get() = Arrangement.spacedBy(JarvisTheme.dimens.spacing.m)

    val titleStyle: TextStyle
        @Composable @ReadOnlyComposable get() = JarvisTheme.typography.headlineSmall
}
