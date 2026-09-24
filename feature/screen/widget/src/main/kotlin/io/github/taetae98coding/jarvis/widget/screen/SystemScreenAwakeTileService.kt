package io.github.taetae98coding.jarvis.widget.screen

import android.service.quicksettings.Tile
import io.github.taetae98coding.jarvis.domain.screen.ScreenAwakeSettingsRepository
import io.github.taetae98coding.jarvis.feature.screen.widget.R
import io.github.taetae98coding.jarvis.domain.screen.SetKeepSystemScreenAwakeUseCase
import io.github.taetae98coding.jarvis.domain.screen.SyncSystemScreenAwakeUseCase
import io.github.taetae98coding.jarvis.domain.screen.SystemScreenAwakeRepository
import io.github.taetae98coding.jarvis.domain.screen.SystemScreenAwakeStatus
import io.github.taetae98coding.jarvis.widget.StatusTileService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/** 빠른 설정의 "화면 꺼짐 방지" 타일. 탭하면 켬/끔이 바뀐다. */
class SystemScreenAwakeTileService : StatusTileService<SystemScreenAwakeTileService.State>() {
    class State(val status: SystemScreenAwakeStatus, val enabled: Boolean)

    override fun observe(): Flow<State> =
        combine(
            inject<SystemScreenAwakeRepository>().observeStatus(),
            inject<ScreenAwakeSettingsRepository>().observeKeepSystemScreenAwake(),
            ::State,
        )

    override fun Tile.render(status: State) {
        state = if (status.enabled) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        label = getString(R.string.system_screen_awake_tile_label)
        subtitle = when {
            !status.status.permitted -> "권한 필요"
            status.enabled -> "켜짐"
            else -> "꺼짐"
        }
        stateDescription = subtitle
    }

    override suspend fun onClick(status: State) {
        if (!status.status.permitted) {
            openWriteSettingsPermission()

            return
        }

        inject<SetKeepSystemScreenAwakeUseCase>()(!status.enabled)
        // 위젯·알림과 같은 이유로 시스템에 한 번 반영한다.
        inject<SyncSystemScreenAwakeUseCase>()()
    }
}
