package io.github.taetae98coding.jarvis.domain.terminal

import kotlinx.coroutines.flow.Flow

class ObserveGitBranchesUseCase(
    private val repository: GitWorktreeRepository,
) {
    operator fun invoke(directory: String): Flow<List<GitBranch>> = repository.observeBranches(directory)
}
