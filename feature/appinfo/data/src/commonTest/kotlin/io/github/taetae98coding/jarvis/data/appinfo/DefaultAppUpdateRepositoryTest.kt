package io.github.taetae98coding.jarvis.data.appinfo

import io.github.taetae98coding.jarvis.domain.appinfo.AppRelease
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DefaultAppUpdateRepositoryTest {
    @Test
    fun newerReleaseIsOffered() = runTest {
        val repository = DefaultAppUpdateRepository(FakeUpdater(latest = "v1.2.0"), currentVersion = "1.0.0")

        assertEquals("1.2.0", repository.observeAvailableUpdate().first()?.version)
    }

    @Test
    fun sameOrOlderReleaseIsNotOffered() = runTest {
        assertNull(DefaultAppUpdateRepository(FakeUpdater(latest = "v1.0.0"), currentVersion = "1.0.0").observeAvailableUpdate().first())
        assertNull(DefaultAppUpdateRepository(FakeUpdater(latest = "v0.9.0"), currentVersion = "1.0.0").observeAvailableUpdate().first())
    }

    @Test
    fun failedCheckIsNotOffered() = runTest {
        assertNull(DefaultAppUpdateRepository(FakeUpdater(latest = null), currentVersion = "1.0.0").observeAvailableUpdate().first())
    }

    @Test
    fun noUpdaterMeansNoUpdate() = runTest {
        val repository = DefaultAppUpdateRepository(updater = null, currentVersion = "1.0.0")

        assertNull(repository.observeAvailableUpdate().first())
        assertTrue(repository.install(Release).isFailure)
    }

    @Test
    fun installFailureIsReturnedNotThrown() = runTest {
        val repository = DefaultAppUpdateRepository(FakeUpdater(latest = null, failure = "체크섬"), currentVersion = "1.0.0")

        assertEquals("체크섬", repository.install(Release).exceptionOrNull()?.message)
    }

    @Test
    fun installIsHandedToTheUpdater() = runTest {
        val updater = FakeUpdater(latest = null)

        assertTrue(DefaultAppUpdateRepository(updater, currentVersion = "1.0.0").install(Release).isSuccess)
        assertEquals(listOf(Release), updater.installed)
    }

    private class FakeUpdater(
        private val latest: String?,
        private val failure: String? = null,
    ) : AppUpdater {
        val installed = mutableListOf<AppRelease>()

        override suspend fun fetchLatestRelease(): String? =
            latest?.let {
                """{"tag_name":"$it","assets":[
                  {"name":"Jarvis.dmg","browser_download_url":"https://example.com/Jarvis.dmg"},
                  {"name":"Jarvis.dmg.sha256","browser_download_url":"https://example.com/Jarvis.dmg.sha256"}]}"""
            }

        override suspend fun install(release: AppRelease) {
            failure?.let { throw IllegalStateException(it) }
            installed += release
        }
    }

    private companion object {
        val Release = AppRelease(version = "1.2.0", downloadUrl = "d", checksumUrl = "c")
    }
}
