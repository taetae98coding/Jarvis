package io.github.taetae98coding.jarvis.data.state

import android.app.Activity
import android.app.Application
import android.os.Bundle
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * 어떤 Activity 든 다시 resume 될 때마다 신호를 낸다.
 *
 * 사용자가 설정 앱이나 권한 다이얼로그에서 돌아온 순간이다. 시스템이 콜백을 주지 않는 권한
 * (`POST_NOTIFICATIONS`)의 유일한 신호이고, 콜백이 있는 권한(`WRITE_SETTINGS`)에서는 제조사 ROM 이
 * 콜백을 빠뜨려도 돌아오는 순간에는 따라가게 하는 안전망이다.
 */
internal fun activityResumes(application: Application): Flow<Unit> =
    callbackFlow {
        val callbacks = object : Application.ActivityLifecycleCallbacks {
            override fun onActivityResumed(activity: Activity) {
                trySend(Unit)
            }

            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit

            override fun onActivityStarted(activity: Activity) = Unit

            override fun onActivityPaused(activity: Activity) = Unit

            override fun onActivityStopped(activity: Activity) = Unit

            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit

            override fun onActivityDestroyed(activity: Activity) = Unit
        }

        application.registerActivityLifecycleCallbacks(callbacks)

        awaitClose { application.unregisterActivityLifecycleCallbacks(callbacks) }
    }
