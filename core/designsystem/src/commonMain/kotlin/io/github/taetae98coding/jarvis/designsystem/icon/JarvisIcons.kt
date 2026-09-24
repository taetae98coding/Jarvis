package io.github.taetae98coding.jarvis.designsystem.icon

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/**
 * 24 × 24 뷰포트, 선 굵기 2 의 윤곽선 아이콘. 칠하지 않고 선만 긋는다.
 *
 * 선 색은 검정이지만 `Icon` 의 tint 가 색 필터로 덮으므로 실제 색은 쓰는 쪽의 `LocalContentColor` 다.
 */
object JarvisIcons {
    val Add: ImageVector by lazy { outline("Add", "M12 5v14M5 12h14") }

    val Back: ImageVector by lazy { outline("Back", "M19 12H5M12 19l-7-7 7-7") }

    val ChevronRight: ImageVector by lazy { outline("ChevronRight", "M9 6l6 6-6 6") }

    val Claude: ImageVector by lazy { outline("Claude", "M12 3v18M3 12h18M7 7l10 10M17 7L7 17") }

    val Close: ImageVector by lazy { outline("Close", "M18 6L6 18M6 6l12 12") }

    val Eye: ImageVector by lazy {
        outline(
            "Eye",
            "M2 12s3.5-7 10-7 10 7 10 7-3.5 7-10 7S2 12 2 12z" +
                "M15 12a3 3 0 1 1-6 0a3 3 0 1 1 6 0z",
        )
    }

    val Monitor: ImageVector by lazy {
        outline(
            "Monitor",
            "M4 4h16a2 2 0 0 1 2 2v9a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2z" +
                "M8 21h8M12 17v4",
        )
    }

    val Play: ImageVector by lazy { outline("Play", "M7 4l13 8-13 8z") }

    val RotateLeft: ImageVector by lazy { outline("RotateLeft", "M3 12a9 9 0 1 0 3-6.7L3 8M3 3v5h5") }

    val RotateRight: ImageVector by lazy { outline("RotateRight", "M21 12a9 9 0 1 1-3-6.7L21 8M21 3v5h-5") }

    val Smartphone: ImageVector by lazy {
        outline(
            "Smartphone",
            "M7 2h10a2 2 0 0 1 2 2v16a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2z" +
                "M11 18h2",
        )
    }

    val SplitSideBySide: ImageVector by lazy { outline("SplitSideBySide", "$Frame M12 3v18") }

    val SplitStacked: ImageVector by lazy { outline("SplitStacked", "$Frame M3 12h18") }

    val Sun: ImageVector by lazy {
        outline(
            "Sun",
            "M16 12a4 4 0 1 1-8 0a4 4 0 1 1 8 0z" +
                "M12 2v2M12 20v2M2 12h2M20 12h2" +
                "M4.93 4.93l1.41 1.41M17.66 17.66l1.41 1.41M6.34 17.66l-1.41 1.41M19.07 4.93l-1.41 1.41",
        )
    }

    val Terminal: ImageVector by lazy { outline("Terminal", "M4 17l6-6-6-6M12 19h8") }

    val Wifi: ImageVector by lazy {
        outline(
            "Wifi",
            "M2 8.82a15 15 0 0 1 20 0M5 12.86a10 10 0 0 1 14 0M8.5 16.43a5 5 0 0 1 7 0M12 20h.01",
        )
    }

    internal val all: List<ImageVector>
        get() = listOf(
            Add, Back, ChevronRight, Claude, Close, Eye, Monitor, Play, RotateLeft, RotateRight,
            Smartphone, SplitSideBySide, SplitStacked, Sun, Terminal, Wifi,
        )
}

// 분할 아이콘 둘이 공유하는 창 테두리.
private const val Frame = "M5 3h14a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2z"

private fun outline(name: String, pathData: String): ImageVector =
    ImageVector.Builder(
        name = "JarvisIcons.$name",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).addPath(
        pathData = addPathNodes(pathData),
        stroke = SolidColor(Color.Black),
        strokeLineWidth = 2f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round,
    ).build()
