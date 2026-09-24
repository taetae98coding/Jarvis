package io.github.taetae98coding.jarvis.domain.terminal

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/** 폴더마다 정해 둔 워크트리를 답하고, 만들기·지우기 요청을 기록한다. [failure] 가 있으면 둘 다 그것으로 실패한다. */
internal class RecordingGitWorktreeRepository(
    private val worktrees: Map<String, GitWorktree> = emptyMap(),
    private val failure: String? = null,
) : GitWorktreeRepository {
    class Added(val repositoryDirectory: String, val branch: String, val path: String, val baseBranch: String?)

    class Removed(val directory: String, val deleteDirectory: Boolean)

    val added = mutableListOf<Added>()

    val removed = mutableListOf<Removed>()

    override fun observeWorktree(directory: String): Flow<GitWorktree?> = flowOf(worktrees[directory])

    override suspend fun addWorktree(repositoryDirectory: String, branch: String, path: String, baseBranch: String?): Result<GitWorktree> {
        added += Added(repositoryDirectory, branch, path, baseBranch)
        if (failure != null) return Result.failure(GitWorktreeException(failure))

        val main = worktrees[repositoryDirectory]?.mainPath ?: repositoryDirectory
        return Result.success(GitWorktree(path = path, mainPath = main, branch = branch))
    }

    override suspend fun removeWorktree(directory: String, deleteDirectory: Boolean): Result<Unit> {
        removed += Removed(directory, deleteDirectory)
        return if (failure != null) Result.failure(GitWorktreeException(failure)) else Result.success(Unit)
    }
}
