package io.github.taetae98coding.jarvis.ui.emulator

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import io.github.taetae98coding.jarvis.designsystem.icon.JarvisIcons
import io.github.taetae98coding.jarvis.ui.home.HomeFeature
import org.koin.compose.viewmodel.koinViewModel

const val EmulatorTitle = "에뮬레이터"

/** 타일은 카드가 눌렸을 때와 같은 기기 목록을 연다. 기능 화면을 따로 두지 않는다. */
object EmulatorHomeFeature : HomeFeature {
    override val id: String = "emulator"
    override val title: String = EmulatorTitle
    override val icon: ImageVector = JarvisIcons.Smartphone
    override val route: NavKey = EmulatorRoute.Devices

    // 셀 수 있는 플랫폼이 하나라도 있으면 지원이다. 아직 답이 없을 때(null)는 지원으로 두어 답이 온 뒤에야
    // 순서가 바뀐다. 처음부터 뒤로 보냈다가 앞으로 당기면 두 번 움직인다.
    @Composable
    override fun isSupported(): Boolean {
        val viewModel = koinViewModel<EmulatorStatusViewModel>()
        val status by viewModel.status.collectAsStateWithLifecycle()

        return status?.let { it.android != null || it.ios != null } ?: true
    }

    @Composable
    override fun HomeCard(modifier: Modifier) {
        EmulatorCard(modifier = modifier)
    }
}
