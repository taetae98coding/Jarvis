package io.github.taetae98coding.jarvis.data.focus

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import io.github.taetae98coding.jarvis.data.PlatformContext
import io.github.taetae98coding.jarvis.data.notification.createNotificationPermission
import io.github.taetae98coding.jarvis.domain.focus.FocusAlarmRepository
import io.github.taetae98coding.jarvis.domain.focus.FocusPhase
import java.util.TimeZone
import kotlin.time.Instant

internal actual fun createFocusAlarm(context: PlatformContext): FocusAlarmRepository = AlarmManagerFocusAlarm(context)

internal actual fun localUtcOffsetSeconds(instant: Instant): Int =
    TimeZone.getDefault().getOffset(instant.toEpochMilliseconds()) / MillisPerSecond

/**
 * 앱 프로세스가 죽어도 울리도록 AlarmManager 에 맡긴다. 울리면 [FocusAlarmReceiver] 가 알림을 올린다.
 *
 * 정확한 알람은 사용자가 "알람 및 리마인더" 를 허용했을 때만 쓴다. Android 14 부터 새로 설치한 앱은 기본으로
 * 거부이고, 거부면 `setAndAllowWhileIdle` 로 떨어져 시스템이 몇 분까지 늦출 수 있다(docs/platform/android.html#focus-timer).
 */
private class AlarmManagerFocusAlarm(platform: PlatformContext) : FocusAlarmRepository {
    private val context = platform.context
    private val alarmManager: AlarmManager? = context.getSystemService(AlarmManager::class.java)
    private val permission = createNotificationPermission(platform)
    private var permissionRequested = false

    override fun schedule(at: Instant, phase: FocusPhase) {
        // 권한 다이얼로그는 타이머를 시작하는 것을 막지 않는다. 거부해도 카드 안의 타이머는 돈다.
        // 두 번 거부하면 요청이 앱 알림 설정 화면을 열어서, 시작할 때마다 설정으로 튕기지 않게 프로세스당 한 번만 묻는다.
        if (!permissionRequested) {
            permissionRequested = true
            permission.requestPermission()
        }

        val manager = alarmManager ?: return
        val intent = pendingIntent(phase)
        val triggerAt = at.toEpochMilliseconds()

        runCatching {
            if (manager.canScheduleExactAlarms()) {
                manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, intent)
            } else {
                manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, intent)
            }
        }
    }

    override fun cancel() {
        alarmManager?.cancel(pendingIntent(FocusPhase.FOCUS))
    }

    // 요청 코드가 하나라 새 알람이 이전 것을 덮고, cancel 은 extra 와 무관하게 같은 PendingIntent 를 찾는다.
    private fun pendingIntent(phase: FocusPhase): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            AlarmRequestCode,
            Intent(context, FocusAlarmReceiver::class.java).putExtra(ExtraPhase, phase.storedValue),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
}

class FocusAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val phase = FocusPhase.fromStored(intent.getStringExtra(ExtraPhase)) ?: return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        if (!manager.areNotificationsEnabled()) return

        // 소리와 헤드업이 있어야 단계가 끝난 것을 알아챈다. 알림 컨트롤 채널(낮은 중요도)과 따로 둔다.
        manager.createNotificationChannel(
            NotificationChannel(ChannelId, "집중 타이머", NotificationManager.IMPORTANCE_HIGH),
        )

        val message = focusAlarmMessage(phase)
        val open = context.packageManager.getLaunchIntentForPackage(context.packageName)?.let {
            PendingIntent.getActivity(context, 0, it, PendingIntent.FLAG_IMMUTABLE)
        }
        val notification = Notification.Builder(context, ChannelId)
            // 이 모듈은 리소스를 갖지 않아 플랫폼 아이콘을 쓴다.
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(message.title)
            .setContentText(message.body)
            .setCategory(Notification.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(open)
            .build()

        runCatching { manager.notify(NotificationId, notification) }
    }
}

private const val ChannelId = "focus_timer"
private const val ExtraPhase = "phase"
private const val AlarmRequestCode = 0x4A46
private const val NotificationId = 0x4A46
private const val MillisPerSecond = 1000
