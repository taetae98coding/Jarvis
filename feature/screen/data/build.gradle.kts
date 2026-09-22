plugins {
    id("jarvis.kmp.library")
    id("jarvis.kmp.test")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.feature.screen.domain)
            // PlatformContext 와 조회 규칙을 쓴다.
            implementation(projects.core.data)
        }

        wasmJsMain.dependencies {
            // localStorage, window. Kotlin 2.4 의 stdlib 에는 들어 있지 않다.
            implementation(libs.kotlinx.browser)
        }
    }
}
