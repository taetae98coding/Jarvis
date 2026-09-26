package io.github.taetae98coding.jarvis.ui.screen

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

const val ScreenAwakeTitle = "화면 꺼짐 방지"
const val SystemScreenAwakeTitle = "화면 꺼짐 방지 (시스템 전역)"

/** 앱이 떠 있는 동안의 화면 꺼짐 방지. 모든 플랫폼이 지원한다(docs/common/keep-screen-awake.html#platforms). */
object ScreenAwakeHomeFeature : HomeFeature {
    override val id: String = "keepScreenAwake"
    override val title: String = ScreenAwakeTitle
    override val icon: ImageVector = JarvisIcons.Eye
    override val route: NavKey = ScreenAwakeRoute.App

    @Composable
    override fun isSupported(): Boolean = true

    @Composable
    override fun HomeCard(modifier: Modifier) {
        ScreenAwakeCard(modifier = modifier)
    }
}

/** 앱이 없는 동안의 화면 꺼짐 방지. 지원 여부는 플랫폼 상태가 정한다(Android 만 true). */
object SystemScreenAwakeHomeFeature : HomeFeature {
    override val id: String = "keepSystemScreenAwake"
    override val title: String = SystemScreenAwakeTitle
    override val icon: ImageVector = JarvisIcons.Monitor
    override val route: NavKey = ScreenAwakeRoute.System

    @Composable
    override fun isSupported(): Boolean {
        val viewModel = koinViewModel<ScreenAwakeViewModel>()
        val status by viewModel.systemScreenAwake.collectAsStateWithLifecycle()

        return status.supported
    }

    @Composable
    override fun HomeCard(modifier: Modifier) {
        SystemScreenAwakeCard(modifier = modifier)
    }
}

@Composable
internal fun ScreenAwakeScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    HomeFeatureScreen(title = ScreenAwakeTitle, onBack = onBack, modifier = modifier) {
        ScreenAwakeCard(modifier = Modifier.fillMaxWidth())
    }
}

@Composable
internal fun SystemScreenAwakeScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    HomeFeatureScreen(title = SystemScreenAwakeTitle, onBack = onBack, modifier = modifier) {
        SystemScreenAwakeCard(modifier = Modifier.fillMaxWidth())
    }
}
