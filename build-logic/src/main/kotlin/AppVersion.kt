/**
 * `gradle/libs.versions.toml` 의 `appVersion`(MAJOR.MINOR.PATCH)에서 빌드 번호를 만든다. Android `versionCode` 와
 * iOS `CURRENT_PROJECT_VERSION` 이 같은 값을 쓴다(docs/common/release-build.html#requirements R4).
 */
fun appVersionCode(appVersion: String): Int {
    val parts = appVersion.split(".").map(String::toInt)
    require(parts.size == 3 && parts.all { it in 0..99 }) { "appVersion 은 MAJOR.MINOR.PATCH 이고 MINOR·PATCH 는 99 이하다: $appVersion" }

    val (major, minor, patch) = parts
    return major * 10000 + minor * 100 + patch
}
