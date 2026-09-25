package io.github.taetae98coding.jarvis.data.appinfo

import io.github.taetae98coding.jarvis.domain.appinfo.AppRelease

/** 플랫폼이 스스로 업데이트하는 방법. 그럴 수 없는 플랫폼은 [platformAppUpdater] 가 null 이다. */
internal interface AppUpdater {
    /** `releases/latest` 응답 본문. 받지 못하면 null. */
    suspend fun fetchLatestRelease(): String?

    /** 받아서 교체를 준비하고 앱 종료를 요청한다. 실패하면 던지고 설치본은 그대로다. */
    suspend fun install(release: AppRelease)
}

internal expect fun platformAppUpdater(): AppUpdater?
