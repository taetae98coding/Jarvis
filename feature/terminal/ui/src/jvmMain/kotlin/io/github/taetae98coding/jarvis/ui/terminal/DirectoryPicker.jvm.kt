package io.github.taetae98coding.jarvis.ui.terminal

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.FileDialog
import java.awt.Frame

// JFileChooser 는 macOS 창처럼 보이지 않아 버렸다(docs/platform/jvm.html#terminal-panel-create).
internal actual val directoryPicker: DirectoryPicker? = DirectoryPicker { initial ->
    // setVisible(true) 는 창이 닫힐 때까지 부른 스레드를 막는다. UI 스레드에서 부르지 않는다.
    withContext(Dispatchers.IO) {
        runCatching {
            val dialog = FileDialog(null as Frame?, "폴더 선택", FileDialog.LOAD)
            initial?.let { dialog.directory = it }
            // macOS 의 CFileDialog 가 창을 띄울 때 이 전역 속성을 읽어 NSOpenPanel 을 폴더 선택으로 연다.
            // 다른 파일 선택 창이 폴더 선택이 되지 않게 닫히면 되돌린다.
            System.setProperty(DirectoriesProperty, "true")
            try {
                dialog.isVisible = true
            } finally {
                System.clearProperty(DirectoriesProperty)
            }
            dialog.files.firstOrNull()?.absolutePath
        }.getOrNull()
    }
}

private const val DirectoriesProperty = "apple.awt.fileDialogForDirectories"
