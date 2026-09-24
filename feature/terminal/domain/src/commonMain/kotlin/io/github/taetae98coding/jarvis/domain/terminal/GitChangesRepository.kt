package io.github.taetae98coding.jarvis.domain.terminal

import kotlinx.coroutines.flow.Flow

/** `git status` 의 한 글자. porcelain 의 X(index)·Y(작업 트리) 자리에 오는 값이다. */
enum class GitChangeKind(val symbol: Char) {
    Modified('M'),
    Added('A'),
    Deleted('D'),
    Renamed('R'),
    Copied('C'),
    TypeChanged('T'),
    Conflicted('U'),
    Untracked('?'),
}

/** [path] 는 저장소 최상위 기준이다. [originalPath] 는 이름 바꿈·복사의 옛 경로다. */
data class GitChange(
    val path: String,
    val kind: GitChangeKind,
    val originalPath: String? = null,
)

/**
 * [root] 는 저장소(워크트리) 최상위 폴더다. [branch] 는 지금 브랜치로, HEAD 가 떨어져 있으면 null 이다.
 * [staged] 는 index 쪽, [unstaged] 는 작업 트리 쪽 변경과 추적하지 않는 파일이다. 둘 다 경로 순이다.
 */
data class GitStatus(
    val root: String,
    val branch: String?,
    val staged: List<GitChange>,
    val unstaged: List<GitChange>,
)

/**
 * `git log --graph` 의 한 줄. 커밋마다 두 줄이다 — 제목 줄과, [isDetail] 인 작성자·날짜·해시 줄. 둘째 줄의 [graph] 는
 * git 이 그린 이음선이다. [commit] 이 null 이면 선만 있는 줄이다.
 */
data class GitGraphLine(
    val graph: String,
    val commit: GitCommit? = null,
    val isDetail: Boolean = false,
)

/** [refs] 는 이 커밋을 가리키는 ref(`HEAD -> main`, `origin/main`, `tag: v1`) 이다. [date] 는 `yyyy-MM-dd HH:mm` 이다. */
data class GitCommit(
    val hash: String,
    val shortHash: String,
    val refs: List<String>,
    val author: String,
    val date: String,
    val subject: String,
) {
    val isHead: Boolean
        get() = refs.any { it == HeadRef || it.startsWith("$HeadRef -> ") }

    private companion object {
        const val HeadRef = "HEAD"
    }
}

interface GitChangesRepository {
    /** [directory] 를 담은 저장소의 변경. 저장소 밖이거나 git 을 쓸 수 없으면 null 이다. cold. */
    fun observeStatus(directory: String): Flow<GitStatus?>

    /** [directory] 를 담은 저장소의 커밋 그래프. 저장소 밖이거나 커밋이 없으면 빈 목록이다. cold. */
    fun observeGraph(directory: String): Flow<List<GitGraphLine>>

    /**
     * [path] 파일의 HEAD 대비 차이. 추적하지 않는 파일은 모든 줄을 더한 것이고, 같거나 무시된 파일은 빈 diff 다. 저장소
     * 밖이거나 git 을 쓸 수 없으면 null 이다. cold.
     */
    fun observeFileDiff(path: String): Flow<GitFileDiff?>

    /** [root] 저장소에서 [changes] 를 stage 한다. 실패는 [GitWorktreeException] 이다. */
    suspend fun stage(root: String, changes: List<GitChange>): Result<Unit>

    /** [root] 저장소에서 [changes] 를 unstage 한다. 이름 바꿈은 옛 경로도 함께 되돌린다. 실패는 [GitWorktreeException] 이다. */
    suspend fun unstage(root: String, changes: List<GitChange>): Result<Unit>
}

class ObserveGitStatusUseCase(
    private val repository: GitChangesRepository,
) {
    operator fun invoke(directory: String): Flow<GitStatus?> = repository.observeStatus(directory)
}

class ObserveGitGraphUseCase(
    private val repository: GitChangesRepository,
) {
    operator fun invoke(directory: String): Flow<List<GitGraphLine>> = repository.observeGraph(directory)
}

class ObserveGitFileDiffUseCase(
    private val repository: GitChangesRepository,
) {
    operator fun invoke(path: String): Flow<GitFileDiff?> = repository.observeFileDiff(path)
}

class StageGitChangesUseCase(
    private val repository: GitChangesRepository,
) {
    suspend operator fun invoke(root: String, changes: List<GitChange>): Result<Unit> =
        if (changes.isEmpty()) Result.success(Unit) else repository.stage(root, changes)
}

class UnstageGitChangesUseCase(
    private val repository: GitChangesRepository,
) {
    suspend operator fun invoke(root: String, changes: List<GitChange>): Result<Unit> =
        if (changes.isEmpty()) Result.success(Unit) else repository.unstage(root, changes)
}
