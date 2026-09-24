package io.github.taetae98coding.jarvis.widget

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.lifecycle.Lifecycle
import io.github.taetae98coding.jarvis.core.widget.R
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch

/**
 * 알림 창에 고정되는 컨트롤 하나. 기능 모듈이 구현하고 [NotificationWidgets.syncWhileStarted] 가 돌린다.
 */
interface NotificationWidget {
    /**
     * 앱이 포그라운드인 동안 수집된다. "알림에 고정" 설정과 기능 상태를 따라 알림을 올리고 내린다.
     * 첫 방출이 곧 onStart 재게시다. 수집이 취소되어도 알림은 남는다. 앱을 떠났다고 알림을 내리는
     * 것이 아니라서다.
     */
    suspend fun sync(context: Context)
}

object NotificationWidgets {
    /** 두 기능이 한 채널을 쓴다. 낮은 중요도라 소리·진동·헤드업이 없다. */
    const val ChannelId = "notification_widget"

    fun syncWhileStarted(context: Context, widgets: List<NotificationWidget>) {
        val application = context.applicationContext
        val owner = ProcessLifecycleOwner.get()

        owner.lifecycleScope.launch {
            // STARTED 를 벗어나면 안의 수집이 전부 취소되고, 다시 STARTED 가 되면 처음부터 다시 돈다.
            // 그래서 Android 14+ 에서 사용자가 지운 알림이 다음 onStart 에 돌아온다.
            owner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                widgets.forEach { widget -> launch { widget.sync(application) } }
            }
        }
    }

    /** notify 직전에 부른다. 이미 있으면 시스템이 무시한다. */
    fun ensureChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return

        val channel = NotificationChannel(
            ChannelId,
            context.getString(R.string.notification_widget_channel_name),
            NotificationManager.IMPORTANCE_LOW,
        )

        manager.createNotificationChannel(channel)
    }
}
