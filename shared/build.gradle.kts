import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidKotlinMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
}

val generateAppVersion = tasks.register("generateAppVersion") {
    val appVersion = libs.versions.appVersion.get()
    val outputDirectory = layout.buildDirectory.dir("generated/appVersion/kotlin")

    inputs.property("appVersion", appVersion)
    outputs.dir(outputDirectory)

    doLast {
        val file = outputDirectory.get()
            .file("io/github/taetae98coding/jarvis/shared/AppVersion.kt")
            .asFile

        file.parentFile.mkdirs()
        file.writeText(
            """
            package io.github.taetae98coding.jarvis.shared

            internal const val APP_VERSION: String = "$appVersion"

            """.trimIndent(),
        )
    }
}

kotlin {
    jvmToolchain(21)

    // Android을 제외한 모든 타깃은 Compose UI를 Skiko로 렌더링한다. Skiko 화면이 필요한 UI
    // 테스트를 위해 그 타깃들을 하나의 소스 세트로 묶는다.
    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    applyDefaultHierarchyTemplate {
        common {
            group("skiko") {
                withJvm()
                withWasmJs()
                withIos()
            }
        }
    }

    android {
        namespace = "io.github.taetae98coding.jarvis.shared"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        withHostTest {}
    }

    jvm()

    // Compose Multiplatform 1.12+ 부터 iosX64(Intel 시뮬레이터) 아티팩트를 더 이상 배포하지 않는다.
    listOf(
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "Shared"
            isStatic = true
        }
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
        // 브라우저에서 Compose UI 테스트가 Skiko 런타임을 불러오려면 필요하다.
        binaries.executable()
    }

    sourceSets {
        commonMain {
            kotlin.srcDir(generateAppVersion)

            dependencies {
                api(libs.compose.runtime)
                api(libs.compose.foundation)
                api(libs.compose.material3)
                api(libs.compose.ui)
                api(libs.compose.components.resources)
                api(libs.compose.ui.toolingPreview)
                api(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.serialization.json)
            }
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.compose.ui.test)
            implementation(libs.kotlinx.coroutines.test)
        }

        jvmTest.dependencies {
            // JVM 에서 runComposeUiTest 가 요구하는 현재 호스트용 Skiko 네이티브 런타임.
            implementation(compose.desktop.currentOs)
        }
    }
}
