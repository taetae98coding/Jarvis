package io.github.taetae98coding.jarvis.data.notification

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings

/**
 * `POST_NOTIFICATIONS` 다이얼로그를 띄우는 투명 Activity. 화면은 없고 결과가 오면 끝난다.
 *
 * 두 번 거부하면 시스템이 다이얼로그를 더 보여주지 않고 곧바로 거부 결과를 돌려준다. 그때는
 * 사용자가 직접 켤 수 있는 앱 알림 설정 화면을 연다.
 */
class PostNotificationsPermissionActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState == null) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), RequestCode)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        val denied = grantResults.firstOrNull() != PackageManager.PERMISSION_GRANTED

        if (denied && !shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
            val settings = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, packageName)

            runCatching { startActivity(settings) }
        }

        finish()
    }
}

private const val RequestCode = 0x4A50
