package io.github.taetae98coding.jarvis.ui.terminal

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * HTML·SVG 파일 탭의 웹 미리보기(docs/common/terminal-file-preview.html V5–V9). [fromDisk] 면 [path] 의 `file://` 주소를
 * 열어 상대 경로 자원이 붙고, 아니면(커밋 파일 탭) [html] 을 그린다. [html] 이 바뀌면 다시 그리고, 컴포지션을 떠나면
 * 페이지를 닫는다. [hidden] 은 페이지가 메뉴를 가리는 엔진(Android)만 뜻이 있다.
 */
@Composable
internal expect fun HtmlFilePreview(
    tabId: Long,
    path: String,
    html: String,
    fromDisk: Boolean,
    hidden: Boolean,
    onFocus: () -> Unit,
    modifier: Modifier,
)

internal fun htmlPreviewMimeType(path: String): String =
    if (path.endsWith(".svg", ignoreCase = true)) "image/svg+xml" else "text/html"
