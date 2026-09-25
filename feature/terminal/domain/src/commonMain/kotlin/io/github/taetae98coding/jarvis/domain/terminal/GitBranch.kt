package io.github.taetae98coding.jarvis.domain.terminal

/**
 * 기준 브랜치 후보(docs/common/terminal-worktree-base-branch.html). [name] 은 git 에 그대로 넘길 이름으로, 원격 추적
 * 브랜치면 `origin/main` 처럼 원격 이름이 붙어 있다. [remote] 는 그 원격 이름이고 로컬 브랜치면 null 이다.
 */
data class GitBranch(
    val name: String,
    val remote: String? = null,
)
