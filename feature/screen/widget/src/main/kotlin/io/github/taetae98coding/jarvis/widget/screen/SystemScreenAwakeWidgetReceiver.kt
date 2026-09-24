package io.github.taetae98coding.jarvis.widget.screen

import android.appwidget.AppWidgetManager
import android.content.Context
import android.provider.Settings
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import io.github.taetae98coding.jarvis.widget.WidgetRefresh

class SystemScreenAwakeWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = SystemScreenAwakeWidget()

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)

        // 트리거 job 은 한 번 뜨면 끝이라 그릴 때마다 다시 건다.
        WidgetRefresh.scheduleOnChange(context, RefreshJobId, javaClass, RefreshUris)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        WidgetRefresh.cancel(context, RefreshJobId)
    }
}

// 앱 안에서 유일해야 한다. 화면 회전 위젯은 0x4A5201 이다.
private const val RefreshJobId = 0x4A5202

// 설정 토글(SharedPreferences)은 URI 가 없다. 권한이 있을 때는 토글이 곧 SCREEN_OFF_TIMEOUT 쓰기라
// 이 URI 로 따라온다.
private val RefreshUris = listOf(
    Settings.System.getUriFor(Settings.System.SCREEN_OFF_TIMEOUT),
)
