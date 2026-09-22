package io.github.taetae98coding.jarvis.domain.appinfo

class GetAppInfoUseCase(
    private val repository: AppInfoRepository,
) {
    operator fun invoke(): AppInfo = repository.getAppInfo()
}
