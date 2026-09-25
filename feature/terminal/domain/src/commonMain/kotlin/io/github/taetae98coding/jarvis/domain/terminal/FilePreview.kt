package io.github.taetae98coding.jarvis.domain.terminal

/** 파일 탭이 그려 보일 수 있는 형식(docs/common/terminal-file-preview.html V1). */
enum class FilePreview {
    Markdown,

    /** 웹 엔진이 그리는 HTML·SVG. */
    Web,
}

private val PreviewExtensions = mapOf(
    "md" to FilePreview.Markdown,
    "markdown" to FilePreview.Markdown,
    "html" to FilePreview.Web,
    "htm" to FilePreview.Web,
    "xhtml" to FilePreview.Web,
    "svg" to FilePreview.Web,
)

fun filePreviewOf(path: String): FilePreview? =
    PreviewExtensions[path.substringAfterLast('/').substringAfterLast('.', missingDelimiterValue = "").lowercase()]
