package io.github.taetae98coding.jarvis.domain.appinfo

class InstallAppUpdateUseCase(
    private val repository: AppUpdateRepository,
) {
    suspend operator fun invoke(release: AppRelease): Result<Unit> = repository.install(release)
}
