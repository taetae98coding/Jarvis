package io.github.taetae98coding.jarvis.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// 글꼴 파일은 번들하지 않는다. 한글 글꼴이 Web 첫 로딩을 늦추는 이유는
// docs/common/design-system.html#decision-font 에 있다.
internal val JarvisTypography: Typography = Typography().run {
    copy(
        titleLarge = titleLarge.copy(fontWeight = FontWeight.SemiBold),
        titleMedium = titleMedium.copy(fontWeight = FontWeight.SemiBold),
    )
}

internal val JarvisCodeTextStyle = TextStyle(
    fontFamily = FontFamily.Monospace,
    fontSize = 13.sp,
)
