plugins {
    id("jarvis.kmp.library")
    id("jarvis.kmp.test")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.feature.theme.domain)
            // PlatformContext 와 SettingsStore 를 쓴다.
            implementation(projects.core.data)
        }

        wasmJsMain.dependencies {
            // ThemeAppearance 의 document. Kotlin 2.4 의 stdlib 에는 들어 있지 않다.
            implementation(libs.kotlinx.browser)
        }
    }
}
