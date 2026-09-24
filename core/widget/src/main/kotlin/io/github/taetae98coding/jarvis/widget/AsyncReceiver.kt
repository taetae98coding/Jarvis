package io.github.taetae98coding.jarvis.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * suspend 유스케이스를 부르는 리시버. Glance 의 ActionCallbackBroadcastReceiver 가 하는 것과 같이
 * goAsync 로 결과를 붙잡고 코루틴이 끝나면 finish 한다. 시스템이 주는 시간은 약 10초다.
 */
abstract class AsyncReceiver : BroadcastReceiver() {
    protected abstract suspend fun onReceiveAsync(context: Context, intent: Intent)

    final override fun onReceive(context: Context, intent: Intent) {
        val result = goAsync()

        ReceiverScope.launch {
            try {
                onReceiveAsync(context.applicationContext, intent)
            } finally {
                result.finish()
            }
        }
    }
}

// 리시버 인스턴스는 onReceive 마다 새로 만들어지므로 스코프는 프로세스 수명으로 둔다.
private val ReceiverScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
