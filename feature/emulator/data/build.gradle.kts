plugins {
    id("jarvis.kmp.library")
    id("jarvis.kmp.test")
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.feature.emulator.domain)
            // 폴링 조회 규칙을 쓴다.
            implementation(projects.core.data)
            implementation(libs.kotlinx.serialization.json)
        }

        wasmJsMain.dependencies {
            // fetch, window. Kotlin 2.4 의 stdlib 에는 들어 있지 않다.
            implementation(libs.kotlinx.browser)
        }
    }
}
