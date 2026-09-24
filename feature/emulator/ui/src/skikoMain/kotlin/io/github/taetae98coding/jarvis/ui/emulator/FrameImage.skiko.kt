package io.github.taetae98coding.jarvis.ui.emulator

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asComposeImageBitmap
import androidx.compose.ui.graphics.asSkiaBitmap
import io.github.taetae98coding.jarvis.domain.emulator.EmulatorFrame
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ColorType
import org.jetbrains.skia.ImageInfo

// Skia 의 N32 는 리틀엔디언에서 BGRA 라 디코더가 준 BGRA 를 그대로 설치한다. installPixels 가 픽셀을
// 자기 저장소로 복사하므로, 돌려 쓰는 버퍼(EmulatorFrame.Pixels)를 넘겨도 안전하다. setImmutable 이라야
// 그릴 때마다(SkiaBackedCanvas 가 Image.makeFromBitmap) 픽셀을 다시 복사하지 않는다.
internal actual fun EmulatorFrame.Pixels.toImageBitmap(): ImageBitmap {
    val info = ImageInfo(width, height, ColorType.BGRA_8888, ColorAlphaType.OPAQUE)

    return Bitmap().apply {
        installPixels(info, pixels, width * 4)
        setImmutable()
    }.asComposeImageBitmap()
}

// 비트맵이 감싼 SkBitmap 을 닫아 네이티브 픽셀을 곧바로 놓는다. Cleaner 에 맡기면 초당 수십 장 × 수 MB 가
// GC 전까지 쌓인다.
internal actual fun ImageBitmap.release() {
    asSkiaBitmap().close()
}
