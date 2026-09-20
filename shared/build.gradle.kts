import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidKotlinMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
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

    // Compose UI renders through Skiko on every target except Android, so those targets get a
    // shared source set for UI tests that need a Skiko surface.
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

    // Compose Multiplatform 1.12+ no longer publishes iosX64 (Intel simulator) artifacts.
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
        // Required so that Compose UI tests can load the Skiko runtime in the browser.
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
            }
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.compose.ui.test)
        }

        jvmTest.dependencies {
            // Skiko native runtime for the current host, required by runComposeUiTest on the JVM.
            implementation(compose.desktop.currentOs)
        }
    }
}
