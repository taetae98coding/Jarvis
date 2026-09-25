package io.github.taetae98coding.jarvis.domain.terminal

import kotlinx.coroutines.flow.Flow

/**
 * 코드 파일의 자동완성·선언·사용처(docs/common/terminal-code-navigation.html). 요청마다 지금 글 전체([text])를 넘겨서 서버가
 * 보는 문서가 입력 칸과 어긋나지 않게 한다. 코드 분석이 준비되지 않았거나 비면 텍스트 검색으로 채운다.
 */
interface CodeIntelRepository {
    /** 수집하는 동안 [path] 의 언어 서버를 붙잡고 문서를 연다(N1·N2). cold 다. */
    fun observeAnalysis(path: String): Flow<CodeAnalysisStatus>

    suspend fun complete(path: String, text: String, offset: Int): CodeCompletions

    /** [item] 을 [offset] 에 넣은 결과(C4). */
    suspend fun applyCompletion(path: String, text: String, offset: Int, item: CodeCompletion): CodeEdit

    suspend fun definition(path: String, text: String, offset: Int): CodeLocations

    suspend fun usages(path: String, text: String, offset: Int): CodeLocations
}

class ObserveCodeAnalysisUseCase(
    private val repository: CodeIntelRepository,
) {
    operator fun invoke(path: String): Flow<CodeAnalysisStatus> = repository.observeAnalysis(path)
}

class CompleteCodeUseCase(
    private val repository: CodeIntelRepository,
) {
    suspend operator fun invoke(path: String, text: String, offset: Int): CodeCompletions {
        val language = codeLanguageOf(path) ?: return CodeCompletions(emptyList(), CodeSource.TextSearch)
        val result = repository.complete(path, text, offset)

        return result.copy(items = filterCompletions(result.items, completionPrefix(text, offset, language)))
    }
}

class ApplyCodeCompletionUseCase(
    private val repository: CodeIntelRepository,
) {
    suspend operator fun invoke(path: String, text: String, offset: Int, item: CodeCompletion): CodeEdit =
        repository.applyCompletion(path, text, offset, item)
}

/** ⌘B(G1·G2). 결과가 커서의 식별자 자신이면 선언 위에서 부른 것이라 사용하는 곳을 찾는다. 식별자가 없으면 null. */
class GoToDeclarationUseCase(
    private val repository: CodeIntelRepository,
) {
    suspend operator fun invoke(path: String, text: String, offset: Int): CodeNavigation? {
        val language = codeLanguageOf(path) ?: return null
        val identifier = identifierAt(text, offset, language) ?: return null
        val declarations = repository.definition(path, text, offset)
        val self = positionOf(text, identifier.first)
        val columns = self.column..self.column + (identifier.last - identifier.first)
        val onDeclaration = declarations.locations.isNotEmpty() && declarations.locations.all { location ->
            location.path == path && location.line == self.line && location.column in columns
        }

        return if (onDeclaration) {
            CodeNavigation(CodeNavigationKind.Usages, repository.usages(path, text, offset))
        } else {
            CodeNavigation(CodeNavigationKind.Declaration, declarations)
        }
    }
}

/** ⌥F7(G1). 식별자가 없으면 null. */
class FindUsagesUseCase(
    private val repository: CodeIntelRepository,
) {
    suspend operator fun invoke(path: String, text: String, offset: Int): CodeNavigation? {
        val language = codeLanguageOf(path) ?: return null
        identifierAt(text, offset, language) ?: return null

        return CodeNavigation(CodeNavigationKind.Usages, repository.usages(path, text, offset))
    }
}
