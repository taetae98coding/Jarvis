package io.github.taetae98coding.jarvis.data.appinfo

import io.github.taetae98coding.jarvis.domain.appinfo.AppRelease
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

internal const val LatestReleaseUrl: String = "https://api.github.com/repos/taetae98coding/Jarvis/releases/latest"

@Serializable
private data class GitHubReleaseDto(
    @SerialName("tag_name") val tagName: String,
    val draft: Boolean = false,
    val prerelease: Boolean = false,
    val assets: List<GitHubAssetDto> = emptyList(),
)

@Serializable
private data class GitHubAssetDto(
    val name: String,
    @SerialName("browser_download_url") val downloadUrl: String,
)

private val GitHubJson = Json { ignoreUnknownKeys = true }

/**
 * `releases/latest` 응답에서 DMG 와 그 `.sha256` 자산을 고른다. 버전 형식이 다르거나 둘 중 하나라도
 * 없으면 설치할 수 없으므로 null 이다(docs/common/app-update.html R3·R5).
 */
internal fun decodeLatestRelease(json: String): AppRelease? {
    val release = runCatching { GitHubJson.decodeFromString<GitHubReleaseDto>(json) }.getOrNull() ?: return null
    if (release.draft || release.prerelease) return null

    val version = release.tagName.removePrefix("v").takeIf { parseVersion(it) != null } ?: return null
    val dmg = release.assets.firstOrNull { it.name.endsWith(".dmg") } ?: return null
    val checksum = release.assets.firstOrNull { it.name == "${dmg.name}.sha256" } ?: return null

    return AppRelease(version = version, downloadUrl = dmg.downloadUrl, checksumUrl = checksum.downloadUrl)
}

/** 둘 다 `MAJOR.MINOR.PATCH` 이고 [candidate] 가 더 클 때만 true. */
internal fun isNewerVersion(candidate: String, current: String): Boolean {
    val next = parseVersion(candidate) ?: return false
    val now = parseVersion(current) ?: return false

    return next.zip(now).firstOrNull { (a, b) -> a != b }?.let { (a, b) -> a > b } ?: false
}

private fun parseVersion(version: String): List<Int>? =
    version.split(".")
        .takeIf { it.size == 3 }
        ?.map { it.toIntOrNull()?.takeIf { part -> part >= 0 } ?: return null }
