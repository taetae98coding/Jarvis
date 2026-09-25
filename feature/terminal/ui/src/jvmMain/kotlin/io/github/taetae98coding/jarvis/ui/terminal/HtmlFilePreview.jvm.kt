package io.github.taetae98coding.jarvis.ui.terminal

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import io.github.taetae98coding.jarvis.browser.BrowserEngine
import io.github.taetae98coding.jarvis.browser.BrowserSurface
import io.github.taetae98coding.jarvis.designsystem.theme.JarvisTheme
import java.io.File
import java.util.Base64

// 페이지는 파일 탭 id 로 엔진에 둔다. 브라우저 탭과 달리 미리보기가 보이는 동안만 산다(V7).
@Composable
internal actual fun HtmlFilePreview(
    tabId: Long,
    path: String,
    html: String,
    fromDisk: Boolean,
    hidden: Boolean,
    onFocus: () -> Unit,
    modifier: Modifier,
) {
    val url = if (fromDisk) File(path).toPath().toUri().toString() else dataUrl(path, html)
    val page = remember(tabId) { BrowserEngine.page(tabId, url) }
    val failed by page.failed.collectAsStateWithLifecycle()
    var loaded by remember(page) { mutableStateOf(html) }

    DisposableEffect(page) { onDispose { BrowserEngine.close(tabId) } }
    LaunchedEffect(page, html) {
        if (html == loaded) return@LaunchedEffect
        loaded = html
        if (fromDisk) page.reload() else page.load(url)
    }

    if (failed) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text(
                text = "[웹 페이지를 열 수 없습니다]",
                style = JarvisTheme.typography.bodyMedium,
                color = JarvisTheme.colorScheme.onSurfaceVariant,
            )
        }
    } else {
        BrowserSurface(page = page, onFocus = onFocus, modifier = modifier)
    }
}

// JCEF 에는 글을 바로 넣는 CefFrame.loadString 이 없다. 커밋 시점 내용은 디스크에 없으므로 data: 주소로 넘긴다.
private fun dataUrl(path: String, html: String): String =
    "data:${htmlPreviewMimeType(path)};charset=utf-8;base64," + Base64.getEncoder().encodeToString(html.encodeToByteArray())
