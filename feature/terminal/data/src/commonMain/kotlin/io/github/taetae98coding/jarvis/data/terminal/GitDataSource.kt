package io.github.taetae98coding.jarvis.data.terminal

import io.github.taetae98coding.jarvis.domain.terminal.GitBranch
import io.github.taetae98coding.jarvis.domain.terminal.GitChange
import io.github.taetae98coding.jarvis.domain.terminal.GitCommitFile
import io.github.taetae98coding.jarvis.domain.terminal.GitFileDiff
import io.github.taetae98coding.jarvis.domain.terminal.GitGraphLine
import io.github.taetae98coding.jarvis.domain.terminal.GitPushTarget
import io.github.taetae98coding.jarvis.domain.terminal.GitStatus
import io.github.taetae98coding.jarvis.domain.terminal.GitWorktree
import io.github.taetae98coding.jarvis.domain.terminal.GitWorktreeException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

internal interface GitDataSource {
    fun observeWorktree(directory: String): Flow<GitWorktree?>

    fun observeBranches(directory: String): Flow<List<GitBranch>>

    suspend fun addWorktree(repositoryDirectory: String, branch: String, path: String, baseBranch: String?): Result<GitWorktree>

    suspend fun removeWorktree(directory: String, deleteDirectory: Boolean): Result<Unit>

    fun observeStatus(directory: String): Flow<GitStatus?>

    fun observeGraph(directory: String): Flow<List<GitGraphLine>>

    fun observeCommitFiles(directory: String, hash: String): Flow<List<GitChange>?>

    fun observeCommitFile(path: String, hash: String): Flow<GitCommitFile?>

    fun observeFileDiff(path: String): Flow<GitFileDiff?>

    suspend fun stage(root: String, changes: List<GitChange>): Result<Unit>

    suspend fun unstage(root: String, changes: List<GitChange>): Result<Unit>

    suspend fun push(root: String, target: GitPushTarget): Result<Unit>
}

/** git 을 실행할 수 없는 타깃. 어떤 폴더도 저장소가 아니라서 패널에 + 가 없고 사이드 바의 Git 구획은 "git 저장소가 아닙니다", 파일 탭에는 diff 가 없다. */
internal object UnsupportedGitDataSource : GitDataSource {
    override fun observeWorktree(directory: String): Flow<GitWorktree?> = flowOf(null)

    override fun observeBranches(directory: String): Flow<List<GitBranch>> = flowOf(emptyList())

    override suspend fun addWorktree(repositoryDirectory: String, branch: String, path: String, baseBranch: String?): Result<GitWorktree> =
        unsupported()

    override suspend fun removeWorktree(directory: String, deleteDirectory: Boolean): Result<Unit> = unsupported()

    override fun observeStatus(directory: String): Flow<GitStatus?> = flowOf(null)

    override fun observeGraph(directory: String): Flow<List<GitGraphLine>> = flowOf(emptyList())

    override fun observeCommitFiles(directory: String, hash: String): Flow<List<GitChange>?> = flowOf(null)

    override fun observeCommitFile(path: String, hash: String): Flow<GitCommitFile?> = flowOf(null)

    override fun observeFileDiff(path: String): Flow<GitFileDiff?> = flowOf(null)

    override suspend fun stage(root: String, changes: List<GitChange>): Result<Unit> = unsupported()

    override suspend fun unstage(root: String, changes: List<GitChange>): Result<Unit> = unsupported()

    override suspend fun push(root: String, target: GitPushTarget): Result<Unit> = unsupported()

    private fun <T> unsupported(): Result<T> = Result.failure(GitWorktreeException("이 플랫폼에서는 git 을 쓸 수 없습니다"))
}

/**
 * `git` 을 띄운다. JVM 만 실제로 띄우고 Android(기기에 git 이 없다)·iOS·Web 은 [UnsupportedGitDataSource] 다.
 * 판정 근거는 docs/common/terminal-worktree.html#platforms 에 있다.
 */
internal expect fun createGitDataSource(): GitDataSource
