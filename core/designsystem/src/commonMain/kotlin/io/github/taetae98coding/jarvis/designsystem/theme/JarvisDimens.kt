package io.github.taetae98coding.jarvis.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
class JarvisDimens(
    val spacing: JarvisSpacing = JarvisSpacing(),
    val iconSize: JarvisIconSize = JarvisIconSize(),
    val stroke: JarvisStroke = JarvisStroke(),
    val layout: JarvisLayout = JarvisLayout(),
)

@Immutable
class JarvisSpacing(
    val xxs: Dp = 2.dp,
    val xs: Dp = 4.dp,
    val s: Dp = 8.dp,
    val m: Dp = 12.dp,
    val l: Dp = 16.dp,
    val xl: Dp = 20.dp,
    val xxl: Dp = 24.dp,
)

@Immutable
class JarvisIconSize(
    /** 버튼 글자 옆이나 탭처럼 글자와 한 줄에 놓이는 아이콘. */
    val small: Dp = 18.dp,
    /** 단독 아이콘과 IconButton 안의 아이콘. */
    val medium: Dp = 24.dp,
)

@Immutable
class JarvisStroke(
    val thin: Dp = 1.dp,
    val thick: Dp = 4.dp,
)

@Immutable
class JarvisLayout(
    val screenPadding: Dp = 16.dp,
    val gridMinCellWidth: Dp = 220.dp,
    val topBarHeight: Dp = 56.dp,
)
