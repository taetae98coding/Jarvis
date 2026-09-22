package io.github.taetae98coding.jarvis.data.screen

import io.github.taetae98coding.jarvis.domain.screen.SystemScreenAwakeRepository
import io.github.taetae98coding.jarvis.domain.screen.SystemScreenAwakeStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

internal class DefaultSystemScreenAwakeRepository(
    private val dataSource: SystemScreenAwakeDataSource,
    scope: CoroutineScope,
) : SystemScreenAwakeRepository {
    // 구독자가 여럿이어도 권한 폴링과 ContentObserver 는 한 벌만 돈다.
    override val status: StateFlow<SystemScreenAwakeStatus> =
        dataSource.observeStatus()
            .stateIn(scope, SharingStarted.WhileSubscribed(), dataSource.readStatus())

    override fun setEnabled(enabled: Boolean) {
        dataSource.setEnabled(enabled)
    }

    override fun requestPermission() {
        dataSource.requestPermission()
    }
}
