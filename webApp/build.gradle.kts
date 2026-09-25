import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        outputModuleName.set("webApp")
        browser {
            commonWebpackConfig {
                outputFileName = "webApp.js"
            }
        }
        binaries.executable()
    }

    sourceSets {
        commonMain.dependencies {
            // 한글 폰트를 composeResources 로 싣는다(docs/platform/web.html#release-build). Res 클래스는 이 의존성이
            // commonMain 에 있어야 생성된다.
            implementation(libs.compose.components.resources)
        }

        wasmJsMain.dependencies {
            implementation(projects.shared)
        }
    }
}
