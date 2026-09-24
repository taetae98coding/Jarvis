package io.github.taetae98coding.jarvis.domain.terminal

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/** 폴더마다 정해 둔 워크트리를 답하고, 만들기 요청을 기록한다. [failure] 가 있으면 만들기가 그것으로 실패한다. */
internal class RecordingGitWorktreeRepository(
    private val worktrees: Map<String, GitWorktree> = emptyMap(),
    private val failure: String? = null,
) : GitWorktreeRepository {
    class Added(val repositoryDirectory: String, val branch: String, val path: String)

    val added = mutableListOf<Added>()

    override fun observeWorktree(directory: String): Flow<GitWorktree?> = flowOf(worktrees[directory])

    override suspend fun addWorktree(repositoryDirectory: String, branch: String, path: String): Result<GitWorktree> {
        added += Added(repositoryDirectory, branch, path)
        if (failure != null) return Result.failure(GitWorktreeException(failure))

        val main = worktrees[repositoryDirectory]?.mainPath ?: repositoryDirectory
        return Result.success(GitWorktree(path = path, mainPath = main))
    }
}
