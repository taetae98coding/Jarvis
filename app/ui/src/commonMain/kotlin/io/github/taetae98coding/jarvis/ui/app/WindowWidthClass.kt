package io.github.taetae98coding.jarvis.ui.app

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import io.github.taetae98coding.jarvis.designsystem.theme.JarvisTheme

/** M3 창 크기 등급 중 홈이 가르는 둘. Expanded 는 Medium 과 같이 다루므로 따로 두지 않는다. */
internal enum class WindowWidthClass {
    Compact,
    Medium,
}

/**
 * 창 너비로 등급을 정한다. 경계는 `JarvisLayout.mediumWindowMinWidth`(600dp) 다.
 *
 * `material3-window-size-class` 의 `calculateWindowSizeClass()` 대신 `LocalWindowInfo.containerSize` 를 직접
 * 읽는다. 그 라이브러리는 실험 API 인 데다 Android 에서 Activity 를 요구해 테스트가 창 크기를 바꿔 끼울 수 없다.
 * 버린 후보는 docs/common/home-adaptive-layout.html#implementation 에 있다.
 */
@Composable
internal fun currentWindowWidthClass(): WindowWidthClass {
    val width = LocalWindowInfo.current.containerSize.width
    val mediumMinWidth = with(LocalDensity.current) { JarvisTheme.dimens.layout.mediumWindowMinWidth.roundToPx() }

    return if (width < mediumMinWidth) WindowWidthClass.Compact else WindowWidthClass.Medium
}
