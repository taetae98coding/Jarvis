package io.github.taetae98coding.jarvis.ui.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation3.runtime.NavKey

/**
 * 기능 하나가 홈에 내놓는 것. 앱 셸의 `FeatureGrid` 는 이 인터페이스만 보고 카드를 놓거나 타일을 놓는다
 * (docs/common/home-adaptive-layout.html).
 *
 * 기능은 서로를 모르고 앱 셸은 기능의 ViewModel 을 모르므로, 지원 여부와 카드는 컴포저블로 내놓아 기능이
 * 자기 ViewModel 을 `koinViewModel()` 로 직접 받는다. [route] 는 타일이 여는 그 기능의 화면이다.
 */
@Stable
interface HomeFeature {
    /** 타일의 테스트 태그와 그리드 항목 키가 된다. 기능마다 달라야 한다. */
    val id: String

    /** 타일 라벨과 기능 화면의 제목. */
    val title: String

    val icon: ImageVector

    /** 타일을 누르면 여는 화면. 백스택에 저장되므로 그 기능의 `navKeySerializers` 에 등록되어 있어야 한다. */
    val route: NavKey

    /**
     * 이 플랫폼에서 지금 쓸 수 있는가. 홈은 이 값이 true 인 기능을 앞에 놓고, false 인 타일은 잠근다.
     * 컴포지션에 있는 동안 읽으며 값이 바뀌면 순서도 다시 정해진다.
     */
    @Composable
    fun isSupported(): Boolean

    /** Medium 이상 창의 홈이 놓는 카드. 홈에서 바로 제어할 수 있는 컨트롤을 담는다. */
    @Composable
    fun HomeCard(modifier: Modifier)
}

fun homeFeatureTileTestTag(feature: HomeFeature): String = "home:tile:${feature.id}"
