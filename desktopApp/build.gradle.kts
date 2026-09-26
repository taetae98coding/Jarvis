import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(projects.shared)
    implementation(compose.desktop.currentOs)
}

compose.desktop {
    application {
        mainClass = "io.github.taetae98coding.jarvis.desktop.MainKt"
        // 번들에 넣을 JDK. 지정하지 않으면 Gradle 을 돌린 JDK 가 들어가서 머신마다 런타임이 달라진다.
        // :desktopApp:run 과 같은 toolchain 으로 맞춘다.
        javaHome = javaToolchains.launcherFor { languageVersion = JavaLanguageVersion.of(21) }
            .get().metadata.installationPath.asFile.absolutePath

        buildTypes.release.proguard {
            // JCEF·javacpp·FFmpeg·pty4j 가 리플렉션·JNI 로 부르는 클래스 때문에 ProGuard 가 미해결 참조 수십만 건으로
            // 실패한다. 규칙으로 막아도 네이티브가 이름으로 찾는 클래스를 확인할 길이 없어 끈다(docs/platform/jvm.html#release-build).
            isEnabled.set(false)
        }

        nativeDistributions {
            targetFormats(TargetFormat.Dmg)
            packageName = "Jarvis"
            packageVersion = libs.versions.appVersion.get()
            // jlink 런타임에 넣을 모듈. 빠지면 설치본에서만 NoClassDefFoundError 가 난다. 의존성을 바꾸면
            // ./gradlew :desktopApp:suggestRuntimeModules 로 다시 뽑는다.
            modules("java.instrument", "java.management", "java.net.http", "java.prefs", "java.sql", "jdk.httpserver", "jdk.unsupported")

            macOS {
                bundleID = "io.github.taetae98coding.jarvis"
                iconFile.set(project.file("icon/Jarvis.icns"))
                infoPlist {
                    // Google 로그인처럼 패스키(WebAuthn)를 확인하는 페이지를 열면 Chromium 이 휴대폰 인증기(하이브리드)를 찾으려고
                    // CoreBluetooth 를 건드린다. 이 키가 없으면 macOS(TCC) 가 앱을 곧바로 죽인다(docs/platform/jvm.html#terminal-browser).
                    extraKeysRawXml = """
                        <key>NSBluetoothAlwaysUsageDescription</key>
                        <string>웹 브라우저 탭의 페이지가 패스키처럼 근처 기기로 인증할 때 Bluetooth 를 씁니다.</string>
                    """.trimIndent()
                }
            }
        }
    }
}
