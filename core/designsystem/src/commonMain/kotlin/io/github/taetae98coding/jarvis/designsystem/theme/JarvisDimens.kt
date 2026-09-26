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
    /** 홈 타일처럼 아이콘이 글자보다 주인공인 자리. */
    val large: Dp = 32.dp,
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
    /** 홈의 아이콘 타일 한 칸. 카드 최소 너비보다 좁아 같은 창에 열이 더 들어간다. */
    val gridMinTileWidth: Dp = 128.dp,
    /** 아이콘(large) + 간격(s) + 두 줄 라벨(bodyMedium 20dp × 2) + 카드 여백(l × 2). 라벨 줄 수가 달라도 타일 높이가 같다. */
    val tileHeight: Dp = 112.dp,
    /** M3 창 크기 등급의 Compact / Medium 경계. 이 너비부터 홈이 타일 대신 카드를 놓는다. */
    val mediumWindowMinWidth: Dp = 600.dp,
    val topBarHeight: Dp = 56.dp,
)
