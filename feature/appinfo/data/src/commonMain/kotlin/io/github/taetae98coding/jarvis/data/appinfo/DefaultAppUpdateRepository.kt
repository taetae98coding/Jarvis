package io.github.taetae98coding.jarvis.data.appinfo

import io.github.taetae98coding.jarvis.data.state.observeByPolling
import io.github.taetae98coding.jarvis.domain.appinfo.AppRelease
import io.github.taetae98coding.jarvis.domain.appinfo.AppUpdateRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours

// GitHub 는 새 Release 를 클라이언트에 알려 주지 않는다. 토큰 없는 API 한도(IP 당 시간당 60회)를 여러 사람이
// 나눠 쓸 수 있어서 길게 둔다(docs/common/app-update.html#implementation).
internal val AppUpdateCheckInterval: Duration = 1.hours

internal class DefaultAppUpdateRepository(
    private val updater: AppUpdater?,
    private val currentVersion: String = APP_VERSION,
) : AppUpdateRepository {
    override fun observeAvailableUpdate(): Flow<AppRelease?> {
        val updater = updater ?: return flowOf(null)

        return observeByPolling(AppUpdateCheckInterval) {
            updater.fetchLatestRelease()
                ?.let(::decodeLatestRelease)
                ?.takeIf { isNewerVersion(it.version, currentVersion) }
        }
    }

    override suspend fun install(release: AppRelease): Result<Unit> {
        val updater = updater ?: return Result.failure(UnsupportedOperationException("이 플랫폼은 스스로 업데이트할 수 없습니다."))

        return try {
            updater.install(release)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
