package io.github.taetae98coding.jarvis.data.appinfo

import java.nio.file.Path
import kotlin.io.path.name

// 데스크탑은 macOS 만 지원한다. 설치본만 업데이트한다 — :desktopApp:run 에는 jpackage 런처가 없어서
// jpackage.app-path 가 비고, 교체할 .app 번들도 없다(docs/platform/jvm.html#app-update).
internal actual fun platformAppUpdater(): AppUpdater? {
    if (!System.getProperty("os.name").orEmpty().startsWith("Mac")) return null

    val launcher = System.getProperty("jpackage.app-path") ?: return null
    // <폴더>/Jarvis.app/Contents/MacOS/Jarvis
    val bundle = Path.of(launcher).parent?.parent?.parent ?: return null
    if (!bundle.name.endsWith(".app")) return null

    return MacAppUpdater(
        bundle = bundle,
        releaseUrl = System.getenv("JARVIS_UPDATE_RELEASE_URL") ?: LatestReleaseUrl,
        userAgent = "Jarvis/$APP_VERSION",
    )
}
