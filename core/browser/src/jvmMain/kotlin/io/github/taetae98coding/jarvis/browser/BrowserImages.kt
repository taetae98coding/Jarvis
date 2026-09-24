package io.github.taetae98coding.jarvis.browser

import io.github.taetae98coding.jarvis.automation.AutomationImage
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.IIOImage
import javax.imageio.ImageIO
import javax.imageio.ImageWriteParam

/**
 * 엔진이 그린 BGRA 한 장(창 밀도 배율)을 CSS 크기 [targetWidth] × [targetHeight] 의 JPEG 로. 이미지 픽셀이 곧
 * 클릭 좌표가 된다(docs/common/mcp-server.html R12).
 */
internal fun encodeJpeg(width: Int, height: Int, pixels: ByteArray, targetWidth: Int, targetHeight: Int): AutomationImage {
    val argb = IntArray(width * height)
    for (i in argb.indices) {
        val o = i * 4
        argb[i] = (0xFF shl 24) or
            ((pixels[o + 2].toInt() and 0xFF) shl 16) or
            ((pixels[o + 1].toInt() and 0xFF) shl 8) or
            (pixels[o].toInt() and 0xFF)
    }
    val source = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB).apply { setRGB(0, 0, width, height, argb, 0, width) }
    val image = if (width == targetWidth && height == targetHeight) {
        source
    } else {
        BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB).also { target ->
            val graphics = target.createGraphics()
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
            graphics.drawImage(source, 0, 0, targetWidth, targetHeight, null)
            graphics.dispose()
        }
    }

    val output = ByteArrayOutputStream()
    val writer = ImageIO.getImageWritersByFormatName("jpeg").next()
    val parameters = writer.defaultWriteParam.apply {
        compressionMode = ImageWriteParam.MODE_EXPLICIT
        compressionQuality = 0.85f
    }
    ImageIO.createImageOutputStream(output).use { stream ->
        writer.output = stream
        writer.write(null, IIOImage(image, null, null), parameters)
    }
    writer.dispose()

    return AutomationImage(output.toByteArray(), "image/jpeg", targetWidth, targetHeight)
}
