package io.github.taetae98coding.jarvis.ui.emulator

import android.graphics.Bitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import io.github.taetae98coding.jarvis.domain.emulator.EmulatorFrame

// Android 기기의 프레임은 호스트가 JPEG 로 주므로 이 경로는 런타임에 오지 않는다. 컴파일을 위해 둔다.
internal actual fun EmulatorFrame.Pixels.toImageBitmap(): ImageBitmap {
    val argb = IntArray(width * height) { i ->
        val base = i * 4
        val b = pixels[base].toInt() and 0xFF
        val g = pixels[base + 1].toInt() and 0xFF
        val r = pixels[base + 2].toInt() and 0xFF
        (0xFF shl 24) or (r shl 16) or (g shl 8) or b
    }

    return Bitmap.createBitmap(argb, width, height, Bitmap.Config.ARGB_8888).asImageBitmap()
}

// android.graphics.Bitmap 은 GC 가 관리한다.
internal actual fun ImageBitmap.release() = Unit
