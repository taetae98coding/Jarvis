package io.github.taetae98coding.jarvis.data.appinfo

import io.github.taetae98coding.jarvis.domain.appinfo.AppRelease
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GitHubReleaseTest {
    @Test
    fun picksDmgAndItsChecksum() {
        assertEquals(
            AppRelease(
                version = "1.2.0",
                downloadUrl = "https://example.com/Jarvis-1.2.0.dmg",
                checksumUrl = "https://example.com/Jarvis-1.2.0.dmg.sha256",
            ),
            decodeLatestRelease(release(tag = "v1.2.0")),
        )
    }

    @Test
    fun releaseWithoutChecksumIsNotInstallable() {
        assertNull(decodeLatestRelease(release(tag = "v1.2.0", assets = listOf("Jarvis-1.2.0.dmg"))))
    }

    @Test
    fun releaseWithoutDmgIsNotInstallable() {
        assertNull(decodeLatestRelease(release(tag = "v1.2.0", assets = listOf("notes.txt"))))
    }

    @Test
    fun tagOutsideVersionFormatIsIgnored() {
        assertNull(decodeLatestRelease(release(tag = "nightly")))
        assertNull(decodeLatestRelease(release(tag = "v1.2")))
    }

    @Test
    fun prereleaseIsIgnored() {
        assertNull(decodeLatestRelease(release(tag = "v1.2.0", prerelease = true)))
    }

    @Test
    fun brokenBodyIsIgnored() {
        assertNull(decodeLatestRelease("{"))
    }

    @Test
    fun versionsCompareAsNumbers() {
        assertTrue(isNewerVersion("1.10.0", "1.9.0"))
        assertTrue(isNewerVersion("2.0.0", "1.99.99"))
        assertTrue(isNewerVersion("1.0.1", "1.0.0"))
        assertFalse(isNewerVersion("1.0.0", "1.0.0"))
        assertFalse(isNewerVersion("0.9.0", "1.0.0"))
        assertFalse(isNewerVersion("1.1.0", "1.0.0-dev"))
    }

    private fun release(
        tag: String,
        prerelease: Boolean = false,
        assets: List<String> = listOf("Jarvis-1.2.0.dmg", "Jarvis-1.2.0.dmg.sha256"),
    ): String {
        val assetJson = assets.joinToString(",") { name ->
            """{"name":"$name","browser_download_url":"https://example.com/$name","size":1}"""
        }
        return """{"tag_name":"$tag","draft":false,"prerelease":$prerelease,"name":"$tag","assets":[$assetJson]}"""
    }
}
