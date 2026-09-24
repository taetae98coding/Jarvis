package io.github.taetae98coding.jarvis.ui.emulator

import androidx.compose.ui.graphics.ImageBitmap
import io.github.taetae98coding.jarvis.domain.emulator.EmulatorFrame
import org.jetbrains.compose.resources.decodeToImageBitmap

/**
 * 프레임 한 장을 [ImageBitmap] 으로 푼다. 깨진 프레임(에이전트가 오류 본문을 이미지인 척 준 것)이면
 * 그 장을 버리고 null 을 준다 — 화면 전체가 죽는 것보다 낫다.
 *
 * [EmulatorFrame.Encoded] 는 Compose 의 멀티플랫폼 디코더가, [EmulatorFrame.Pixels] 는 플랫폼별 [toImageBitmap]
 * 이 푼다. 무거운 일이라 부르는 쪽이 UI 스레드 밖(FrameDecodeContext)에서 부른다.
 */
internal fun EmulatorFrame.toImageBitmapOrNull(): ImageBitmap? =
    runCatching {
        when (this) {
            is EmulatorFrame.Encoded -> bytes.decodeToImageBitmap()
            is EmulatorFrame.Pixels -> toImageBitmap()
        }
    }.getOrNull()

/** BGRA 픽셀을 [ImageBitmap] 으로. 스트림에서 온 프레임은 JVM 에서만 이 경로를 탄다. */
internal expect fun EmulatorFrame.Pixels.toImageBitmap(): ImageBitmap

/**
 * 프레임 [ImageBitmap] 의 네이티브 픽셀 메모리를 놓는다. 초당 수십 장이 GC 를 기다리며 쌓이지 않게
 * 새 프레임으로 바꾸는 순간·화면을 떠나는 순간 부른다(docs/common/device-mirroring.html R10). GC 가
 * 관리하는 타깃에서는 no-op 이다.
 */
internal expect fun ImageBitmap.release()
