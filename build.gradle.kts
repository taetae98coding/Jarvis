plugins {
    base
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.kotlinJvm) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.kotlinSerialization) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidKotlinMultiplatformLibrary) apply false
    // build-logic 의 클래스(appVersionCode)를 루트와 모든 하위 프로젝트의 빌드 스크립트가 보게 한다.
    id("jarvis.kmp.library") apply false
}

// Xcode 는 빌드 설정을 Kotlin 단계보다 먼저 확정하므로 Gradle 이 iOS 버전을 대신 쓸 수 없다. 대신 어긋나면
// check 를 실패시킨다(docs/platform/ios.html#release-build).
val checkIosAppVersion by tasks.registering {
    val appVersion = libs.versions.appVersion.get()
    val xcconfig = providers.fileContents(layout.projectDirectory.file("iosApp/Configuration/Config.xcconfig")).asText
    inputs.property("appVersion", appVersion)
    inputs.property("xcconfig", xcconfig)

    doLast {
        val settings = xcconfig.get().lineSequence()
            .map(String::trim)
            .filter { it.isNotEmpty() && !it.startsWith("//") && "=" in it }
            .associate { it.substringBefore("=").trim() to it.substringAfter("=").trim() }
        val expected = mapOf(
            "MARKETING_VERSION" to appVersion,
            "CURRENT_PROJECT_VERSION" to appVersionCode(appVersion).toString(),
        )
        val mismatches = expected.filter { (key, value) -> settings[key] != value }

        check(mismatches.isEmpty()) {
            "iosApp/Configuration/Config.xcconfig 가 appVersion $appVersion 과 어긋난다: " +
                mismatches.entries.joinToString { (key, value) -> "$key=${settings[key]} (기대값 $value)" }
        }
    }
}

tasks.check {
    dependsOn(checkIosAppVersion)
}

// :desktopApp:run 은 각 모듈의 build/libs/*.jar 를 그대로 classpath 에 올리고, JVM 은 jar 를 연 채 클래스를 처음 쓸 때 읽는다.
// Gradle 의 Jar 태스크는 기존 파일을 같은 inode 에 잘라 다시 쓰므로, 앱을 띄워 둔 채 다시 빌드하면(main 에 머지한 뒤의 빌드,
// 다른 세션의 jvmTest) 돌고 있던 앱이 아직 읽지 않은 클래스를 NoClassDefFoundError 로 잃는다. 쓰기 전에 지워 새 inode 에 쓰면
// 열어 둔 옛 파일은 그대로 읽힌다(docs/platform/jvm.html#dev-run-rebuild).
subprojects {
    tasks.withType<Jar>().configureEach {
        doFirst { archiveFile.get().asFile.delete() }
    }
}
