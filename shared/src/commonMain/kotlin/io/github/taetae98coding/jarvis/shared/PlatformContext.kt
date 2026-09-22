package io.github.taetae98coding.jarvis.shared

import androidx.compose.runtime.Composable
import io.github.taetae98coding.jarvis.data.PlatformContext

/**
 * :data 에 넘길 플랫폼 핸들을 얻는다.
 *
 * 플랫폼 API 를 부르는 코드는 :data 에 둔다는 규칙의 예외다. Android 의 `Context` 는 Compose 트리의
 * `LocalContext` 로만 얻을 수 있고, 그것 하나 때문에 :data 에 Compose 를 들이지는 않는다.
 */
@Composable
internal expect fun rememberPlatformContext(): PlatformContext
