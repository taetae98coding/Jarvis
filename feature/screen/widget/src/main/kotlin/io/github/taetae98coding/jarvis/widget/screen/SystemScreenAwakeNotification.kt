package io.github.taetae98coding.jarvis.widget.screen

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import io.github.taetae98coding.jarvis.domain.screen.ScreenAwakeSettingsRepository
import io.github.taetae98coding.jarvis.feature.screen.widget.R
import io.github.taetae98coding.jarvis.domain.screen.SystemScreenAwakeNotificationRepository
import io.github.taetae98coding.jarvis.domain.screen.SystemScreenAwakeRepository
import io.github.taetae98coding.jarvis.domain.screen.SystemScreenAwakeStatus
import io.github.taetae98coding.jarvis.widget.NotificationWidget
import io.github.taetae98coding.jarvis.widget.NotificationWidgets
import io.github.taetae98coding.jarvis.widget.WidgetRefresh
import io.github.taetae98coding.jarvis.widget.WriteSettingsPermissionActivity
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import io.github.taetae98coding.jarvis.core.widget.R as CoreR
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

/**
 * 알림 창에 고정되는 화면 꺼짐 방지 컨트롤. 문구는 docs/common/home-screen-widget.html#behavior 가 기준이다.
 */
object SystemScreenAwakeNotification : NotificationWidget {
    override suspend fun sync(context: Context) {
        val notification = inject<SystemScreenAwakeNotificationRepository>()
        val system = inject<SystemScreenAwakeRepository>()
        val settings = inject<ScreenAwakeSettingsRepository>()

        combine(
            notification.observeStatus(),
            system.observeStatus(),
            settings.observeKeepSystemScreenAwake(),
        ) { pin, status, enabled -> Triple(pin, status, enabled) }
            .collect { (pin, status, enabled) ->
                if (pin.pinned && pin.permitted) post(context, status, enabled) else cancel(context)
            }
    }

    /** 설정을 다시 읽어 고정이면 지금 상태로 다시 게시한다. 알림 버튼과 설정 변경 트리거가 부른다. */
    suspend fun refresh(context: Context) {
        val pin = inject<SystemScreenAwakeNotificationRepository>().readStatus()

        if (pin.pinned && pin.permitted) {
            val status = inject<SystemScreenAwakeRepository>().observeStatus().first()
            val enabled = inject<ScreenAwakeSettingsRepository>().readKeepSystemScreenAwake()

            post(context, status, enabled)
        } else {
            cancel(context)
        }
    }

    /** 사용자가 알림을 지웠다. 알림은 이미 없으니 설정 변경 트리거만 멈춘다. 다음 onStart 가 다시 건다. */
    fun stopRefreshing(context: Context) {
        WidgetRefresh.cancel(context, RefreshJobId)
    }

    private fun post(context: Context, status: SystemScreenAwakeStatus, enabled: Boolean) {
        NotificationWidgets.ensureChannel(context)
        context.notificationManager?.notify(NotificationId, build(context, status, enabled))

        WidgetRefresh.scheduleOnChange(
            context,
            RefreshJobId,
            SystemScreenAwakeNotificationReceiver::class.java,
            RefreshUris,
            action = SystemScreenAwakeNotificationReceiver.ActionRefresh,
        )
    }

    private fun cancel(context: Context) {
        context.notificationManager?.cancel(NotificationId)
        WidgetRefresh.cancel(context, RefreshJobId)
    }

    private fun build(context: Context, status: SystemScreenAwakeStatus, enabled: Boolean): Notification {
        val collapsed = RemoteViews(context.packageName, R.layout.notification_system_screen_awake)
            .apply { bind(context, status, enabled) }
        val expanded = RemoteViews(context.packageName, R.layout.notification_system_screen_awake_big)
            .apply { bind(context, status, enabled) }

        return NotificationCompat.Builder(context, NotificationWidgets.ChannelId)
            .setSmallIcon(R.drawable.ic_system_screen_awake)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setCustomContentView(collapsed)
            .setCustomBigContentView(expanded)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setShowWhen(false)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(launchApp(context))
            .setDeleteIntent(broadcast(context, RequestDismissed, SystemScreenAwakeNotificationReceiver.ActionDismissed))
            .build()
    }

    private fun RemoteViews.bind(context: Context, status: SystemScreenAwakeStatus, enabled: Boolean) {
        setTextViewText(R.id.system_screen_awake_status, describe(status, enabled))
        setTextViewText(R.id.system_screen_awake_toggle, if (enabled) "켜짐" else "꺼짐")
        setOnClickPendingIntent(
            R.id.system_screen_awake_toggle,
            if (status.permitted) {
                broadcast(context, RequestToggle, SystemScreenAwakeNotificationReceiver.ActionSetEnabled) {
                    putExtra(SystemScreenAwakeNotificationReceiver.ExtraEnabled, !enabled)
                }
            } else {
                // 권한이 없으면 권한 중계 Activity 를 연다. 위젯과 같은 이유다.
                PendingIntent.getActivity(
                    context,
                    RequestPermission,
                    Intent(context, WriteSettingsPermissionActivity::class.java),
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                )
            },
        )
        // setBackgroundResource 는 @RemotableViewMethod 라 알림에서도 부를 수 있다.
        setInt(
            R.id.system_screen_awake_toggle,
            "setBackgroundResource",
            if (enabled) CoreR.drawable.notification_widget_button_selected else CoreR.drawable.notification_widget_button,
        )
        setTextColor(
            R.id.system_screen_awake_toggle,
            context.getColor(
                if (enabled) CoreR.color.notification_widget_button_selected_text else CoreR.color.notification_widget_button_text,
            ),
        )
    }

    private fun broadcast(context: Context, requestCode: Int, action: String, extras: Intent.() -> Unit = {}): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            requestCode,
            Intent(context, SystemScreenAwakeNotificationReceiver::class.java).setAction(action).apply(extras),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

    private fun launchApp(context: Context): PendingIntent? {
        val launch = context.packageManager.getLaunchIntentForPackage(context.packageName) ?: return null

        return PendingIntent.getActivity(context, RequestLaunch, launch, PendingIntent.FLAG_IMMUTABLE)
    }

    private fun describe(status: SystemScreenAwakeStatus, enabled: Boolean): String {
        if (!status.permitted) return "시스템 설정 변경 권한이 필요합니다. 누르면 권한 설정 화면이 열립니다."

        val state = if (enabled) "앱을 닫아도 화면이 꺼지지 않습니다." else "시스템 설정대로 화면이 꺼집니다."
        val current = status.screenOffTimeout?.let { " 현재 시스템 설정은 ${it.describe()}입니다." } ?: ""

        return state + current
    }

    private fun Duration.describe(): String =
        when {
            this >= 1.days -> "${inWholeDays}일"
            this >= 1.hours -> "${inWholeHours}시간"
            this >= 1.minutes -> "${inWholeMinutes}분"
            else -> "${inWholeSeconds}초"
        }

    private val Context.notificationManager: NotificationManager?
        get() = getSystemService(NotificationManager::class.java)
}

// 앱 안에서 유일해야 한다. 회전 알림은 0x4A5301 이다.
private const val NotificationId = 0x4A5302

// 위젯의 job(0x4A5201·0x4A5202)과 다르다. 회전 알림은 0x4A5203 이다.
private const val RefreshJobId = 0x4A5204

// 위젯 리시버와 같다. 설정 토글은 URI 가 없지만 권한이 있으면 토글이 곧 SCREEN_OFF_TIMEOUT 쓰기다.
private val RefreshUris = listOf(
    Settings.System.getUriFor(Settings.System.SCREEN_OFF_TIMEOUT),
)

private const val RequestLaunch = 0
private const val RequestDismissed = 1
private const val RequestPermission = 2
private const val RequestToggle = 3
