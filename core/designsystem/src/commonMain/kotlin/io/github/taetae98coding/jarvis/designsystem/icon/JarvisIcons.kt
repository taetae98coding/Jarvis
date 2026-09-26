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
    val Activity: ImageVector by lazy { outline("Activity", "M22 12h-4l-3 9L9 3l-3 9H2") }

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

    val Backspace: ImageVector by lazy {
        outline(
            "Backspace",
            "M21 4H8l-7 8 7 8h13a2 2 0 0 0 2-2V6a2 2 0 0 0-2-2z" +
                "M18 9l-6 6M12 9l6 6",
        )
    }

    val Battery: ImageVector by lazy {
        outline(
            "Battery",
            "M4 7h12a2 2 0 0 1 2 2v6a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V9a2 2 0 0 1 2-2z" +
                "M22 11v2",
        )
    }

    val Calculator: ImageVector by lazy {
        outline(
            "Calculator",
            "M6 2h12a2 2 0 0 1 2 2v16a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2z" +
                "M8 6h8M16 14v4M8 10h.01M12 10h.01M16 10h.01M8 14h.01M12 14h.01M8 18h.01M12 18h.01",
        )
    }

    val Check: ImageVector by lazy { outline("Check", "M20 6L9 17l-5-5") }

    val ChevronDown: ImageVector by lazy { outline("ChevronDown", "M6 9l6 6 6-6") }

    val ChevronLeft: ImageVector by lazy { outline("ChevronLeft", "M15 6l-6 6 6 6") }

    val ChevronRight: ImageVector by lazy { outline("ChevronRight", "M9 6l6 6-6 6") }

    val Claude: ImageVector by lazy { outline("Claude", "M12 3v18M3 12h18M7 7l10 10M17 7L7 17") }

    val Close: ImageVector by lazy { outline("Close", "M18 6L6 18M6 6l12 12") }

    val Code: ImageVector by lazy { outline("Code", "M16 18l6-6-6-6M8 6l-6 6 6 6") }

    val Copy: ImageVector by lazy {
        outline(
            "Copy",
            "M11 9h9a2 2 0 0 1 2 2v9a2 2 0 0 1-2 2h-9a2 2 0 0 1-2-2v-9a2 2 0 0 1 2-2z" +
                "M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1",
        )
    }

    val Edit: ImageVector by lazy { outline("Edit", "M12 20h9M16.5 3.5a2.12 2.12 0 0 1 3 3L7 19l-4 1 1-4z") }

    val Eye: ImageVector by lazy {
        outline(
            "Eye",
            "M2 12s3.5-7 10-7 10 7 10 7-3.5 7-10 7S2 12 2 12z" +
                "M15 12a3 3 0 1 1-6 0a3 3 0 1 1 6 0z",
        )
    }

    val File: ImageVector by lazy {
        outline(
            "File",
            "M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" +
                "M14 2v6h6",
        )
    }

    val Folder: ImageVector by lazy {
        outline("Folder", "M22 19a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h5l2 3h9a2 2 0 0 1 2 2z")
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

    val Moon: ImageVector by lazy { outline("Moon", "M21 12.79A9 9 0 1 1 11.21 3A7 7 0 0 0 21 12.79z") }

    val Logs: ImageVector by lazy { outline("Logs", "M8 6h13M8 12h13M8 18h13M3 6h.01M3 12h.01M3 18h.01") }

    val Monitor: ImageVector by lazy {
        outline(
            "Monitor",
            "M4 4h16a2 2 0 0 1 2 2v9a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2z" +
                "M8 21h8M12 17v4",
        )
    }

    val OpenInNew: ImageVector by lazy {
        outline(
            "OpenInNew",
            "M18 13v6a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h6" +
                "M15 3h6v6M10 14L21 3",
        )
    }

    val Pause: ImageVector by lazy { outline("Pause", "M6 4h4v16H6zM14 4h4v16h-4z") }

    val Play: ImageVector by lazy { outline("Play", "M7 4l13 8-13 8z") }

    val PlusMinus: ImageVector by lazy { outline("PlusMinus", "M12 3v14M5 10h14M5 21h14") }
    val QrCode: ImageVector by lazy {
        outline(
            "QrCode",
            "M4 3h3a1 1 0 0 1 1 1v3a1 1 0 0 1-1 1H4a1 1 0 0 1-1-1V4a1 1 0 0 1 1-1z" +
                "M17 3h3a1 1 0 0 1 1 1v3a1 1 0 0 1-1 1h-3a1 1 0 0 1-1-1V4a1 1 0 0 1 1-1z" +
                "M4 16h3a1 1 0 0 1 1 1v3a1 1 0 0 1-1 1H4a1 1 0 0 1-1-1v-3a1 1 0 0 1 1-1z" +
                "M21 16h-3a2 2 0 0 0-2 2v3M21 21v.01M12 7v3a2 2 0 0 1-2 2H7M3 12h.01M12 3h.01M12 16v.01M16 12h1M21 12v.01M12 21v-1",
        )
    }

    val Remove: ImageVector by lazy { outline("Remove", "M5 12h14") }

    val RotateLeft: ImageVector by lazy { outline("RotateLeft", "M3 12a9 9 0 1 0 3-6.7L3 8M3 3v5h5") }

    val RotateRight: ImageVector by lazy { outline("RotateRight", "M21 12a9 9 0 1 1-3-6.7L21 8M21 3v5h-5") }

    val Ruler: ImageVector by lazy {
        outline(
            "Ruler",
            "M21.3 15.3a2.4 2.4 0 0 1 0 3.4l-2.6 2.6a2.4 2.4 0 0 1-3.4 0L2.7 8.7a2.41 2.41 0 0 1 0-3.4l2.6-2.6a2.41 2.41 0 0 1 3.4 0z" +
                "M14.5 12.5l2-2M11.5 9.5l2-2M8.5 6.5l2-2M17.5 15.5l2-2",
        )
    }

    val SkipForward: ImageVector by lazy { outline("SkipForward", "M5 4l10 8-10 8zM19 5v14") }

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

    val Swap: ImageVector by lazy { outline("Swap", "M8 3L4 7l4 4M4 7h16M16 21l4-4-4-4M20 17H4") }

    val Terminal: ImageVector by lazy { outline("Terminal", "M4 17l6-6-6-6M12 19h8") }

    val Timer: ImageVector by lazy { outline("Timer", "M10 2h4M12 14l3-3M4 14a8 8 0 1 0 16 0a8 8 0 1 0-16 0") }

    val Type: ImageVector by lazy { outline("Type", "M4 7V4h16v3M9 20h6M12 4v16") }

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
            Activity, Add, Android, Apple, Back, Backspace, Battery, Calculator, Check, ChevronDown, ChevronLeft, ChevronRight, Claude, Close, Code, Copy, Edit,
            Eye, File, Folder, Forward, GitBranch, Globe, Logs, Monitor, Moon, Pause, Play, PlusMinus, QrCode, Remove, RotateLeft, RotateRight, Ruler,
            SkipForward, Smartphone, Sun, Swap, Terminal, Timer, Type, User, Wifi,
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
