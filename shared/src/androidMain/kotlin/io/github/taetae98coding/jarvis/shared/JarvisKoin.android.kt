package io.github.taetae98coding.jarvis.shared

import android.content.Context
import io.github.taetae98coding.jarvis.data.PlatformContext

/**
 * Android 만 인자가 있다. `SharedPreferences` 와 `Settings.System` 이 `Context` 를 요구한다.
 *
 * 리포지토리는 앱 수명 동안 사는 single 이라 Activity 컨텍스트를 넘기면 그만큼 붙들려 있는다.
 */
fun startJarvisKoin(context: Context) {
    startJarvisKoinWith(PlatformContext(context.applicationContext))
}
