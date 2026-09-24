package io.github.taetae98coding.jarvis.ui.emulator

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.retain.RetainObserver
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap

/**
 * 기기 화면에 마지막으로 그린 프레임. `retain` 이 들고 있어서 컴포지션이 다시 만들어져도 빈 화면으로
 * 되돌아가지 않는다.
 *
 * ViewModel 이 아니라 화면이 들고 있는 이유는 [ImageBitmap] 이 플랫폼 그래픽 객체를 감싼 화면 계층
 * 타입이기 때문이다. 자세한 이유는 docs/common/retained-state.html 에 있다.
 *
 * [RetainObserver] 라서 영영 버려질 때(다른 기기로 바뀌거나 화면이 사라질 때) [onRetired] 에서 마지막
 * 프레임의 네이티브 메모리도 놓는다. 프레임을 바꿀 때마다 이전 것도 [updateImage] 가 놓는다
 * (docs/common/device-mirroring.html R10).
 */
@Stable
internal class EmulatorFrameState : RetainObserver {
    var image by mutableStateOf<ImageBitmap?>(null)
        private set

    /** 한 장도 받지 못한 채 실패했다는 뜻이다. 받아 둔 프레임이 있으면 그것을 계속 보여준다. */
    var failed by mutableStateOf(false)

    /** 새 프레임으로 바꾸고 이전 프레임의 픽셀 메모리를 곧바로 놓는다. */
    fun updateImage(next: ImageBitmap) {
        val previous = image
        image = next
        failed = false
        if (previous !== next) previous?.release()
    }

    /** 한 장도 못 받았으면 실패, 받아 둔 것이 있으면 그대로 둔다. */
    fun markFailed() {
        failed = image == null
    }

    override fun onRetired() {
        image?.release()
        image = null
    }

    override fun onUnused() {
        onRetired()
    }

    override fun onRetained() = Unit

    override fun onEnteredComposition() = Unit

    override fun onExitedComposition() = Unit
}
