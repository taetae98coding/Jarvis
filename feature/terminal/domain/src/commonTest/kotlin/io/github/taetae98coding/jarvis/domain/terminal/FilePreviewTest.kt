package io.github.taetae98coding.jarvis.domain.terminal

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class FilePreviewTest {
    @Test
    fun previewComesFromTheExtension() {
        assertEquals(FilePreview.Markdown, filePreviewOf("/a/README.md"))
        assertEquals(FilePreview.Markdown, filePreviewOf("/a/notes.markdown"))
        assertEquals(FilePreview.Web, filePreviewOf("/a/index.html"))
        assertEquals(FilePreview.Web, filePreviewOf("/a/old.htm"))
        assertEquals(FilePreview.Web, filePreviewOf("/a/page.xhtml"))
        assertEquals(FilePreview.Web, filePreviewOf("/a/icon.svg"))
    }

    @Test
    fun extensionIsCaseInsensitive() {
        assertEquals(FilePreview.Markdown, filePreviewOf("/a/README.MD"))
        assertEquals(FilePreview.Web, filePreviewOf("/a/INDEX.Html"))
    }

    @Test
    fun otherFilesHaveNoPreview() {
        assertNull(filePreviewOf("/a/Main.kt"))
        assertNull(filePreviewOf("/a/layout.xml"))
        assertNull(filePreviewOf("/a/Makefile"))
        assertNull(filePreviewOf("/a.html/Makefile"))
    }
}
