package io.github.taetae98coding.jarvis.domain.terminal

import kotlinx.coroutines.flow.Flow

interface GitWorktreeRepository {
    /**
     * [directory] 를 담고 있는 워크트리와 그 현재 브랜치. git 저장소 안이 아니거나, 폴더가 없거나, git 을 쓸 수
     * 없는 플랫폼이면 null 이다. 수집하는 동안만 다시 읽는다(cold).
     */
    fun observeWorktree(directory: String): Flow<GitWorktree?>

    /**
     * [repositoryDirectory] 가 속한 저장소에 [branch] 브랜치의 워크트리를 [path] 에 만든다. 브랜치가 없으면
     * [baseBranch](null 이면 지금 HEAD)에서 새로 만들고, 있으면 [baseBranch] 와 무관하게 그 브랜치를 체크아웃한다.
     * 실패는 [GitWorktreeException] 이다.
     */
    suspend fun addWorktree(repositoryDirectory: String, branch: String, path: String, baseBranch: String? = null): Result<GitWorktree>

    /**
     * [directory] 를 담고 있는 워크트리를 저장소에서 떼고, 지금 체크아웃하고 있는 브랜치를 병합 여부와 무관하게 지운다
     * (detached 면 브랜치는 그대로). [deleteDirectory] 면 폴더도 지우되 커밋하지 않은 변경이 있으면 거부하고, 아니면 폴더의
     * 파일을 git 과 무관한 보통 폴더로 남긴다. 워크트리가 아니거나 main 워크트리면 실패다. 실패는 [GitWorktreeException] 이다.
     */
    suspend fun removeWorktree(directory: String, deleteDirectory: Boolean): Result<Unit>
}
