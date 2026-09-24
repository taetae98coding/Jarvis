package io.github.taetae98coding.jarvis.widget.rotation

import android.content.Context
import android.content.Intent
import io.github.taetae98coding.jarvis.domain.rotation.RotateDeviceUseCase
import io.github.taetae98coding.jarvis.domain.rotation.RotationAngle
import io.github.taetae98coding.jarvis.domain.rotation.SetDeviceRotationAngleUseCase
import io.github.taetae98coding.jarvis.domain.rotation.SetDeviceRotationLockUseCase
import io.github.taetae98coding.jarvis.widget.AsyncReceiver

/** 알림 버튼, 설정 변경 트리거, 사용자의 알림 삭제를 받는다. 위젯의 ActionCallback 셋과 같은 유스케이스를 부른다. */
class DeviceRotationNotificationReceiver : AsyncReceiver() {
    override suspend fun onReceiveAsync(context: Context, intent: Intent) {
        when (intent.action) {
            ActionSetAngle -> {
                val angle = RotationAngle.ofDegrees(intent.getIntExtra(ExtraDegrees, -1)) ?: return

                inject<SetDeviceRotationAngleUseCase>()(angle)
            }

            ActionRotate -> inject<RotateDeviceUseCase>()(intent.getIntExtra(ExtraSteps, 0))

            ActionSetLock -> inject<SetDeviceRotationLockUseCase>()(intent.getBooleanExtra(ExtraLocked, false))

            ActionRefresh -> Unit

            ActionDismissed -> {
                DeviceRotationNotification.stopRefreshing(context)

                return
            }

            else -> return
        }

        DeviceRotationNotification.refresh(context)
    }

    companion object {
        const val ActionSetAngle = "io.github.taetae98coding.jarvis.widget.rotation.SET_ANGLE"
        const val ActionRotate = "io.github.taetae98coding.jarvis.widget.rotation.ROTATE"
        const val ActionSetLock = "io.github.taetae98coding.jarvis.widget.rotation.SET_LOCK"
        const val ActionRefresh = "io.github.taetae98coding.jarvis.widget.rotation.REFRESH"
        const val ActionDismissed = "io.github.taetae98coding.jarvis.widget.rotation.DISMISSED"

        const val ExtraDegrees = "degrees"
        const val ExtraSteps = "steps"
        const val ExtraLocked = "locked"
    }
}
