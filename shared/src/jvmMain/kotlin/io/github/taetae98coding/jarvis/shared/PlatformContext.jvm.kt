package io.github.taetae98coding.jarvis.shared

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import io.github.taetae98coding.jarvis.data.PlatformContext

@Composable
internal actual fun rememberPlatformContext(): PlatformContext = remember { PlatformContext() }
