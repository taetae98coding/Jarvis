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
import io.github.taetae98coding.jarvis.domain.terminal.CodeTextEdit
import io.github.taetae98coding.jarvis.domain.terminal.codeLanguageOf
import io.github.taetae98coding.jarvis.domain.terminal.completionPrefix
import io.github.taetae98coding.jarvis.domain.terminal.cursorAfterEdits
import io.github.taetae98coding.jarvis.domain.terminal.identifierAt
import io.github.taetae98coding.jarvis.domain.terminal.wordCompletions
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf

/** 코드 분석에 먼저 묻고, 준비되지 않았거나 비었으면 텍스트 검색으로 채운다(C5·G6). */
internal class DefaultCodeIntelRepository(
    private val analysis: CodeAnalysisDataSource,
    private val search: TextCodeSearch,
) : CodeIntelRepository {
    override fun observeAnalysis(path: String): Flow<CodeAnalysisStatus> {
        val language = codeLanguageOf(path) ?: return flowOf(CodeAnalysisStatus.Unavailable(null))

        return flow { emitAll(analysis.observe(language, search.root(path, language), path)) }
    }

    override suspend fun complete(path: String, text: String, offset: Int): CodeCompletions {
        val language = codeLanguageOf(path) ?: return CodeCompletions(emptyList(), CodeSource.TextSearch)
        val analyzed = analysis.complete(language, search.root(path, language), path, text, offset)
        if (!analyzed.isNullOrEmpty()) return CodeCompletions(analyzed, CodeSource.Analysis)

        return CodeCompletions(wordCompletions(text, offset, language), CodeSource.TextSearch)
    }

    override suspend fun applyCompletion(path: String, text: String, offset: Int, item: CodeCompletion): CodeEdit {
        val language = codeLanguageOf(path) ?: return CodeEdit(text, offset)
        if (item.data != null) {
            analysis.applyCompletion(language, search.root(path, language), path, text, offset, item)?.let { return it }
        }

        val prefix = completionPrefix(text, offset, language)
        val edit = CodeTextEdit(offset - prefix.length, offset, item.label)

        return CodeEdit(text.replaceRange(edit.start, edit.end, edit.text), cursorAfterEdits(edit, emptyList()))
    }

    override suspend fun definition(path: String, text: String, offset: Int): CodeLocations =
        locate(path, text, offset, analysis::definition) { root, name, language -> search.declarations(root, name, language, path, text) }

    override suspend fun usages(path: String, text: String, offset: Int): CodeLocations =
        locate(path, text, offset, analysis::usages) { root, name, language -> search.usages(root, name, language, path, text) }

    private suspend fun locate(
        path: String,
        text: String,
        offset: Int,
        analyze: suspend (CodeLanguage, String, String, String, Int) -> AnalysisLocations?,
        fallback: suspend (root: String, name: String, CodeLanguage) -> List<CodeLocation>,
    ): CodeLocations {
        val language = codeLanguageOf(path) ?: return CodeLocations(emptyList(), CodeSource.TextSearch)
        val identifier = identifierAt(text, offset, language) ?: return CodeLocations(emptyList(), CodeSource.TextSearch)
        val root = search.root(path, language)
        val analyzed = analyze(language, root, path, text, offset)
        if (analyzed != null && analyzed.locations.isNotEmpty()) return CodeLocations(analyzed.locations, CodeSource.Analysis)

        val found = fallback(root, text.substring(identifier), language)
        if (found.isEmpty() && analyzed != null && analyzed.skipped > 0) return CodeLocations(emptyList(), CodeSource.Analysis, libraryOnly = true)

        return CodeLocations(found, CodeSource.TextSearch)
    }
}
