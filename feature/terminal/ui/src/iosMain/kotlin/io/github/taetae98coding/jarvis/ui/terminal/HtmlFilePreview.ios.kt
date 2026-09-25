package io.github.taetae98coding.jarvis.ui.terminal

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

// 이 타깃에는 터미널이 없고 웹 페이지도 띄우지 않아(isBrowserSupported = false) 불리지 않는다(V8).
@Composable
internal actual fun HtmlFilePreview(
    tabId: Long,
    path: String,
    html: String,
    fromDisk: Boolean,
    hidden: Boolean,
    onFocus: () -> Unit,
    modifier: Modifier,
) = Unit
