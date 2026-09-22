package io.github.taetae98coding.jarvis.ui.emulator

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap

/**
 * 기기 화면에 마지막으로 그린 프레임. `retain` 이 들고 있어서 컴포지션이 다시 만들어져도 빈 화면으로
 * 되돌아가지 않는다.
 *
 * ViewModel 이 아니라 화면이 들고 있는 이유는 [ImageBitmap] 이 플랫폼 그래픽 객체를 감싼 화면 계층
 * 타입이기 때문이다. 자세한 이유는 docs/common/retained-state.html 에 있다.
 */
@Stable
internal class EmulatorFrameState {
    var image by mutableStateOf<ImageBitmap?>(null)

    /** 한 장도 받지 못한 채 실패했다는 뜻이다. 받아 둔 프레임이 있으면 그것을 계속 보여준다. */
    var failed by mutableStateOf(false)
}
