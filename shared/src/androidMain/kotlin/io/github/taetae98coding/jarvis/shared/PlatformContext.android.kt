package io.github.taetae98coding.jarvis.shared

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import io.github.taetae98coding.jarvis.data.PlatformContext

@Composable
internal actual fun rememberPlatformContext(): PlatformContext {
    val context = LocalContext.current

    // 리포지토리는 컴포지션보다 오래 살 수 있다. Activity 컨텍스트를 넘기면 그만큼 붙들려 있는다.
    return remember(context) { PlatformContext(context.applicationContext) }
}
