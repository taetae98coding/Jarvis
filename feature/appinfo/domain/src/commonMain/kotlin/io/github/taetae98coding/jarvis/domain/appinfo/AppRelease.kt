package io.github.taetae98coding.jarvis.domain.appinfo

/** 지금 앱보다 새 버전의 배포본(docs/common/app-update.html). */
data class AppRelease(
    val version: String,
    val downloadUrl: String,
    val checksumUrl: String,
)
