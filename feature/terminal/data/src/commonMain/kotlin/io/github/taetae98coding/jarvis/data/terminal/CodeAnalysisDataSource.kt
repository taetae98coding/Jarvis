package io.github.taetae98coding.jarvis.data.terminal

import io.github.taetae98coding.jarvis.domain.terminal.CodeAnalysisStatus
import io.github.taetae98coding.jarvis.domain.terminal.CodeCompletion
import io.github.taetae98coding.jarvis.domain.terminal.CodeCompletions
import io.github.taetae98coding.jarvis.domain.terminal.CodeEdit
import io.github.taetae98coding.jarvis.domain.terminal.CodeIntelRepository
import io.github.taetae98coding.jarvis.domain.terminal.CodeLanguage
import io.github.taetae98coding.jarvis.domain.terminal.CodeLocation
import io.github.taetae98coding.jarvis.domain.terminal.CodeLocations
import io.github.taetae98coding.jarvis.domain.terminal.CodeSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * 언어 서버가 주는 코드 분석(docs/common/terminal-code-navigation.html). 요청이 null 이면 서버가 없거나 준비되지 않았거나
 * 시간을 넘긴 것이라 저장소가 텍스트 검색으로 채운다.
 */
internal interface CodeAnalysisDataSource {
    /** 수집하는 동안 ([language], [root]) 의 서버를 붙잡고, 끝나면 [path] 문서를 닫는다. */
    fun observe(language: CodeLanguage, root: String, path: String): Flow<CodeAnalysisStatus>

    suspend fun complete(language: CodeLanguage, root: String, path: String, text: String, offset: Int): List<CodeCompletion>?

    suspend fun applyCompletion(language: CodeLanguage, root: String, path: String, text: String, offset: Int, item: CodeCompletion): CodeEdit?

    suspend fun definition(language: CodeLanguage, root: String, path: String, text: String, offset: Int): AnalysisLocations?

    suspend fun usages(language: CodeLanguage, root: String, path: String, text: String, offset: Int): AnalysisLocations?
}

/** [skipped] 는 파일이 아니라서(라이브러리 안) 뺀 결과 수다. */
internal data class AnalysisLocations(val locations: List<CodeLocation>, val skipped: Int)

/** 언어 서버를 돌릴 수 없는 타깃. 텍스트 검색만 쓴다. */
internal object UnavailableCodeAnalysisDataSource : CodeAnalysisDataSource {
    override fun observe(language: CodeLanguage, root: String, path: String): Flow<CodeAnalysisStatus> = flowOf(CodeAnalysisStatus.Unavailable(null))

    override suspend fun complete(language: CodeLanguage, root: String, path: String, text: String, offset: Int): List<CodeCompletion>? = null

    override suspend fun applyCompletion(language: CodeLanguage, root: String, path: String, text: String, offset: Int, item: CodeCompletion): CodeEdit? = null

    override suspend fun definition(language: CodeLanguage, root: String, path: String, text: String, offset: Int): AnalysisLocations? = null

    override suspend fun usages(language: CodeLanguage, root: String, path: String, text: String, offset: Int): AnalysisLocations? = null
}

/** 판정 근거는 docs/common/terminal-code-navigation.html#platforms 에 있다. */
internal expect fun createCodeIntelRepository(): CodeIntelRepository

/** 파일 탭을 열 수 없는 타깃(터미널 화면에 들어갈 수 없는 iOS·Web). */
internal object UnsupportedCodeIntelRepository : CodeIntelRepository {
    override fun observeAnalysis(path: String): Flow<CodeAnalysisStatus> = flowOf(CodeAnalysisStatus.Unavailable(null))

    override suspend fun complete(path: String, text: String, offset: Int): CodeCompletions = CodeCompletions(emptyList(), CodeSource.TextSearch)

    override suspend fun applyCompletion(path: String, text: String, offset: Int, item: CodeCompletion): CodeEdit = CodeEdit(text, offset)

    override suspend fun definition(path: String, text: String, offset: Int): CodeLocations = CodeLocations(emptyList(), CodeSource.TextSearch)

    override suspend fun usages(path: String, text: String, offset: Int): CodeLocations = CodeLocations(emptyList(), CodeSource.TextSearch)
}
