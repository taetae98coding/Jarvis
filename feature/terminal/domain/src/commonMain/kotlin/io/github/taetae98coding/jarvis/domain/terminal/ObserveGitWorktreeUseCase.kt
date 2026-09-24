package io.github.taetae98coding.jarvis.domain.terminal

import kotlinx.coroutines.flow.Flow

class ObserveGitWorktreeUseCase(
    private val repository: GitWorktreeRepository,
) {
    operator fun invoke(directory: String): Flow<GitWorktree?> = repository.observeWorktree(directory)
}
