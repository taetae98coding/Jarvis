package io.github.taetae98coding.jarvis.data.terminal

import io.github.taetae98coding.jarvis.domain.terminal.GitWorktree
import io.github.taetae98coding.jarvis.domain.terminal.GitWorktreeException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

internal interface GitDataSource {
    fun observeWorktree(directory: String): Flow<GitWorktree?>

    suspend fun addWorktree(repositoryDirectory: String, branch: String, path: String): Result<GitWorktree>
}

/** git 을 실행할 수 없는 타깃. 어떤 폴더도 저장소가 아니라서 패널에 + 가 없다. */
internal object UnsupportedGitDataSource : GitDataSource {
    override fun observeWorktree(directory: String): Flow<GitWorktree?> = flowOf(null)

    override suspend fun addWorktree(repositoryDirectory: String, branch: String, path: String): Result<GitWorktree> =
        Result.failure(GitWorktreeException("이 플랫폼에서는 git 을 쓸 수 없습니다"))
}

/**
 * `git` 을 띄운다. JVM 만 실제로 띄우고 Android(기기에 git 이 없다)·iOS·Web 은 [UnsupportedGitDataSource] 다.
 * 판정 근거는 docs/common/terminal-worktree.html#platforms 에 있다.
 */
internal expect fun createGitDataSource(): GitDataSource
