package io.github.taetae98coding.jarvis.data.terminal

import io.github.taetae98coding.jarvis.domain.terminal.CodeFile
import io.github.taetae98coding.jarvis.domain.terminal.CodeLanguage
import io.github.taetae98coding.jarvis.domain.terminal.CodeLocation
import io.github.taetae98coding.jarvis.domain.terminal.codeLanguageOf
import io.github.taetae98coding.jarvis.domain.terminal.codeProjectRoot
import io.github.taetae98coding.jarvis.domain.terminal.findDeclarations
import io.github.taetae98coding.jarvis.domain.terminal.findUsages
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import okio.FileSystem
import okio.IOException
import okio.Path
import okio.Path.Companion.toPath

/**
 * 프로젝트 루트(N3)를 정하고, 루트 아래 같은 언어 파일에서 이름을 글자로 찾는다(G6). 지금 보고 있는 파일은 디스크 대신
 * 넘겨받은 글(편집 중인 글)로 보고 맨 앞에 둔다.
 */
internal class TextCodeSearch(
    private val fileSystem: FileSystem,
    private val dispatcher: CoroutineDispatcher,
) {
    suspend fun root(path: String, language: CodeLanguage): String =
        withContext(dispatcher) {
            codeProjectRoot(path, language) { directory -> fileSystem.listOrNull(directory.toPath())?.map { it.name }?.toSet() }
        }

    suspend fun declarations(root: String, name: String, language: CodeLanguage, path: String, text: String): List<CodeLocation> =
        withContext(dispatcher) { findDeclarations(files(root, language, CodeFile(path, text)), name, language) }

    suspend fun usages(root: String, name: String, language: CodeLanguage, path: String, text: String): List<CodeLocation> =
        withContext(dispatcher) { findUsages(files(root, language, CodeFile(path, text)), name, language) }

    // 폴더 순서대로 훑는다. okio 메타데이터는 링크를 따라가지 않아서, 폴더를 가리키는 심볼릭 링크는 파일로 보여 들어가지 않는다 — 돌고 도는 링크를 막는다.
    private fun files(root: String, language: CodeLanguage, current: CodeFile): Sequence<CodeFile> =
        sequence {
            yield(current)

            val queue = ArrayDeque<Path>().apply { add(root.toPath()) }
            var seen = 0
            while (queue.isNotEmpty() && seen < TextSearchMaxFiles) {
                val directory = queue.removeFirst()
                val children = fileSystem.listOrNull(directory)?.sortedBy { it.name } ?: continue
                for (child in children) {
                    val metadata = fileSystem.metadataOrNull(child) ?: continue
                    if (metadata.isDirectory) {
                        if (child.name !in SkippedDirectories) queue.add(child)
                        continue
                    }
                    if (codeLanguageOf(child.name) != language || child.toString() == current.path) continue
                    if ((metadata.size ?: 0) > TextSearchMaxBytes) continue

                    seen++
                    val text = try {
                        fileSystem.read(child) { readUtf8() }
                    } catch (_: IOException) {
                        continue
                    }
                    yield(CodeFile(child.toString(), text))
                    if (seen >= TextSearchMaxFiles) break
                }
            }
        }

    private companion object {
        val SkippedDirectories = setOf("build", ".build", ".gradle", ".git", ".idea", "node_modules", "DerivedData", "Pods", ".kotlin")

        const val TextSearchMaxFiles = 20_000

        const val TextSearchMaxBytes = 1024L * 1024
    }
}
