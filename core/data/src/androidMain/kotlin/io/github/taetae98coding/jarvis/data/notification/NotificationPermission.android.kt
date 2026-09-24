package io.github.taetae98coding.jarvis.data.notification

import android.app.Application
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import io.github.taetae98coding.jarvis.data.PlatformContext
import io.github.taetae98coding.jarvis.data.state.activityResumes
import io.github.taetae98coding.jarvis.data.state.observeSystemState
import kotlinx.coroutines.flow.Flow
import kotlin.time.Duration.Companion.seconds

actual fun createNotificationPermission(context: PlatformContext): NotificationPermission =
    PostNotificationsPermission(context.context)

/**
 * `POST_NOTIFICATIONS` 허용 여부. minSdk 33 이라 항상 런타임 권한이다.
 *
 * 런타임 권한 변화에는 시스템 콜백이 없다. 사용자가 다이얼로그나 앱 알림 설정에서 돌아오면 Activity 가
 * 다시 resume 되므로 그것을 신호로 쓰고, Application 을 얻지 못하면 폴링으로 대체한다.
 */
private class PostNotificationsPermission(private val context: Context) : NotificationPermission {
    private val manager: NotificationManager? = context.getSystemService(NotificationManager::class.java)

    override val supported: Boolean = true

    override fun readPermitted(): Boolean = manager?.areNotificationsEnabled() == true

    override fun observePermitted(): Flow<Boolean> {
        val application = context.applicationContext as? Application

        return observeSystemState(
            signals = application?.let(::activityResumes),
            interval = PostNotificationsPollInterval,
        ) { readPermitted() }
    }

    override fun requestPermission() {
        if (readPermitted()) return

        // data 계층은 애플리케이션 컨텍스트만 갖는다. 런타임 권한 다이얼로그는 Activity 가 불러야 해서
        // 화면 없는 Activity 를 새 태스크로 띄운다. 카드 스위치를 켤 때만 오는 요청이라 앱이 포그라운드다.
        val intent = Intent(context, PostNotificationsPermissionActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        runCatching { context.startActivity(intent) }
    }
}

// Application 을 얻지 못했을 때만 쓴다. 사용자가 다이얼로그에서 허용한 것을 알아채야 한다.
private val PostNotificationsPollInterval = 2.seconds
