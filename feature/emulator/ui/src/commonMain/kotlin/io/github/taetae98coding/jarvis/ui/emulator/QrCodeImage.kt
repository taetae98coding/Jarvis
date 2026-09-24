package io.github.taetae98coding.jarvis.ui.emulator

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
internal fun QrCodeImage(
    text: String,
    contentDescription: String,
    modifier: Modifier = Modifier,
    size: Dp = QrCodeImageDefaults.size,
    darkColor: Color = QrCodeImageDefaults.darkColor,
    lightColor: Color = QrCodeImageDefaults.lightColor,
) {
    val qr = remember(text) { QrCode.encode(text) }

    Canvas(modifier = modifier.size(size).semantics { this.contentDescription = contentDescription }) {
        drawRect(lightColor)

        val cells = qr.size + QrCodeImageDefaults.QuietZoneModules * 2
        val module = this.size.minDimension / cells
        // 칸 경계가 픽셀 사이에 걸리면 이웃 칸 사이에 가는 틈이 보인다. 반 픽셀 겹쳐 그린다.
        val cell = Size(module + 0.5f, module + 0.5f)

        for (y in 0 until qr.size) {
            for (x in 0 until qr.size) {
                if (qr[x, y]) {
                    drawRect(
                        color = darkColor,
                        topLeft = Offset(
                            (x + QrCodeImageDefaults.QuietZoneModules) * module,
                            (y + QrCodeImageDefaults.QuietZoneModules) * module,
                        ),
                        size = cell,
                    )
                }
            }
        }
    }
}

internal object QrCodeImageDefaults {
    // 규격이 요구하는 사방 여백. 이보다 좁으면 둘레의 다른 그림을 칸으로 잘못 읽는 스캐너가 있다.
    const val QuietZoneModules = 4

    // 페어링 페이로드는 버전 4(33칸 + 여백 8칸)라 한 칸이 5dp 남짓이 된다.
    val size: Dp = 220.dp

    // 테마를 따르지 않는다. 기기의 스캐너는 밝은 바탕의 어두운 칸만 확실히 읽고, 다크 테마의 반전된
    // QR 은 읽지 못하는 기기가 있다.
    val darkColor: Color = Color.Black
    val lightColor: Color = Color.White
}
