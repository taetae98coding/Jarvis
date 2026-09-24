package io.github.taetae98coding.jarvis.widget.rotation

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import io.github.taetae98coding.jarvis.domain.rotation.DeviceRotationNotificationRepository
import io.github.taetae98coding.jarvis.domain.rotation.DeviceRotationRepository
import io.github.taetae98coding.jarvis.feature.rotation.widget.R
import io.github.taetae98coding.jarvis.domain.rotation.DeviceRotationStatus
import io.github.taetae98coding.jarvis.domain.rotation.RotationAngle
import io.github.taetae98coding.jarvis.widget.NotificationWidget
import io.github.taetae98coding.jarvis.widget.NotificationWidgets
import io.github.taetae98coding.jarvis.widget.WidgetRefresh
import io.github.taetae98coding.jarvis.widget.WriteSettingsPermissionActivity
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import io.github.taetae98coding.jarvis.core.widget.R as CoreR

/**
 * 알림 창에 고정되는 화면 회전 컨트롤. 문구는 docs/common/home-screen-widget.html#behavior 가 기준이다.
 *
 * 그리기는 XML RemoteViews 다. 위젯의 Glance 를 여기 쓸 수 없는 이유는
 * docs/common/notification-widget.html#implementation 에 있다.
 */
object DeviceRotationNotification : NotificationWidget {
    override suspend fun sync(context: Context) {
        val notification = inject<DeviceRotationNotificationRepository>()
        val rotation = inject<DeviceRotationRepository>()

        combine(notification.observeStatus(), rotation.observeStatus()) { pin, status -> pin to status }
            .collect { (pin, status) ->
                if (pin.pinned && pin.permitted) post(context, status) else cancel(context)
            }
    }

    /** 설정을 다시 읽어 고정이면 지금 상태로 다시 게시한다. 알림 버튼과 설정 변경 트리거가 부른다. */
    suspend fun refresh(context: Context) {
        val pin = inject<DeviceRotationNotificationRepository>().readStatus()

        if (pin.pinned && pin.permitted) {
            post(context, inject<DeviceRotationRepository>().observeStatus().first())
        } else {
            cancel(context)
        }
    }

    /** 사용자가 알림을 지웠다. 알림은 이미 없으니 설정 변경 트리거만 멈춘다. 다음 onStart 가 다시 건다. */
    fun stopRefreshing(context: Context) {
        WidgetRefresh.cancel(context, RefreshJobId)
    }

    private fun post(context: Context, status: DeviceRotationStatus) {
        NotificationWidgets.ensureChannel(context)
        context.notificationManager?.notify(NotificationId, build(context, status))

        WidgetRefresh.scheduleOnChange(
            context,
            RefreshJobId,
            DeviceRotationNotificationReceiver::class.java,
            RefreshUris,
            action = DeviceRotationNotificationReceiver.ActionRefresh,
        )
    }

    private fun cancel(context: Context) {
        context.notificationManager?.cancel(NotificationId)
        WidgetRefresh.cancel(context, RefreshJobId)
    }

    private fun build(context: Context, status: DeviceRotationStatus): Notification {
        val collapsed = RemoteViews(context.packageName, R.layout.notification_device_rotation)
            .apply { bind(context, status, expanded = false) }
        val expanded = RemoteViews(context.packageName, R.layout.notification_device_rotation_big)
            .apply { bind(context, status, expanded = true) }

        return NotificationCompat.Builder(context, NotificationWidgets.ChannelId)
            .setSmallIcon(R.drawable.ic_device_rotation)
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
            .setDeleteIntent(broadcast(context, RequestDismissed, DeviceRotationNotificationReceiver.ActionDismissed))
            .build()
    }

    private fun RemoteViews.bind(context: Context, status: DeviceRotationStatus, expanded: Boolean) {
        setTextViewText(R.id.device_rotation_status, status.describe())

        button(
            context,
            R.id.device_rotation_lock,
            text = if (status.locked) "고정" else "자동",
            selected = status.locked,
            onClick = status.actionOrPermission(context) {
                broadcast(context, RequestLock, DeviceRotationNotificationReceiver.ActionSetLock) {
                    putExtra(DeviceRotationNotificationReceiver.ExtraLocked, !status.locked)
                }
            },
        )

        if (!expanded) return

        RotationAngle.entries.forEach { angle ->
            button(
                context,
                angleViewId(angle),
                text = "${angle.degrees}°",
                selected = status.angle == angle,
                onClick = status.actionOrPermission(context) {
                    broadcast(context, RequestAngle + angle.ordinal, DeviceRotationNotificationReceiver.ActionSetAngle) {
                        putExtra(DeviceRotationNotificationReceiver.ExtraDegrees, angle.degrees)
                    }
                },
            )
        }

        listOf(R.id.device_rotation_backward to -1, R.id.device_rotation_forward to 1).forEach { (viewId, steps) ->
            setOnClickPendingIntent(
                viewId,
                status.actionOrPermission(context) {
                    broadcast(context, RequestRotate + steps, DeviceRotationNotificationReceiver.ActionRotate) {
                        putExtra(DeviceRotationNotificationReceiver.ExtraSteps, steps)
                    }
                },
            )
        }
    }

    private fun RemoteViews.button(
        context: Context,
        viewId: Int,
        text: String,
        selected: Boolean,
        onClick: PendingIntent,
    ) {
        setTextViewText(viewId, text)
        setOnClickPendingIntent(viewId, onClick)
        // setBackgroundResource 는 @RemotableViewMethod 라 알림에서도 부를 수 있다.
        setInt(
            viewId,
            "setBackgroundResource",
            if (selected) CoreR.drawable.notification_widget_button_selected else CoreR.drawable.notification_widget_button,
        )
        setTextColor(
            viewId,
            context.getColor(
                if (selected) CoreR.color.notification_widget_button_selected_text else CoreR.color.notification_widget_button_text,
            ),
        )
    }

    private fun angleViewId(angle: RotationAngle): Int =
        when (angle) {
            RotationAngle.Degrees0 -> R.id.device_rotation_angle_0
            RotationAngle.Degrees90 -> R.id.device_rotation_angle_90
            RotationAngle.Degrees180 -> R.id.device_rotation_angle_180
            RotationAngle.Degrees270 -> R.id.device_rotation_angle_270
        }

    // 권한이 없으면 모든 버튼이 권한 중계 Activity 를 연다. 위젯과 같은 이유다(그려진 버튼이 낡아도 탭 시점에
    // 권한을 다시 본다). SystemUI 가 Activity PendingIntent 를 직접 보내므로 트램펄린 금지와 무관하다.
    private fun DeviceRotationStatus.actionOrPermission(context: Context, action: () -> PendingIntent): PendingIntent =
        if (permitted) {
            action()
        } else {
            PendingIntent.getActivity(
                context,
                RequestPermission,
                Intent(context, WriteSettingsPermissionActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
        }

    // PendingIntent 는 extras 를 비교하지 않는다. 각도만 다른 인텐트가 하나로 합쳐지지 않게 요청 코드를 나눈다.
    private fun broadcast(context: Context, requestCode: Int, action: String, extras: Intent.() -> Unit = {}): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            requestCode,
            Intent(context, DeviceRotationNotificationReceiver::class.java).setAction(action).apply(extras),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

    private fun launchApp(context: Context): PendingIntent? {
        val launch = context.packageManager.getLaunchIntentForPackage(context.packageName) ?: return null

        return PendingIntent.getActivity(context, RequestLaunch, launch, PendingIntent.FLAG_IMMUTABLE)
    }

    private fun DeviceRotationStatus.describe(): String {
        val degrees = angle?.degrees

        return when {
            !permitted -> "시스템 설정 변경 권한이 필요합니다. 누르면 권한 설정 화면이 열립니다."
            degrees == null -> "현재 각도를 읽을 수 없습니다."
            locked -> "현재 $degrees°에 고정되어 있습니다."
            else -> "현재 $degrees°, 센서를 따라 회전합니다."
        }
    }

    private val Context.notificationManager: NotificationManager?
        get() = getSystemService(NotificationManager::class.java)
}

// 앱 안에서 유일해야 한다. 꺼짐 방지 알림은 0x4A5302 다.
private const val NotificationId = 0x4A5301

// 위젯의 job(0x4A5201·0x4A5202)과 다르다. 꺼짐 방지 알림은 0x4A5204 다.
private const val RefreshJobId = 0x4A5203

// 위젯 리시버와 같다. 잠금과 고정 각도만 URI 로 알려진다.
private val RefreshUris = listOf(
    Settings.System.getUriFor(Settings.System.ACCELEROMETER_ROTATION),
    Settings.System.getUriFor(Settings.System.USER_ROTATION),
)

private const val RequestLaunch = 0
private const val RequestDismissed = 1
private const val RequestPermission = 2
private const val RequestLock = 3
private const val RequestAngle = 10
private const val RequestRotate = 20
