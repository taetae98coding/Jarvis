package io.github.taetae98coding.jarvis.widget

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings

/**
 * 권한이 없을 때 위젯 버튼이 여는 투명 Activity. 화면은 없고 곧 끝난다.
 *
 * 설정 인텐트를 위젯 버튼에 곧바로 걸면, 사용자가 허용하고 돌아온 뒤에도 그려진 버튼이 낡아 있어
 * 다시 설정으로 보낸다. 권한 변화는 URI 알림이 아니라 설정 변경 트리거로도 못 잡는다. 그래서 탭
 * 시점에 여기서 권한을 다시 본다. 이미 허용됐으면 위젯을 다시 그리게 하고, 아니면 설정 화면을 연다.
 */
class WriteSettingsPermissionActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Settings.System.canWrite(this)) {
            WidgetRefresh.requestUpdateAll(this)
        } else {
            val settings = Intent(
                Settings.ACTION_MANAGE_WRITE_SETTINGS,
                Uri.fromParts("package", packageName, null),
            )

            // 설정 화면에서 돌아오면 런처가 보이도록 이 Activity 를 백스택에 남기지 않는다(noHistory).
            runCatching { startActivity(settings) }
        }

        finish()
    }
}
