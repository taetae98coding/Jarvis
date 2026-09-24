package io.github.taetae98coding.jarvis.domain.terminal

/**
 * 폴더를 담고 있는 git 워크트리. [path] 는 그 워크트리의 최상위 폴더, [mainPath] 는 저장소 main 워크트리
 * (`.git` 디렉터리를 직접 가진 작업 트리)의 최상위 폴더다. 폴더가 main 워크트리 안에 있으면 둘이 같다.
 */
data class GitWorktree(
    val path: String,
    val mainPath: String,
) {
    /** [branch] 브랜치의 워크트리를 둘 기본 폴더. main 워크트리 옆의 `<main 폴더 이름>-worktrees/<branch>`. */
    fun defaultWorktreePath(branch: String): String = "${mainPath.trimEnd('/')}$WorktreesSuffix/${branch.trim()}"

    companion object {
        const val WorktreesSuffix = "-worktrees"
    }
}

/** `git` 이 거부했거나 실행할 수 없었다. [message] 는 창에 그대로 보인다(git 의 stderr). */
class GitWorktreeException(message: String) : RuntimeException(message)
