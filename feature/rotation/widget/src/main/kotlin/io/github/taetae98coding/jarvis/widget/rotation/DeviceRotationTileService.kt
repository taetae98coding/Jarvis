package io.github.taetae98coding.jarvis.widget.rotation

import android.service.quicksettings.Tile
import io.github.taetae98coding.jarvis.domain.rotation.DeviceRotationRepository
import io.github.taetae98coding.jarvis.feature.rotation.widget.R
import io.github.taetae98coding.jarvis.domain.rotation.DeviceRotationStatus
import io.github.taetae98coding.jarvis.domain.rotation.RotateDeviceUseCase
import io.github.taetae98coding.jarvis.widget.StatusTileService
import kotlinx.coroutines.flow.Flow

/**
 * 빠른 설정의 "화면 회전" 타일. 탭하면 +90° 돌리고 그 각도에 고정한다.
 * 잠금 토글은 시스템의 자동 회전 타일이 이미 있어 만들지 않는다(docs/common/notification-widget.html).
 */
class DeviceRotationTileService : StatusTileService<DeviceRotationStatus>() {
    override fun observe(): Flow<DeviceRotationStatus> = inject<DeviceRotationRepository>().observeStatus()

    override fun Tile.render(status: DeviceRotationStatus) {
        state = if (status.locked) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        label = getString(R.string.device_rotation_tile_label)
        subtitle = status.subtitle()
        stateDescription = subtitle
    }

    override suspend fun onClick(status: DeviceRotationStatus) {
        if (!status.permitted) {
            openWriteSettingsPermission()

            return
        }

        inject<RotateDeviceUseCase>()(1)
    }

    private fun DeviceRotationStatus.subtitle(): String {
        val degrees = angle?.degrees

        return when {
            !permitted -> "권한 필요"
            degrees == null -> "각도 알 수 없음"
            locked -> "$degrees° 고정"
            else -> "자동 회전"
        }
    }
}
