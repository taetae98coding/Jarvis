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

    val Android: ImageVector by lazy {
        outline(
            "Android",
            "M4 17a8 8 0 0 1 16 0z" +
                "M7.5 10.5L5.5 7M16.5 10.5l2-3.5" +
                "M9 14h.01M15 14h.01",
        )
    }

    val Apple: ImageVector by lazy {
        outline(
            "Apple",
            "M12 20.94c1.5 0 2.75 1.06 4 1.06c3 0 6-8 6-12.22A4.91 4.91 0 0 0 17 5c-2.22 0-4 1.44-5 2" +
                "c-1-.56-2.78-2-5-2a4.9 4.9 0 0 0-5 4.78C2 14 5 22 8 22c1.25 0 2.5-1.06 4-1.06z" +
                "M10 2c1 .5 2 2 2 5",
        )
    }

    val Back: ImageVector by lazy { outline("Back", "M19 12H5M12 19l-7-7 7-7") }

    val ChevronRight: ImageVector by lazy { outline("ChevronRight", "M9 6l6 6-6 6") }

    val Claude: ImageVector by lazy { outline("Claude", "M12 3v18M3 12h18M7 7l10 10M17 7L7 17") }

    val Close: ImageVector by lazy { outline("Close", "M18 6L6 18M6 6l12 12") }

    val Edit: ImageVector by lazy { outline("Edit", "M12 20h9M16.5 3.5a2.12 2.12 0 0 1 3 3L7 19l-4 1 1-4z") }

    val Eye: ImageVector by lazy {
        outline(
            "Eye",
            "M2 12s3.5-7 10-7 10 7 10 7-3.5 7-10 7S2 12 2 12z" +
                "M15 12a3 3 0 1 1-6 0a3 3 0 1 1 6 0z",
        )
    }

    val Forward: ImageVector by lazy { outline("Forward", "M5 12h14M12 5l7 7-7 7") }

    val GitBranch: ImageVector by lazy {
        outline(
            "GitBranch",
            "M6 3v12" +
                "M21 6a3 3 0 1 1-6 0a3 3 0 1 1 6 0z" +
                "M9 18a3 3 0 1 1-6 0a3 3 0 1 1 6 0z" +
                "M18 9a9 9 0 0 1-9 9",
        )
    }

    val Globe: ImageVector by lazy {
        outline(
            "Globe",
            "M22 12a10 10 0 1 1-20 0a10 10 0 1 1 20 0z" +
                "M2 12h20M12 2a15 15 0 0 1 0 20a15 15 0 0 1 0-20z",
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

    val Sun: ImageVector by lazy {
        outline(
            "Sun",
            "M16 12a4 4 0 1 1-8 0a4 4 0 1 1 8 0z" +
                "M12 2v2M12 20v2M2 12h2M20 12h2" +
                "M4.93 4.93l1.41 1.41M17.66 17.66l1.41 1.41M6.34 17.66l-1.41 1.41M19.07 4.93l-1.41 1.41",
        )
    }

    val Terminal: ImageVector by lazy { outline("Terminal", "M4 17l6-6-6-6M12 19h8") }

    val User: ImageVector by lazy {
        outline(
            "User",
            "M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2" +
                "M16 7a4 4 0 1 1-8 0a4 4 0 1 1 8 0z",
        )
    }

    val Wifi: ImageVector by lazy {
        outline(
            "Wifi",
            "M2 8.82a15 15 0 0 1 20 0M5 12.86a10 10 0 0 1 14 0M8.5 16.43a5 5 0 0 1 7 0M12 20h.01",
        )
    }

    internal val all: List<ImageVector>
        get() = listOf(
            Add, Android, Apple, Back, ChevronRight, Claude, Close, Edit, Eye, Forward, GitBranch, Globe, Monitor, Play, RotateLeft,
            RotateRight, Smartphone, Sun, Terminal, User, Wifi,
        )
}

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
