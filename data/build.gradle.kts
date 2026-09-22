import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidKotlinMultiplatformLibrary)
    alias(libs.plugins.kotlinSerialization)
}

val generateAppVersion = tasks.register("generateAppVersion") {
    val appVersion = libs.versions.appVersion.get()
    val outputDirectory = layout.buildDirectory.dir("generated/appVersion/kotlin")

    inputs.property("appVersion", appVersion)
    outputs.dir(outputDirectory)

    doLast {
        val file = outputDirectory.get()
            .file("io/github/taetae98coding/jarvis/data/appinfo/AppVersion.kt")
            .asFile

        file.parentFile.mkdirs()
        file.writeText(
            """
            package io.github.taetae98coding.jarvis.data.appinfo

            internal const val APP_VERSION: String = "$appVersion"

            """.trimIndent(),
        )
    }
}

kotlin {
    jvmToolchain(21)

    compilerOptions {
        // PlatformContext 가 expect class 다. Beta 기능이라는 안내일 뿐이라 경고를 끈다.
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    android {
        namespace = "io.github.taetae98coding.jarvis.data"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        withHostTest {}
    }

    jvm()

    iosArm64()
    iosSimulatorArm64()

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
    }

    sourceSets {
        commonMain {
            kotlin.srcDir(generateAppVersion)

            dependencies {
                // DataModule 이 도메인 인터페이스를 내보내므로 api 다.
                api(projects.domain)
                implementation(libs.kotlinx.serialization.json)
            }
        }

        wasmJsMain.dependencies {
            // localStorage, window, fetch. Kotlin 2.4 의 stdlib 에는 들어 있지 않다.
            implementation(libs.kotlinx.browser)
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}
