package io.github.taetae98coding.jarvis.ui.rotation

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import io.github.taetae98coding.jarvis.designsystem.icon.JarvisIcons
import io.github.taetae98coding.jarvis.ui.home.HomeFeature
import io.github.taetae98coding.jarvis.ui.home.HomeFeatureScreen
import org.koin.compose.viewmodel.koinViewModel

const val DeviceRotationTitle = "화면 회전"

/** 지원 여부는 플랫폼 상태가 정한다(Android 만 true, docs/common/device-rotation.html#platforms). */
object DeviceRotationHomeFeature : HomeFeature {
    override val id: String = "deviceRotation"
    override val title: String = DeviceRotationTitle
    override val icon: ImageVector = JarvisIcons.RotateRight
    override val route: NavKey = DeviceRotationRoute

    @Composable
    override fun isSupported(): Boolean {
        val viewModel = koinViewModel<DeviceRotationViewModel>()
        val status by viewModel.deviceRotation.collectAsStateWithLifecycle()

        return status.supported
    }

    @Composable
    override fun HomeCard(modifier: Modifier) {
        DeviceRotationCard(modifier = modifier)
    }
}

@Composable
internal fun DeviceRotationScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    HomeFeatureScreen(title = DeviceRotationTitle, onBack = onBack, modifier = modifier) {
        DeviceRotationCard(modifier = Modifier.fillMaxWidth())
    }
}
