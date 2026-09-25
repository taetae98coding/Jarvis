package io.github.taetae98coding.jarvis.data.appinfo

// 이 플랫폼에서는 앱이 자기 설치본을 바꾸지 않는다(docs/platform/web.html#app-update).
internal actual fun platformAppUpdater(): AppUpdater? = null
