package io.github.taetae98coding.jarvis.ui.battery

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import io.github.taetae98coding.jarvis.designsystem.icon.JarvisIcons
import io.github.taetae98coding.jarvis.domain.battery.BatteryStatus
import io.github.taetae98coding.jarvis.ui.home.HomeFeature
import io.github.taetae98coding.jarvis.ui.home.HomeFeatureScreen
import org.koin.compose.viewmodel.koinViewModel

const val BatteryTitle = "배터리"

/** 타일은 카드를 그대로 담은 기능 화면을 연다. */
object BatteryHomeFeature : HomeFeature {
    override val id: String = "battery"
    override val title: String = BatteryTitle
    override val icon: ImageVector = JarvisIcons.Battery
    override val route: NavKey = BatteryRoute

    // 기기에 배터리가 없거나(Mac mini 같은 데스크톱) 읽을 수 없으면(Battery Status API 가 없는 브라우저) 잠근다.
    // 아직 답이 없을 때(Loading)는 지원으로 두어 답이 온 뒤에야 순서가 바뀐다. 처음부터 뒤로 보냈다가 앞으로 당기면 두 번 움직인다.
    @Composable
    override fun isSupported(): Boolean {
        val viewModel = koinViewModel<BatteryViewModel>()
        val battery by viewModel.battery.collectAsStateWithLifecycle()

        return battery != BatteryStatus.NoBattery && battery != BatteryStatus.Unavailable
    }

    @Composable
    override fun HomeCard(modifier: Modifier) {
        BatteryCard(modifier = modifier)
    }
}

@Composable
internal fun BatteryScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    HomeFeatureScreen(title = BatteryTitle, onBack = onBack, modifier = modifier) {
        BatteryCard(modifier = Modifier.fillMaxWidth())
    }
}
