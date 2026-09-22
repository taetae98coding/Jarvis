plugins {
    id("jarvis.kmp.library")
    id("jarvis.kmp.test")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.feature.rotation.domain)
            implementation(projects.core.data)
        }

        wasmJsMain.dependencies {
            // screen.orientation. Kotlin 2.4 의 stdlib 에는 들어 있지 않다.
            implementation(libs.kotlinx.browser)
        }
    }
}
