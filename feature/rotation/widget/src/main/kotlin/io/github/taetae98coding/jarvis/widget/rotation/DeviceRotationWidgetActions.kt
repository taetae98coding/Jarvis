package io.github.taetae98coding.jarvis.widget.rotation

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import io.github.taetae98coding.jarvis.domain.rotation.RotateDeviceUseCase
import io.github.taetae98coding.jarvis.domain.rotation.RotationAngle
import io.github.taetae98coding.jarvis.domain.rotation.SetDeviceRotationAngleUseCase
import io.github.taetae98coding.jarvis.domain.rotation.SetDeviceRotationLockUseCase

/*
 * 카드와 같은 유스케이스를 부른다. "각도를 고르면 잠근다" 같은 규칙은 여기 없고 domain 에 있다.
 * 끝나면 위젯을 다시 그린다. 설정 변경 트리거(WidgetRefresh)도 곧 한 번 더 그리지만, 그것은
 * 수 초 뒤라 탭한 손가락이 기다리기엔 길다.
 */

class SetAngleAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val angle = parameters[DegreesKey]?.let(RotationAngle::ofDegrees) ?: return

        inject<SetDeviceRotationAngleUseCase>()(angle)
        DeviceRotationWidget().update(context, glanceId)
    }
}

class RotateAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val steps = parameters[StepsKey] ?: return

        inject<RotateDeviceUseCase>()(steps)
        DeviceRotationWidget().update(context, glanceId)
    }
}

class SetLockAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val locked = parameters[LockedKey] ?: return

        inject<SetDeviceRotationLockUseCase>()(locked)
        DeviceRotationWidget().update(context, glanceId)
    }
}
