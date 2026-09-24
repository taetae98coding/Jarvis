package io.github.taetae98coding.jarvis.widget.rotation

import android.appwidget.AppWidgetManager
import android.content.Context
import android.provider.Settings
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import io.github.taetae98coding.jarvis.widget.WidgetRefresh

class DeviceRotationWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = DeviceRotationWidget()

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)

        // 트리거 job 은 한 번 뜨면 끝이라 그릴 때마다 다시 건다. 자기 자신이 보낸 갱신에도 다시 걸려
        // 사슬이 이어진다.
        WidgetRefresh.scheduleOnChange(context, RefreshJobId, javaClass, RefreshUris)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        WidgetRefresh.cancel(context, RefreshJobId)
    }
}

// 앱 안에서 유일해야 한다. 화면 꺼짐 방지 위젯은 0x4A5202 다.
private const val RefreshJobId = 0x4A5201

// 잠금과 고정 각도만 URI 로 알려진다. 자동 회전 중 디스플레이가 도는 것은 설정이 아니라서 잡지 못한다.
private val RefreshUris = listOf(
    Settings.System.getUriFor(Settings.System.ACCELEROMETER_ROTATION),
    Settings.System.getUriFor(Settings.System.USER_ROTATION),
)
