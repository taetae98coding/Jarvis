package io.github.taetae98coding.jarvis.widget

import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PersistableBundle

/**
 * 시스템 설정(콘텐트 URI)이 바뀌면 위젯이나 알림을 다시 그리게 한다.
 *
 * 위젯·알림에는 ContentObserver 를 붙잡아 둘 프로세스가 없다. JobScheduler 의 콘텐트 URI 트리거는 시스템이
 * 대신 URI 알림을 지켜보다가 job 을 띄워 주므로, 앱 카드가 ContentObserver 로 따라가는 것과 같은
 * 변화를 프로세스 없이 받는다. 트리거 job 은 한 번 뜨면 끝이라 받은 쪽(위젯 리시버의 onUpdate, 알림
 * 리시버의 REFRESH)이 다시 건다.
 *
 * [action] 이 ACTION_APPWIDGET_UPDATE 면 살아 있는 위젯 id 를 모아 보내고, 다른 액션이면 그 액션의
 * 명시적 브로드캐스트를 리시버에 보낸다(알림 위젯).
 */
object WidgetRefresh {
    fun scheduleOnChange(
        context: Context,
        jobId: Int,
        receiver: Class<out BroadcastReceiver>,
        uris: List<Uri>,
        action: String = AppWidgetManager.ACTION_APPWIDGET_UPDATE,
    ) {
        val scheduler = context.getSystemService(JobScheduler::class.java) ?: return

        val job = JobInfo.Builder(jobId, ComponentName(context, WidgetRefreshJobService::class.java))
            .apply { uris.forEach { addTriggerContentUri(JobInfo.TriggerContentUri(it, 0)) } }
            // 잠금과 각도처럼 연달아 바뀌는 값을 한 번으로 묶되 오래 기다리지는 않는다.
            .setTriggerContentUpdateDelay(TriggerUpdateDelayMillis)
            .setTriggerContentMaxDelay(TriggerMaxDelayMillis)
            .setExtras(
                PersistableBundle().apply {
                    putString(ReceiverKey, receiver.name)
                    putString(ActionKey, action)
                },
            )
            // setPersisted 는 콘텐트 트리거와 함께 쓸 수 없다. 재부팅 뒤에는 다음 onUpdate 가 다시 건다.
            .build()

        scheduler.schedule(job)
    }

    fun cancel(context: Context, jobId: Int) {
        context.getSystemService(JobScheduler::class.java)?.cancel(jobId)
    }

    /** 이 앱의 모든 위젯 리시버에 갱신을 보낸다. 권한처럼 URI 알림이 없는 변화 뒤에 쓴다. */
    fun requestUpdateAll(context: Context) {
        val manager = AppWidgetManager.getInstance(context)

        manager.installedProviders
            .filter { it.provider.packageName == context.packageName }
            .forEach { requestUpdate(context, manager, it.provider) }
    }

    internal fun requestUpdate(context: Context, manager: AppWidgetManager, receiver: ComponentName) {
        val ids = manager.getAppWidgetIds(receiver)

        // 위젯이 하나도 없으면 보낼 곳이 없다. 트리거 job 도 여기서 사슬이 끊긴다.
        if (ids.isEmpty()) return

        // APPWIDGET_UPDATE 는 자기 패키지의 컴포넌트를 명시하면 앱도 보낼 수 있다(AMS 의 호환 예외).
        val update = Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE)
            .setComponent(receiver)
            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)

        context.sendBroadcast(update)
    }
}

class WidgetRefreshJobService : JobService() {
    override fun onStartJob(params: JobParameters): Boolean {
        val receiverName = params.extras.getString(ReceiverKey) ?: return false
        val receiver = ComponentName(this, receiverName)
        val action = params.extras.getString(ActionKey) ?: AppWidgetManager.ACTION_APPWIDGET_UPDATE

        if (action == AppWidgetManager.ACTION_APPWIDGET_UPDATE) {
            WidgetRefresh.requestUpdate(this, AppWidgetManager.getInstance(this), receiver)
        } else {
            sendBroadcast(Intent(action).setComponent(receiver))
        }

        return false
    }

    override fun onStopJob(params: JobParameters): Boolean = false
}

private const val ReceiverKey = "receiver"
private const val ActionKey = "action"
private const val TriggerUpdateDelayMillis = 300L
private const val TriggerMaxDelayMillis = 2_000L
