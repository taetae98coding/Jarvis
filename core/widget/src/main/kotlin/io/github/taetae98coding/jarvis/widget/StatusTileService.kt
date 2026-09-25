package io.github.taetae98coding.jarvis.widget

import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * 보이는 동안(onStartListening~onStopListening)만 [observe] 를 수집해 타일을 그리는 빠른 설정 타일.
 *
 * 탭은 마지막으로 그린 상태로 [onClick] 을 부른다. 탭 처리는 리스닝 스코프가 아니라 서비스 스코프에서
 * 돌린다. 패널이 닫히며 onStopListening 이 오면 리스닝 스코프가 취소되는데, 그때 유스케이스가 중간에
 * 끊기면 잠금만 걸리고 각도는 안 바뀌는 식의 반쪽 결과가 남는다.
 */
abstract class StatusTileService<T : Any> : TileService() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var listening: Job? = null
    private var latest: T? = null

    protected abstract fun observe(): kotlinx.coroutines.flow.Flow<T>

    protected abstract fun Tile.render(status: T)

    protected abstract suspend fun onClick(status: T)

    override fun onStartListening() {
        super.onStartListening()

        listening?.cancel()
        listening = serviceScope.launch {
            observe().collect { status ->
                latest = status
                qsTile?.apply {
                    render(status)
                    updateTile()
                }
            }
        }
    }

    override fun onStopListening() {
        listening?.cancel()
        listening = null

        super.onStopListening()
    }

    override fun onClick() {
        super.onClick()

        val status = latest ?: return

        serviceScope.launch { onClick(status) }
    }

    override fun onDestroy() {
        serviceScope.cancel()

        super.onDestroy()
    }

    /** 권한이 없을 때 권한 중계 Activity 를 열고 패널을 접는다. */
    protected fun openWriteSettingsPermission() {
        val intent = Intent(this, WriteSettingsPermissionActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val pendingIntent = PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )

            startActivityAndCollapse(pendingIntent)
        } else {
            // targetSdk 34+ 앱이 API 34 기기에서 이 판을 부르면 UnsupportedOperationException 이다. 33 에서만 쓴다.
            // lint 는 SDK_INT 분기를 보지 못하고 자기 검사 ID 로만 억제된다.
            @Suppress("DEPRECATION", "StartActivityAndCollapseDeprecated")
            startActivityAndCollapse(intent)
        }
    }
}
