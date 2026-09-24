package io.github.taetae98coding.jarvis.ui.terminal

/** 폴더 선택 창. 고른 폴더의 절대 경로, 취소하면 null 이다. [initial] 이 있으면 그 폴더에서 연다. */
internal fun interface DirectoryPicker {
    suspend fun pick(initial: String?): String?
}

/** null 이면 이 타깃에는 셸이 들어갈 경로를 주는 폴더 선택 창이 없다. 경로를 직접 입력받는다. */
internal expect val directoryPicker: DirectoryPicker?
