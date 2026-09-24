package io.github.taetae98coding.jarvis.widget.screen

import android.content.Context
import android.content.Intent
import io.github.taetae98coding.jarvis.domain.screen.SetKeepSystemScreenAwakeUseCase
import io.github.taetae98coding.jarvis.domain.screen.SyncSystemScreenAwakeUseCase
import io.github.taetae98coding.jarvis.widget.AsyncReceiver

/** 알림 버튼, 설정 변경 트리거, 사용자의 알림 삭제를 받는다. 위젯의 ToggleSystemScreenAwakeAction 과 같은 경로다. */
class SystemScreenAwakeNotificationReceiver : AsyncReceiver() {
    override suspend fun onReceiveAsync(context: Context, intent: Intent) {
        when (intent.action) {
            ActionSetEnabled -> {
                inject<SetKeepSystemScreenAwakeUseCase>()(intent.getBooleanExtra(ExtraEnabled, false))
                // 시스템에 쓰는 ApplySystemScreenAwakeUseCase 는 앱 루트 ViewModel 스코프에서만 돈다. 여기서 한 번 반영한다.
                inject<SyncSystemScreenAwakeUseCase>()()
            }

            ActionRefresh -> Unit

            ActionDismissed -> {
                SystemScreenAwakeNotification.stopRefreshing(context)

                return
            }

            else -> return
        }

        SystemScreenAwakeNotification.refresh(context)
    }

    companion object {
        const val ActionSetEnabled = "io.github.taetae98coding.jarvis.widget.screen.SET_ENABLED"
        const val ActionRefresh = "io.github.taetae98coding.jarvis.widget.screen.REFRESH"
        const val ActionDismissed = "io.github.taetae98coding.jarvis.widget.screen.DISMISSED"

        const val ExtraEnabled = "enabled"
    }
}
