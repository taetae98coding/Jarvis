package io.github.taetae98coding.jarvis.ui.qrcode

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import io.github.taetae98coding.jarvis.designsystem.theme.JarvisTheme
import io.github.taetae98coding.jarvis.domain.qrcode.QrMatrix
import kotlin.math.floor

/**
 * 조용한 영역을 포함해 정사각형으로 그린다. 모듈 한 칸은 가능하면 정수 픽셀로 맞춰, 칸 사이에 안티에일리어싱
 * 실선이 생겨 판독기가 모듈 경계를 잘못 읽지 않게 한다.
 */
@Composable
internal fun QrCodeImage(
    matrix: QrMatrix,
    modifier: Modifier = Modifier,
    colors: QrCodeImageColors = QrCodeImageDefaults.colors(),
) {
    Spacer(
        modifier = modifier
            .aspectRatio(1f)
            .semantics { contentDescription = QrCodeImageDefaults.ContentDescription }
            .drawWithCache {
                val modules = matrix.size + QrCodeImageDefaults.QuietZoneModules * 2
                val exact = size.minDimension / modules
                val cell = if (exact >= 1f) floor(exact) else exact
                val origin = Offset((size.width - cell * modules) / 2, (size.height - cell * modules) / 2)
                val runs = darkRuns(matrix)

                onDrawBehind {
                    drawRect(colors.light, topLeft = origin, size = Size(cell * modules, cell * modules))
                    for (run in runs) {
                        drawRect(
                            color = colors.dark,
                            topLeft = origin + Offset((run.x + QrCodeImageDefaults.QuietZoneModules) * cell, (run.y + QrCodeImageDefaults.QuietZoneModules) * cell),
                            size = Size(run.length * cell, cell),
                        )
                    }
                }
            },
    )
}

private class DarkRun(val x: Int, val y: Int, val length: Int)

// 한 줄의 연속된 어두운 모듈을 사각형 하나로 묶는다. 버전 40 도 칸마다 그리는 것보다 사각형이 몇 배 적다.
private fun darkRuns(matrix: QrMatrix): List<DarkRun> = buildList {
    for (y in 0 until matrix.size) {
        var x = 0
        while (x < matrix.size) {
            if (!matrix.isDark(x, y)) {
                x++
                continue
            }
            val start = x
            while (x < matrix.size && matrix.isDark(x, y)) x++
            add(DarkRun(start, y, x - start))
        }
    }
}

@Immutable
internal class QrCodeImageColors(val dark: Color, val light: Color)

internal object QrCodeImageDefaults {
    /** ISO/IEC 18004 가 요구하는 최소 조용한 영역. dp 가 아니라 모듈 수다. */
    const val QuietZoneModules = 4

    const val ContentDescription = "QR 코드"

    // 테마를 따르지 않는다. 판독기는 밝은 바탕의 어두운 모듈을 가정한다(docs/common/qr-code.html#decision-colors).
    @Composable
    @ReadOnlyComposable
    fun colors(): QrCodeImageColors = QrCodeImageColors(dark = JarvisTheme.colors.scanCodeDark, light = JarvisTheme.colors.scanCodeLight)
}
