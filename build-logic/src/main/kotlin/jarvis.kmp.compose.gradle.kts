import org.gradle.api.artifacts.VersionCatalogsExtension
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    id("jarvis.kmp.library")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
}

val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

fun library(alias: String) = libs.findLibrary(alias).get()

kotlin {
    // Android 을 제외한 모든 타깃은 Compose UI 를 Skiko 로 렌더링한다. Skiko 화면이 필요한 UI
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

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        // 브라우저에서 Compose UI 테스트가 Skiko 런타임을 불러오려면 필요하다. Compose 플러그인의
        // checkComposeUiTestConfigurationForWasmJs 는 Compose 를 쓰는 wasmJs 타깃이면 UI 테스트가
        // 없어도 실행 바이너리를 요구한다 (CMP-4906).
        binaries.executable()
    }

    sourceSets {
        // Style API 는 foundation 1.12 에서 실험 API 다. 이유는 docs/common/design-system.html 에 있다.
        all {
            languageSettings.optIn("androidx.compose.foundation.style.ExperimentalFoundationStyleApi")
        }

        commonMain.dependencies {
            // 기능 모듈과 앱 셸이 모두 같은 Compose·주입·내비게이션 표면을 쓴다.
            api(library("compose-runtime"))
            api(library("compose-foundation"))
            api(library("compose-material3"))
            api(library("compose-ui"))
            api(library("compose-components-resources"))
            api(library("compose-ui-toolingPreview"))

            api(library("koin-compose"))
            api(library("koin-compose-viewmodel"))
            api(library("koin-compose-navigation3"))

            implementation(library("androidx-lifecycle-viewmodel-compose"))
            implementation(library("androidx-navigation3-runtime"))
            implementation(library("androidx-navigation3-ui"))
            implementation(library("androidx-lifecycle-viewmodel-navigation3"))
            // retain 은 compose runtime 이 아니라 별도 아티팩트에 있다.
            implementation(library("compose-runtime-retain"))
        }
    }
}
