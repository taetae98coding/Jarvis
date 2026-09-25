package io.github.taetae98coding.jarvis.domain.appinfo

import kotlinx.coroutines.flow.Flow

interface AppUpdateRepository {
    /** 설치할 수 있는 새 버전. 없거나, 확인하지 못했거나, 이 플랫폼이 스스로 업데이트하지 못하면 null. */
    fun observeAvailableUpdate(): Flow<AppRelease?>

    /** 성공하면 앱이 곧 종료되고 새 버전으로 다시 뜬다. 실패하면 앱과 설치본은 그대로다. */
    suspend fun install(release: AppRelease): Result<Unit>
}
