package io.github.taetae98coding.jarvis.domain.screen

import kotlinx.coroutines.flow.MutableStateFlow

internal class FakeSystemScreenAwakeRepository(
    initial: SystemScreenAwakeStatus = SystemScreenAwakeStatus(supported = true, permitted = true),
) : SystemScreenAwakeRepository {
    override val status = MutableStateFlow(initial)

    // 적용 호출을 순서대로 기록한다. "권한이 없으면 아무것도 하지 않는다" 를 확인하려면 마지막
    // 값이 아니라 불렸는지 여부를 봐야 한다.
    val applied = mutableListOf<Boolean>()
    var permissionRequests = 0
        private set

    override fun setEnabled(enabled: Boolean) {
        applied += enabled
    }

    override fun requestPermission() {
        permissionRequests++
    }
}
