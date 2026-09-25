package io.github.taetae98coding.jarvis.ui.terminal

import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.kdroidfilter.webview.web.WebView
import io.github.kdroidfilter.webview.web.rememberWebViewNavigator
import io.github.kdroidfilter.webview.web.rememberWebViewState
import io.github.kdroidfilter.webview.web.rememberWebViewStateWithHTMLData
import java.io.File

// 웹뷰는 이 컴포지션에만 있어 미리보기가 가려지면 버린다(V7).
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
    val file = File(path)
    val state = if (fromDisk) {
        rememberWebViewState(Uri.fromFile(file).toString())
    } else {
        rememberWebViewStateWithHTMLData(
            data = html,
            baseUrl = Uri.fromFile(file.parentFile ?: file).toString() + "/",
            mimeType = htmlPreviewMimeType(path),
        )
    }
    // API 30 부터 기본값이 false 라, 켜지 않으면 file:// 문서와 그 폴더의 CSS·이미지를 읽지 못한다. 읽는 곳은 앱 샌드박스뿐이다.
    state.webSettings.androidWebSettings.allowFileAccess = true
    val navigator = rememberWebViewNavigator()
    var loaded by remember { mutableStateOf(html) }

    // 파일 주소로 연 웹뷰는 파일이 바뀐 것을 모른다. 파일 탭이 새 내용을 읽으면 다시 부른다(V6).
    LaunchedEffect(html) {
        if (html == loaded) return@LaunchedEffect
        loaded = html
        if (fromDisk) navigator.reload()
    }

    // 페이지를 컴포지션에서 빼면 웹뷰가 닫히고 다시 열린다. 메뉴가 떠 있는 동안은 크기만 0 으로 줄인다(V9).
    Box(modifier = modifier) {
        WebView(
            state = state,
            navigator = navigator,
            modifier = if (hidden) Modifier.size(0.dp) else Modifier.fillMaxSize(),
        )
    }
}
