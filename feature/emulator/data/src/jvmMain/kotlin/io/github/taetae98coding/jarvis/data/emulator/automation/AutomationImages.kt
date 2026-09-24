package io.github.taetae98coding.jarvis.data.emulator.automation

import io.github.taetae98coding.jarvis.automation.AutomationImage
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.IIOImage
import javax.imageio.ImageIO
import javax.imageio.ImageWriteParam
import kotlin.math.max
import kotlin.math.roundToInt

// Claude 가 한 장을 줄이지 않고 읽는 긴 변 상한에 맞춘다(docs/common/mcp-server.html R17).
internal const val AutomationImageMaxSize = 1280

/** 긴 변이 [maxSize] 를 넘지 않게 줄이는 배율. 줄일 필요가 없으면 1 이다. 캡처와 입력이 같은 규칙을 쓴다. */
internal fun fitScale(width: Int, height: Int, maxSize: Int = AutomationImageMaxSize): Double =
    minOf(1.0, maxSize.toDouble() / max(width, height))

internal fun scaled(size: Int, scale: Double): Int = (size * scale).roundToInt().coerceAtLeast(1)

/** BGRA 8888(행 간격 width × 4) 를 [targetWidth] × [targetHeight] JPEG 로. [pixels] 는 이미 복사한 것이어야 한다. */
internal fun bgraToImage(width: Int, height: Int, pixels: ByteArray, targetWidth: Int, targetHeight: Int): AutomationImage {
    val argb = IntArray(width * height)
    for (i in argb.indices) {
        val o = i * 4
        val b = pixels[o].toInt() and 0xFF
        val g = pixels[o + 1].toInt() and 0xFF
        val r = pixels[o + 2].toInt() and 0xFF
        argb[i] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
    }
    val source = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB).apply { setRGB(0, 0, width, height, argb, 0, width) }

    return encode(resize(source, targetWidth, targetHeight))
}

/** PNG·JPEG 한 장을 [targetWidth] × [targetHeight] JPEG 로. 읽지 못하면 null. */
internal fun resizeImage(bytes: ByteArray, targetWidth: Int, targetHeight: Int): AutomationImage? {
    val source = runCatching { ImageIO.read(bytes.inputStream()) }.getOrNull() ?: return null

    return encode(resize(source, targetWidth, targetHeight))
}

internal fun imageSize(bytes: ByteArray): Pair<Int, Int>? =
    runCatching { ImageIO.read(bytes.inputStream()) }.getOrNull()?.let { it.width to it.height }

private fun resize(source: BufferedImage, width: Int, height: Int): BufferedImage {
    if (source.width == width && source.height == height) return source

    val target = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
    val graphics = target.createGraphics()
    graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
    graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
    graphics.drawImage(source, 0, 0, width, height, null)
    graphics.dispose()

    return target
}

// PNG 는 1280px 한 장이 1~2MB 라 도구 결과가 무겁다. 좌표를 읽는 데는 JPEG 로 충분하다.
private fun encode(image: BufferedImage): AutomationImage {
    val output = ByteArrayOutputStream()
    val writer = ImageIO.getImageWritersByFormatName("jpeg").next()
    val parameters = writer.defaultWriteParam.apply {
        compressionMode = ImageWriteParam.MODE_EXPLICIT
        compressionQuality = JpegQuality
    }
    ImageIO.createImageOutputStream(output).use { stream ->
        writer.output = stream
        writer.write(null, IIOImage(image, null, null), parameters)
    }
    writer.dispose()

    return AutomationImage(output.toByteArray(), "image/jpeg", image.width, image.height)
}

private const val JpegQuality = 0.85f
