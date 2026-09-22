import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidKotlinMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
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
        namespace = "io.github.taetae98coding.jarvis.ui"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()
    }

    jvm()

    iosArm64()
    iosSimulatorArm64()

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
        // 브라우저에서 Compose UI 테스트가 Skiko 런타임을 불러오려면 필요하다.
        binaries.executable()
    }

    sourceSets {
        commonMain.dependencies {
            api(projects.domain)

            // 앱 모듈이 :shared 를 통해 Compose 를 그대로 쓰므로 api 로 내보낸다.
            api(libs.compose.runtime)
            api(libs.compose.foundation)
            api(libs.compose.material3)
            api(libs.compose.ui)
            api(libs.compose.components.resources)
            api(libs.compose.ui.toolingPreview)
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
