package io.github.taetae98coding.jarvis.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/** M3 [androidx.compose.material3.ColorScheme] 에 없는 색 역할. */
@Immutable
class JarvisColors(
    val success: Color,
    val onSuccess: Color,
    val successContainer: Color,
    val onSuccessContainer: Color,
    val warning: Color,
    val onWarning: Color,
    val warningContainer: Color,
    val onWarningContainer: Color,
    val terminalBackground: Color,
    val terminalForeground: Color,
    val scanCodeDark: Color,
    val scanCodeLight: Color,
)

// 셸 프로그램은 어두운 배경을 가정해서 색을 고른다. 라이트 테마에서도 터미널은 어둡게 둔다.
private val TerminalBackground = Color(0xFF1E1E1E)
private val TerminalForeground = Color(0xFFD4D4D4)

// QR 코드 판독기는 밝은 바탕의 어두운 모듈을 가정하고, 반전된 코드는 못 읽는 것이 많다. 다크 테마에서도 흑백 그대로 둔다.
private val ScanCodeDark = Color(0xFF000000)
private val ScanCodeLight = Color(0xFFFFFFFF)

internal val JarvisLightColors = JarvisColors(
    success = Color(0xFF1B6D2F),
    onSuccess = Color(0xFFFFFFFF),
    successContainer = Color(0xFFA4F4AA),
    onSuccessContainer = Color(0xFF002109),
    warning = Color(0xFF7C5800),
    onWarning = Color(0xFFFFFFFF),
    warningContainer = Color(0xFFFFDEA6),
    onWarningContainer = Color(0xFF271900),
    terminalBackground = TerminalBackground,
    terminalForeground = TerminalForeground,
    scanCodeDark = ScanCodeDark,
    scanCodeLight = ScanCodeLight,
)

internal val JarvisDarkColors = JarvisColors(
    success = Color(0xFF89D78F),
    onSuccess = Color(0xFF003913),
    successContainer = Color(0xFF00531E),
    onSuccessContainer = Color(0xFFA4F4AA),
    warning = Color(0xFFF5BF48),
    onWarning = Color(0xFF422C00),
    warningContainer = Color(0xFF5E4200),
    onWarningContainer = Color(0xFFFFDEA6),
    terminalBackground = TerminalBackground,
    terminalForeground = TerminalForeground,
    scanCodeDark = ScanCodeDark,
    scanCodeLight = ScanCodeLight,
)
