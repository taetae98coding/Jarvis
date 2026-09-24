package io.github.taetae98coding.jarvis.data.state

import android.app.AppOpsManager
import android.app.Application
import android.content.Context
import android.provider.Settings
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.merge
import kotlin.time.Duration.Companion.seconds

/**
 * `WRITE_SETTINGS` 허용 여부. 전역 화면 유지와 화면 회전이 같은 권한을 본다.
 *
 * 권한은 설정 앱에서만 바뀐다. appop 가 바뀌면 AppOpsManager 가 알려주고, 사용자가 설정 앱에서 돌아오면
 * Activity 가 다시 resume 된다. 뒤의 것은 제조사 ROM 이 appop 알림을 빠뜨려도 돌아오는 순간에는
 * 따라가게 하는 안전망이다.
 */
fun observeWriteSettingsPermission(context: Context): Flow<Boolean> =
    observeSystemState(signals = writeSettingsChanges(context), interval = WriteSettingsPollInterval) {
        Settings.System.canWrite(context)
    }

private fun writeSettingsChanges(context: Context): Flow<Unit>? {
    val appOps = context.getSystemService(AppOpsManager::class.java) ?: return null

    val opChanges = callbackFlow {
        val listener = AppOpsManager.OnOpChangedListener { _, _ -> trySend(Unit) }

        appOps.startWatchingMode(AppOpsManager.OPSTR_WRITE_SETTINGS, context.packageName, listener)

        awaitClose { appOps.stopWatchingMode(listener) }
    }

    val application = context.applicationContext as? Application ?: return opChanges

    return merge(opChanges, activityResumes(application))
}

// AppOpsManager 를 얻지 못했을 때만 쓴다. 사용자가 설정 화면에서 허용하고 돌아온 것을 알아채야 한다.
private val WriteSettingsPollInterval = 2.seconds
