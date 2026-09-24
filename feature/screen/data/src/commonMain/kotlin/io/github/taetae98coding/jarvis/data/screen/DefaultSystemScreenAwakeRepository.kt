package io.github.taetae98coding.jarvis.data.screen

import io.github.taetae98coding.jarvis.domain.screen.SystemScreenAwakeRepository
import io.github.taetae98coding.jarvis.domain.screen.SystemScreenAwakeStatus
import kotlinx.coroutines.flow.Flow

internal class DefaultSystemScreenAwakeRepository(
    private val dataSource: SystemScreenAwakeDataSource,
) : SystemScreenAwakeRepository {
    override fun observeStatus(): Flow<SystemScreenAwakeStatus> = dataSource.observeStatus()

    override fun readStatus(): SystemScreenAwakeStatus = dataSource.readStatus()

    override fun setEnabled(enabled: Boolean) {
        dataSource.setEnabled(enabled)
    }

    override fun requestPermission() {
        dataSource.requestPermission()
    }
}
