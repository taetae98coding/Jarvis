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
    // apply 안에서는 width 가 빈 Bitmap 의 폭(0)이 된다. 행 간격 0 으로 설치되면 installPixels 는 true 를
    // 주지만 그릴 때 Image::makeFromBitmap 이 실패해 EDT 가 죽는다(2026-09-25 실물 기기에서 확인).
    val rowBytes = width * 4

    return Bitmap().apply {
        check(installPixels(info, pixels, rowBytes)) { "BGRA 픽셀을 설치하지 못했다" }
        setImmutable()
    }.asComposeImageBitmap()
}

// 비트맵이 감싼 SkBitmap 을 닫아 네이티브 픽셀을 곧바로 놓는다. Cleaner 에 맡기면 초당 수십 장 × 수 MB 가
// GC 전까지 쌓인다.
internal actual fun ImageBitmap.release() {
    asSkiaBitmap().close()
}
