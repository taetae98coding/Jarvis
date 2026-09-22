package io.github.taetae98coding.jarvis.ui.emulator

import io.github.taetae98coding.jarvis.domain.emulator.EmulatorDevice

/**
 * 에뮬레이터 카드에서 들어가는 두 화면. 내비게이션 라이브러리를 쓰지 않는 이유는 경로가 이 둘뿐이고
 * 딥링크도 백스택 복원도 필요 없기 때문이다. 경로가 늘면 그때 라이브러리를 들인다.
 */
internal sealed interface EmulatorRoute {
    data object Devices : EmulatorRoute

    data class Screen(val device: EmulatorDevice) : EmulatorRoute
}
