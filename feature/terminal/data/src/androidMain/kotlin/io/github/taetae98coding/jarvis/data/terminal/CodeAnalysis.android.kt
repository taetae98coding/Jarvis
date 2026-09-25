package io.github.taetae98coding.jarvis.data.terminal

import io.github.taetae98coding.jarvis.domain.terminal.CodeIntelRepository
import kotlinx.coroutines.Dispatchers
import okio.FileSystem

// 기기 안에서 언어 서버를 돌릴 수 없어 텍스트 검색만 쓴다(docs/platform/android.html#terminal-code-navigation).
internal actual fun createCodeIntelRepository(): CodeIntelRepository =
    DefaultCodeIntelRepository(UnavailableCodeAnalysisDataSource, TextCodeSearch(FileSystem.SYSTEM, Dispatchers.IO))
